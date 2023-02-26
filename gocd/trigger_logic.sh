#!/usr/bin/env bash

echo Loading gocd explicit trigger logic - checks and actions

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

function fail_if_stage_running() {
  local stage_status=$1

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo fail_if_stage_running >> gocd_trigger.log; echo stage_status "$stage_status" >> gocd_trigger.log; }

  local stage_result=$(extract_stage_status_result "$stage_status")

  if [[ "$stage_result" == "Unknown" ]]; then
    echo "Failing fast as stages is currently running according to $stage_status"
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

microservices='pds-adaptor nems-event-processor'

function check_environment_is_deployed() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft

  # simplified pipelines next steps:

  # note:
  # just comparing stage status manifests may not be sufficient if you want to optimise - as e.g. if one gets scheduled
  # or assigned but is not building yet, it needn't invalidate run, but hardly seems worth it

  # NB there is a Cancel stage API which would be better if can be used on self than this red fail
  local stage_name=deploy.$environment_id

  for microservice in $microservices
  do
    echo Checking that $microservice is not deploying into $environment_id
    local stage_status=$(get_latest_stage_run_status $microservice $stage_name)
    fail_if_stage_running "$stage_status"

    echo Saving stage status manifest before tests
    echo "$stage_status" > $(stage_status_manifest_filename before $microservice $stage_name)
  done

  echo No deployments running into $environment_id. Allowing tests to run.
}

function check_environment_is_still_deployed_after() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft
  local stage_name=deploy.$environment_id

  echo Saving stage status manifests after tests
  for microservice in $microservices
  do
    echo Capturing current deploy status of $microservice into $environment_id
    local stage_name=deploy.$environment_id
    local stage_status=$(get_latest_stage_run_status $microservice $stage_name)
    echo "$stage_status" > $(stage_status_manifest_filename after $microservice $stage_name)
  done

  echo Comparing before and after statuses to ensure no deployment into $environment_id overlapped with tests
  local status_change
  local has_status_changed
  for microservice in $microservices
  do
    local before_status_filename=$(stage_status_manifest_filename before $microservice $stage_name)
    local after_status_filename=$(stage_status_manifest_filename after $microservice $stage_name)

    echo Checking $microservice has not been deployed during tests
    status_change=$(diff $before_status_filename $after_status_filename)
    has_status_changed=$?

    echo has_status_changed "$has_status_changed"
    if [ $has_status_changed -ne 0 ]; then
      echo "Exiting pending re-run of tests as $microservice deployment occurred into $environment_id, status change: $status_change"
      exit 121
    fi
  done

  # next is to find latest previous passing run of this e2e tests job

  # then next is to compare this manifests versions to *previous passing manifest*, and trigger next stages in those
  # pipelines that have different version IDs... or maybe actually safer to check latest completed next stage on
  # each microservice pipeline and trigger next stage if doesn't match? probably the prior - stick to e2e tests
  # triggering with reference to itself and previous runs - if other failures or re-runs occur that is a case for
  # manual repair

  # todo 99: allow force of e2e tests run skipping pre-requisite checks e.g. if FORCE_TEST_RUN=true
}

function trigger_downstream_stages() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft

  # NB there is a https://api.gocd.org/21.3.0/#comment-on-pipeline-instance API which maybe could add
  # some tracking info to this trigger, although it looks like comment is across whole pipeline run
  # and not stage specific meaning it could be a bit verbose / inappropriate to add comments for all triggers

  # was also thinking: one drawback to not having the e2e test process in the pipeline is we don't get to see the
  # red build within the pipeline - however we could have a job inline in the microservice pipeline that waits for
  # the automatically-triggered e2e tests job to complete and then continues (or fails or cancels) based


  echo automatic material dependency locator for pds-adaptor material is $GO_DEPENDENCY_LOCATOR_PDS_ADAPTOR
  trigger_stage_run $(echo $GO_DEPENDENCY_LOCATOR_PDS_ADAPTOR | sed 's/deploy.dev\/.*/prepare.test/')
  echo automatic material dependency locator for nems-event-processor material is $GO_DEPENDENCY_LOCATOR_NEMS_EVENT_PROCESSOR
  trigger_stage_run $(echo $GO_DEPENDENCY_LOCATOR_NEMS_EVENT_PROCESSOR | sed 's/deploy.dev\/.*/prepare.test/')
}