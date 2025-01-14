#!/bin/bash
OS=`uname`
# This script allows up to quickly update versions where they are needed.
#PROJECT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout | sed 's/-SNAPSHOT//g')
PROJECT_VERSION=$1
if [ "$OS" = 'Darwin' ]; then
        # for MacOS
        sed -i '' "s/version = .*/version = \"${PROJECT_VERSION}\",/g" oan-rest/src/main/java/org/jax/oan/Application.java

    else
        # for Linux and Windows
        sed -i "s/version = .*/version = \"${PROJECT_VERSION}\"/g" oan-rest/src/main/java/org/jax/oan/Application.java
fi
./mvnw versions:set -DnewVersion=${PROJECT_VERSION}
