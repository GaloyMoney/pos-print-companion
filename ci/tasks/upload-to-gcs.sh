#!/bin/bash

set -eu

version=$(cat $VERSION_FILE)

echo $json_key > key.json

whoami

gcloud auth activate-service-account --key-file key.json

pushd artifacts

gsutil cp -r app/build/outputs/apk/release/* gs://$bucket/pos-print-companion/$GCS_DIRECTORY/android/pos-print-companion-$(date +%s)-v${version}/apk/release/
