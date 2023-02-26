#!/usr/bin/env bash

echo Stubbing gocd api client functions for tests

function get_stage_run_history() {
  echo $get_stage_run_history_stub_response
}

function trigger_stage_run() {
  echo $trigger_stage_run_stub_response
}