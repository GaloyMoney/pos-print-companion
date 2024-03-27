#!/bin/zsh

set -eu
export CI_ROOT="$(pwd)"
export BUILD_NUMBER=$(cat ${CI_ROOT}/build-number-android/android)

./gradlew build

mkdir -p artifacts/android/app/build/outputs
cp -r app/build/outputs/* artifacts/android/app/build/outputs
