#!/bin/bash

if [[ -z "${LARKY_HOME}" ]]; then
  echo "LARKY_HOME not found"
  exit 1
fi

#URL="https://github-registry-files.githubusercontent.com/300670787/e34cd980-8332-11eb-84a7-105b30bd4cf5?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIAIWNJYAX4CSVEH53A%2F20210312%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20210312T211445Z&X-Amz-Expires=300&X-Amz-Signature=141d38ff961566f197d5624baf8c5e39c8a3c08cbddb4ecbdbd7eff470f4c76c&X-Amz-SignedHeaders=host&actor_id=0&key_id=0&repo_id=300670787&response-content-disposition=filename%3Dlarky-0.3.0-jar-with-dependencies.jar&response-content-type=application%2Foctet-stream"
#wget -O $LARKY_HOME/larky-api/src/main/resources/larky-0.3.0-fat.jar $URL
