#!/bin/bash

# set default verbose level to warning
__VERBOSE=4

# Setup Logging Infra
declare LOG_LEVELS
LOG_LEVELS=([0]="emerg" [1]="alert" [2]="crit" [3]="err" [4]="warning" [5]="notice" [6]="info" [7]="debug")
function .log () {
  local LEVEL=${1}
  shift
  if [ ${__VERBOSE} -ge ${LEVEL} ]; then
    echo "[${LOG_LEVELS[$LEVEL]}]" "$@"
  fi
}

# Read flags from command line
while getopts ":dis" o; do
    case "${o}" in
        d) #debug
            __VERBOSE=7
            ;;
        i) #debug
            __VERBOSE=6
            ;;
        s) #silent
            __VERBOSE=3
            ;;
        *)
          ;;
    esac
done
.log 7 "VERBOSE levels set to [${LOG_LEVELS[$__VERBOSE]}]"


# Get larky lib home path
LARKY_LIB_HOME=${LARKY_LIB_HOME:-~/.larky/lib}
mkdir -p $LARKY_LIB_HOME
if [ $? -ne 0 ] ; then
    .log 3 "could not create directory $LARKY_LIB_HOME"
    exit
fi

# Construct GraphQL query to get Larky package versions and jar files
gql_query='query {
  repository(owner: \"verygoodsecurity\", name: \"starlarky\") {
    packages(first:1, names: [\"com.verygood.security.larky\"]) {
      nodes {
        versions(first:100) {
          nodes {
            version
            files(first:20) {
              nodes {
                name
                url
              }
            }
          }
        }
      }
    }
  }
}'


# Get packages from github registry using GraphQL API
# Parse and clean output into {"packages": [ {v1,jar1},{v2,jar2},...,{vN,jarN} ] }
.log 6 "Downloading from $url into $LARKY_API_JAR"
package_json=$(
  curl  $( (( __VERBOSE < 7 )) && printf %s '-s' ) \
        -H 'Content-Type: application/json' \
        -H "Authorization: bearer $GITHUB_API_TOKEN" \
        -X POST \
        -d "{\"query\": \"$(echo $gql_query)\"}" \
        https://api.github.com/graphql \
  | jq "{ packages: [
          .data.repository.packages.nodes[].versions.nodes[]
            | { version: .version,
                url:  ( .files.nodes[]
                        | select(.name
                            | test(\"^.*with-dependencies\\\.jar$\")
                          )
                        | .url
                      )
              }
          ]
        }"
)

# Download fat jar files from github registry
echo $package_json | jq -c '.packages[]'| while read i; do
    # get cleaned verion & jar name
    version=$(jq ".version" <<< $i | sed -e 's/^"//' -e 's/"$//')
    url=$(jq ".url" <<< $i | sed -e 's/^"//' -e 's/"$//')

    # construct output jar paths
    LARKY_API_JAR=$LARKY_LIB_HOME/larky-$version-fat.jar

    # get jar
    .log 6 "Downloading from $url into $LARKY_API_JAR"
    curl $( (( __VERBOSE < 7 )) && printf %s '-s' ) -o $LARKY_API_JAR -L $url

done