#!/bin/bash


if [[ -z "${LARKY_HOME}" ]]; then
  echo "LARKY_HOME not found"
  exit 1
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
package_json=$(
  curl -H 'Content-Type: application/json' \
        -H "Authorization: bearer $GITHUB_API_TOKEN" \
        -X POST \
        -d "{\"query\": \"$(echo $gql_query)\"}" \
        https://api.github.com/graphql \
  | jq "{ packages: [
          .data.repository.packages.nodes[].versions.nodes[]
            | { version: .version,
                jar:  ( .files.nodes[]
                        | select(.name
                            | test(\"^.*with-dependencies\\\.jar$\")
                          )
                          | .name
                      )
              }
          ]
        }"
)

# Download fat jar files from github registry
API_RESOURCE_HOME=$LARKY_HOME/larky-api/src/main/resources
LARKY_REGISTRY=https://maven.pkg.github.com/verygoodsecurity/starlarky/com/verygood/security/larky
echo $package_json | jq -c '.packages[]'| while read i; do
    # get verion & jar name
    version=$(jq ".version" <<< $i)
    jar=$(jq ".jar" <<< $i)

    # remove double quotes
    version=$(sed -e 's/^"//' -e 's/"$//' <<< $version)
    jar=$(sed -e 's/^"//' -e 's/"$//' <<< $jar)

    # get full registry and output jar paths
    LARKY_REGISTRY_JAR=$LARKY_REGISTRY/$version/$jar
    LARKY_API_JAR=$API_RESOURCE_HOME/larky-$version-fat.jar

    # get jar
    wget --user $GITHUB_USERNAME --password $GITHUB_API_TOKEN -O $LARKY_API_JAR $LARKY_REGISTRY_JAR
done


