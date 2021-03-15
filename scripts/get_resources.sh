#!/bin/bash

if [[ -z "${LARKY_HOME}" ]]; then
  echo "LARKY_HOME not found"
  exit 1
fi

API_RESOURCE_HOME=$LARKY_HOME/larky-api/src/main/resources
LARKY_REGISTRY=https://maven.pkg.github.com/verygoodsecurity/starlarky/com/verygood/security/larky

LARKY_V030_REGISTRY=$LARKY_REGISTRY/0.3.0/larky-0.3.0-jar-with-dependencies.jar
LARKY_V030_PATH=$API_RESOURCE_HOME/larky-0.3.0-fat.jar
wget --user $GITHUB_USERNAME --password $GITHUB_API_TOKEN -O $LARKY_V030_PATH $LARKY_V030_REGISTRY