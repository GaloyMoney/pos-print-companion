#!/bin/bash

set -eu

echo "$keystore" | base64 --decode > repo/app/pos-keystore

current_dir=$(pwd)
cd repo/
./gradlew build
cd "$current_dir"

mkdir -p artifacts/app/build/outputs/apk
cp -r repo/app/build/outputs/apk/* artifacts/app/build/outputs/apk
