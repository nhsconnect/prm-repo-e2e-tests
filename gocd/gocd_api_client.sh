#!/usr/bin/env bash

echo Loading gocd api client functions

function init_gocd_api_access() {
  if [[ -z "${GOCD_API_TOKEN}" ]]; then
    export GOCD_API_TOKEN_PARAM="/repo/user-input/gocd-access-token"
    export GOCD_API_TOKEN=$(aws ssm get-parameter --with-decryption --region ${AWS_DEFAULT_REGION} --name ${GOCD_API_TOKEN_PARAM} | jq -r .Parameter.Value)
    export GOCD_HOST=prod.gocd.patient-deductions.nhs.uk
  fi
}

function get_stage_run_history() {
  local pipeline_name=$1
  local stage_name=$2

  init_gocd_api_access

  local stage_history=$(curl --silent --fail "https://$GOCD_HOST/go/api/stages/$pipeline_name/$stage_name/history" \
    $GOCD_API_CURL_OPTIONS \
    -H "Authorization: bearer $GOCD_API_TOKEN" \
    -H 'Accept: application/vnd.go.cd.v3+json')

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo get_stage_run_history pipeline_name $pipeline_name stage_name $stage_name >> gocd_trigger.log >> gocd_trigger.log; echo "$stage_history" >> gocd_trigger.log; }

  echo "$stage_history"
}

function get_pipeline_config() {
  local pipeline_name=$1

  init_gocd_api_access

  local pipeline_config=$(curl --silent --fail "https://$GOCD_HOST/go/api/admin/pipelines/$pipeline_name" \
    $GOCD_API_CURL_OPTIONS \
    -H "Authorization: bearer $GOCD_API_TOKEN" \
    -H 'Accept: application/vnd.go.cd.v11+json')

  [[ $GOCD_TRIGGER_LOG == DEBUG ]] && { date >> gocd_trigger.log; echo get_pipeline_config $pipeline_name >> gocd_trigger.log >> gocd_trigger.log; echo "$pipeline_config" >> gocd_trigger.log; }

  echo "$pipeline_config"
}

function trigger_stage_run() {
  local stage_spec=$1

  echo triggering $stage_spec

  init_gocd_api_access

  curl --silent --fail "https://$GOCD_HOST/go/api/stages/$stage_spec/run" \
    $GOCD_API_CURL_OPTIONS \
    -H "Authorization: bearer $GOCD_API_TOKEN" \
    -H 'Accept: application/vnd.go.cd.v2+json' \
    -H 'X-GoCD-Confirm: true' \
    -X POST
}