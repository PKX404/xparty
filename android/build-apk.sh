#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${XPARTY_ANDROID_SDK:?Set XPARTY_ANDROID_SDK to the Android SDK directory}"
: "${XPARTY_KEYSTORE:?Set XPARTY_KEYSTORE to your private release keystore}"
: "${XPARTY_STORE_PASS:?Set XPARTY_STORE_PASS to the keystore password}"
tools="$XPARTY_ANDROID_SDK/build-tools/35.0.0"
platform="$XPARTY_ANDROID_SDK/platforms/android-35/android.jar"
rm -rf build/classes build/dex build/generated
mkdir -p build/classes build/dex build/generated
"$tools/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$tools/aapt2" link -o build/unsigned.apk -I "$platform" --manifest app/src/main/AndroidManifest.xml --java build/generated --min-sdk-version 26 --target-sdk-version 35 build/resources.zip
javac -source 8 -target 8 -bootclasspath "$platform:$tools/core-lambda-stubs.jar" -d build/classes app/src/main/java/com/pkx404/xparty/MainActivity.java
find build/classes -name '*.class' -print > build/classes.list
"$tools/d8" --release --min-api 26 --lib "$platform" --output build/dex @build/classes.list
python3 - <<'PY'
import zipfile
from pathlib import Path
with zipfile.ZipFile('build/unsigned.apk','a',compression=zipfile.ZIP_STORED) as apk:
 for dex in Path('build/dex').glob('*.dex'):apk.write(dex,dex.name)
PY
"$tools/zipalign" -f -P 16 4 build/unsigned.apk build/aligned.apk
"$tools/apksigner" sign --ks "$XPARTY_KEYSTORE" --ks-key-alias xparty --ks-pass env:XPARTY_STORE_PASS --key-pass env:XPARTY_STORE_PASS --out build/Xparty-1.1.1.apk build/aligned.apk
"$tools/apksigner" verify --verbose --print-certs build/Xparty-1.1.1.apk
"$tools/zipalign" -c -P 16 4 build/Xparty-1.1.1.apk
