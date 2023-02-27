#!/usr/bin/env bash

source gocd/gocd_api_client_stub.sh
source gocd/trigger_functions.sh

fn_output=''
fn_result=999
expected=expected-not-set

echo get_next_stage_name should return the name of the stage after the specified pipeline stage

get_pipeline_config_stub_response='{
                                     "name" : "some-pipeline",
                                     "stages" : [ {
                                       "name" : "prepare.dev",
                                       "jobs" : [ {
                                         "name" : "prepare.dev"
                                       } ]
                                     }, {
                                       "name" : "deploy.dev",
                                       "jobs" : [ {
                                         "name" : "deploy.dev"
                                       } ]
                                     }, {
                                       "name" : "prepare.test",
                                       "jobs" : [ {
                                         "name" : "prepare.test"
                                       } ]
                                     } ]
                                   }'

fn_output=$(get_next_stage_name some-pipeline deploy.dev)
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
if [ "$fn_output" != 'prepare.test' ]; then
  echo 'should be returning next stage name but returned:' "$fn_output"
  exit 42
fi
echo passed


echo extract_pipeline_counter should pull the pipeline_counter field out of a stage status object

fn_output=$(extract_pipeline_counter '{
  "counter": "1",
  "result": "Unknown",
  "pipeline_counter": 333,
  "jobs": []
}')
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
if [ "$fn_output" != "333" ]; then
  echo "should have returned pipeline run counter stage run was within, but got $fn_output"
  exit $fn_result
fi
echo passed


echo get_latest_stage_run_status should return the first stage run object representing the latest triggered

get_stage_run_history_stub_response='{
                                       "stages": [
                                         {
                                           "counter": "1",
                                           "result": "Unknown",
                                           "pipeline_counter": 52,
                                           "jobs": []
                                         },
                                         {
                                           "counter": "1",
                                           "result": "Passed",
                                           "pipeline_counter": 51,
                                           "jobs": []
                                         }
                                       ]
                                     }'

fn_output=$(get_latest_stage_run_status some_pipeline some_stage)
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
if [ "$fn_output" != '{
  "counter": "1",
  "result": "Unknown",
  "pipeline_counter": 52,
  "jobs": []
}' ]; then
  echo 'should be returning latest stage run but returned:' "$fn_output"
  exit 42
fi
echo passed


echo fail_if_stage_running should exit with failure if stage is still running i.e. an unknown result

fn_output=$(fail_if_stage_running '{
  "counter": "1",
  "result": "Unknown",
  "pipeline_counter": 52,
  "jobs": []
}')
fn_result=$?

if [ $fn_result -eq 0 ]; then
  echo 'should be exiting with non-zero code to denote failure'
  exit 42
fi
echo passed


echo fail_if_stage_running should return normally if stage is not running i.e. has completed and result known

fn_output=$(fail_if_stage_running '{
  "counter": "1",
  "result": "Passed",
  "pipeline_counter": 52,
  "jobs": []
}')
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
echo passed


echo fail_if_stage_running should return normally if stage is not running but failed which could occur \
     if manually triggered to force re-test but of course tests would not be triggered automatically in this case

fn_output=$(fail_if_stage_running '{
  "counter": "1",
  "result": "Failed",
  "pipeline_counter": 52,
  "jobs": []
}')
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
echo passed


echo stage_status_manifest_filename should identify before-test json file clearly

fn_output=$(stage_status_manifest_filename before some-pipeline some-stage)
fn_result=$?

expected=some-pipeline-some-stage-status.before.json

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
if [ "$fn_output" != "$expected" ]; then
  echo "return value should have been $expected, but got $fn_output"
  exit $fn_result
fi
echo passed


echo stage_status_manifest_filename should identify after-test json file clearly

fn_output=$(stage_status_manifest_filename after the-pipeline the-stage)
fn_result=$?

expected=the-pipeline-the-stage-status.after.json

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
if [ "$fn_output" != "$expected" ]; then
  echo "return value should have been $expected, but got $fn_output"
  exit $fn_result
fi
echo passed


echo stage_status_manifest_filename should reject a context which is neither before or after

fn_output=$(stage_status_manifest_filename not_before_or_after the-pipeline the-stage)
fn_result=$?

expected=the-pipeline-the-stage-status.after.json

if [ $fn_result -eq 0 ]; then
  echo 'should have failed to generate filename for unknown context'
  exit 42
fi
echo passed


