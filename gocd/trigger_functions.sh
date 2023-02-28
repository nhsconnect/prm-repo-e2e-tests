#!/usr/bin/env bash

echo Loading gocd explicit trigger logic and functions - checks and actions

function get_latest_stage_run_status() {
  local pipeline_name=$1
  local stage_name=$2

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo get_latest_stage_run_status >> gocd_trigger.log; echo pipeline_name $pipeline_name stage_name $stage_name >> gocd_trigger.log; }

  get_stage_run_history $pipeline_name $stage_name | jq .stages[0]
}

function extract_stage_status_result() {
  local stage_status=$1

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo extract_stage_status_result stage_status >> gocd_trigger.log; echo "$stage_status" >> gocd_trigger.log; }
  echo $stage_status | jq -r .result
}

function extract_pipeline_counter() {
  local stage_status=$1

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo extract_stage_status_result stage_status >> gocd_trigger.log; echo "$stage_status" >> gocd_trigger.log; }
  echo $stage_status | jq -r .pipeline_counter
}

function fail_if_stage_running() {
  local stage_status=$1

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo fail_if_stage_running >> gocd_trigger.log; echo stage_status "$stage_status" >> gocd_trigger.log; }

  local stage_result=$(extract_stage_status_result "$stage_status")

  if [[ "$stage_result" == "Unknown" ]]; then
    echo "Failing fast as stage is currently running according to $stage_status"
    exit 37
  fi
}

function stage_status_manifest_filename() {
  local context=$1
  local pipeline=$2
  local stage=$3

  if [ "$context" != 'before' ] && [ "$context" != 'after' ]; then
    echo "Unknown context: $context"
    exit 77
  fi

  echo $pipeline-$stage-status.$context.json
}

function get_next_stage_name() {
  local pipeline_name=$1
  local stage_name=$2

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo get_latest_stage_run_status >> gocd_trigger.log; echo pipeline_name $pipeline_name stage_name $stage_name >> gocd_trigger.log; }

  get_pipeline_config $pipeline_name | jq -r .stages[].name | grep -A1 $stage_name | grep -v $stage_name
}

function check_environment_is_deployed() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft

  if [ "$FORCE_TEST_RUN" == 'true' ]; then
    echo Skipping pre-test deployment status checks as FORCE_TEST_RUN is $FORCE_TEST_RUN
    exit 0
  fi

  # note:
  # just comparing stage status manifests may not be sufficient if you want to optimise - as e.g. if one gets scheduled
  # or assigned but is not building yet, it needn't invalidate run, but hardly seems worth it

  local stage_name=deploy.$environment_id

  local is_stage_running
  for microservice in $MICROSERVICES
  do
    echo Checking that $microservice is not deploying into $environment_id
    local stage_status=$(get_latest_stage_run_status $microservice $stage_name)

    set +e
    (fail_if_stage_running "$stage_status")
    is_stage_running=$?
    set -e
    if [ $is_stage_running -ne 0 ]; then
      echo Caught that stage is still running - attempting to cancel THIS stage run
      cancel_stage_run $GO_PIPELINE_NAME/$GO_PIPELINE_COUNTER/$GO_STAGE_NAME/$GO_STAGE_COUNTER
      exit 99
    fi

    echo Saving stage status manifest before tests
    echo "$stage_status" > $(stage_status_manifest_filename before $microservice $stage_name)
  done

  echo No deployments running into $environment_id. Allowing tests to run.
}

function check_environment_is_still_deployed_after() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft
  local stage_name=deploy.$environment_id

  if [ "$FORCE_TEST_RUN" == 'true' ]; then
    echo Skipping post-test deployment status checks as FORCE_TEST_RUN is $FORCE_TEST_RUN
    exit 0
  fi

  echo Saving stage status manifests after tests
  for microservice in $MICROSERVICES
  do
    echo Capturing current deploy status of $microservice into $environment_id
    local stage_name=deploy.$environment_id
    local stage_status=$(get_latest_stage_run_status $microservice $stage_name)
    echo "$stage_status" > $(stage_status_manifest_filename after $microservice $stage_name)
  done

  echo Comparing before and after statuses to ensure no deployment into $environment_id overlapped with tests
  local status_change
  local has_status_changed
  for microservice in $MICROSERVICES
  do
    local before_status_filename=$(stage_status_manifest_filename before $microservice $stage_name)
    local after_status_filename=$(stage_status_manifest_filename after $microservice $stage_name)

    echo Checking $microservice has not been deployed during tests
    set +e
    status_change=$(diff $before_status_filename $after_status_filename)
    has_status_changed=$?
    set -e

    if [ $has_status_changed -ne 0 ]; then
      echo "Cancelling tests pending re-run as $microservice deployment occurred into $environment_id during tests, status change: $status_change"
      cancel_stage_run $GO_PIPELINE_NAME/$GO_PIPELINE_COUNTER/$GO_STAGE_NAME/$GO_STAGE_COUNTER
      exit 121
    fi
  done
}

function trigger_downstream_stages() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft
  local stage_name=deploy.$environment_id

  if [ "$FORCE_TEST_RUN" == 'true' ]; then
    echo Skipping triggering of downstream next stages as FORCE_TEST_RUN is $FORCE_TEST_RUN so status logic skipped
    exit 0
  fi

  for microservice in $MICROSERVICES
  do
    echo Getting latest run counter of next stage after $microservice $stage_name
    local after_status_filename=$(stage_status_manifest_filename after $microservice $stage_name)
    local deploy_stage_status=$(cat $after_status_filename)

    local next_stage_name=$(get_next_stage_name $microservice $stage_name)
    local latest_next_stage_status=$(get_latest_stage_run_status $microservice $next_stage_name)

    local deploy_stage_pipeline_counter=$(extract_pipeline_counter "$deploy_stage_status")
    local latest_next_stage_pipeline_counter=$(extract_pipeline_counter "$latest_next_stage_status")

    # trigger if pipeline counter does not match that from manifest
    if [ "$deploy_stage_pipeline_counter" != "$latest_next_stage_pipeline_counter" ]; then
      echo Next stage counter does not match - triggering
      trigger_stage_run $microservice/$deploy_stage_pipeline_counter/$next_stage_name
    else
      echo Next stage of $microservice already at $deploy_stage_pipeline_counter - doing nothing NB if this is pipeline rerun will require manual rerun
    fi

  done

  # NB there is a https://api.gocd.org/21.3.0/#comment-on-pipeline-instance API which maybe could add
  # some tracking info to this trigger, although it looks like comment is across whole pipeline run
  # and not stage specific meaning it could be a bit verbose / inappropriate to add comments for all triggers

  # also AFAICT the comment is easy to overlook and does not stand out at all
}