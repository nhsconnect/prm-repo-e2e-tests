#!/usr/bin/env bash

echo Loading gocd explicit trigger logic - checks and actions

function get_latest_stage_run_status() {
  get_stage_run_history $1 $2 | jq .stages[0]
}

function extract_stage_status_result() {
  local stage_status=$1

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo extract_stage_status_result stage_status >> gocd_trigger.log; echo "$stage_status" >> gocd_trigger.log; }
  echo $stage_status | jq -r .result
}

function fail_if_stage_running() {
  local pipeline_name=$1
  local stage_name=$2

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo fail_if_stage_running >> gocd_trigger.log; echo pipeline_name $pipeline_name stage_name $stage_name >> gocd_trigger.log; }

  local stage_status=$(get_latest_stage_run_status $pipeline_name $stage_name)

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo fail_if_stage_running >> gocd_trigger.log; echo stage_status "$stage_status" >> gocd_trigger.log; }

  local stage_result=$(extract_stage_status_result "$stage_status")

  if [[ "$stage_result" == "Unknown" ]]; then
    echo "Failing e2e tests fast as $pipeline_name is currently running $stage_name"
    exit 37
  fi
}

function check_environment_is_deployed() {
  local environment_id=${ENVIRONMENT_ID:='ENVIRONMENT_ID is not set'} # yeah using ENVIRONMENT_ID because NHS_ENVIRONMENT is daft

  # simplified pipelines next steps:

  # note:
  # create versions manifest - then can just check is identical at end of tests?
  # ... well not quite if you want to optimise - as e.g. if one gets scheduled or assigned
  # but is not building yet, it needn't invalidate run, but for simplicity:

  # todo 0: switch from looking for job status 'Completed', to stage result !'Unknown' which ensures
  #         that waits for stage if has multiple parallel jobs

  # make sure all are in state completed at start of run
  # todo 1: split fail_if_stage_running to capture of stage status and separate fail \
  #         which means stage status can be used afterwards for comparison

  # NB there is a Cancel stage API which would be better if can be used on self than this red fail
  fail_if_stage_running pds-adaptor deploy.$environment_id
  fail_if_stage_running nems-event-processor deploy.$environment_id

  echo No deployments running into $environment_id. Allowing tests to run.

  # make sure all are in state completed at end of run, including which pipeline stage and job run number
  # todo 2: capture input stage statuses again and compare vs previous, fail if different

  # if start and end manifests not identical, fail build due to potential changes while tests running (tests
  # should then automatically re-run when in-flight or completed change triggers this test job again)

  # next is to find latest previous passing run of this e2e tests job

  # then next is to compare this manifests versions to previous passing manifest, and trigger next stages in those
  # pipelines that have different version IDs... or maybe actually safer to check latest completed next stage on
  # each microservice pipeline and trigger next stage if doesn't match? probably the prior - stick to e2e tests
  # triggering with reference to itself and previous runs - if other failures or re-runs occur that is a case for
  # manual repair

  # think about / try inline e2e-tests in microservices pipelines? probs need to check only one e2e tests running at
  # a time as well as no other deploys going on

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