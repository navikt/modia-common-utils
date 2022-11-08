#!/bin/bash
set -e

TIME=$(TZ="Europe/Oslo" date +%Y.%m.%d-%H.%M)
COMMIT=$(git rev-parse --short=12 HEAD)
VERSION="1.$TIME-$COMMIT-beta"
echo "Tagging commit: $VERSION"

git tag $VERSION
git push --tags

echo "Setting version: $VERSION"
mvn -B versions:set -DnewVersion="$VERSION"
mvn -B versions:commit

echo "Running release beta"
mvn -B --settings .github/maven-settings.xml deploy -Dmaven.wagon.http.pool=false
