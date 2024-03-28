#!/bin/bash

set -eu

current_dir=$(pwd)
cd repo/
./gradlew build
cd "$current_dir"

mkdir -p artifacts/app/build/outputs/apk
cp -r repo/app/build/outputs/apk/* artifacts/app/build/outputs/apk
