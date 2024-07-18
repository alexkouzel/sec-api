#!/bin/bash

if [ -z "$1" ]; then
  echo "Provide version. Usage: $0 <version>"
else
  ./gradlew -Pversion=$1 publish
fi
