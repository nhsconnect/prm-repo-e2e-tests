#!/usr/bin/env bash

source gocd/gocd_api_client_stub.sh
source gocd/trigger_logic.sh

fn_output=''
fn_result=999

echo fail_if_stage_running should exit with failure if stage is still running i.e. an unknown result

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

fn_output=$(fail_if_stage_running some_pipeline some_stage)
fn_result=$?

if [ $fn_result -eq 0 ]; then
  echo 'should be exiting with non-zero code to denote failure'
  exit 42
fi
echo passed


echo fail_if_stage_running should return normally if stage is not running i.e. has completed and result known

get_stage_run_history_stub_response='{
                                       "stages": [
                                         {
                                           "counter": "1",
                                           "result": "Passed",
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

fn_output=$(fail_if_stage_running some_pipeline some_stage)
fn_result=$?

if [ $fn_result -ne 0 ]; then
  echo 'should not have failed, but failed with' $fn_result
  exit $fn_result
fi
echo passed


