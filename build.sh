#!/usr/bin/env bash
# 构建「临时ROOT」APK
# 依赖：JDK 11+、Android SDK（build-tools 34+、platforms/android-34+）
#
# 用法：
#   ./build.sh                 # 输出 dist/temp-root.apk
#   ./build.sh /tmp/x.apk      # 指定输出路径
#   BT=... AJ=... ./build.sh   # 覆盖 SDK 路径
set -euo pipefail
cd "$(dirname "$0")"

BT="${BT:-/opt/android-sdk/build-tools/36.0.0}"
AJ="${AJ:-/opt/android-sdk/platforms/android-36/android.jar}"
SHIZUKU_VER="${SHIZUKU_VER:-13.1.5}"
SHIZUKU_URL="https://repo1.maven.org/maven2/dev/rikka/shizuku/api/${SHIZUKU_VER}/api-${SHIZUKU_VER}.aar"
SHIZUKU_PROVIDER_URL="https://repo1.maven.org/maven2/dev/rikka/shizuku/provider/${SHIZUKU_VER}/provider-${SHIZUKU_VER}.aar"
OUT="${1:-dist/temp-root.apk}"
KS="${KS:-keystore.jks}"
KS_PASS="${KS_PASS:-temproot}"

APP=app
B=build

echo "==> 0/6 准备依赖"
mkdir -p "$B/libs" "$B/classes" "$B/dex" "$(dirname "$OUT")"
if [ ! -f "$B/libs/shizuku-classes.jar" ]; then
  echo "    下载 Shizuku API ${SHIZUKU_VER}"
  curl -fsSL -o "$B/shizuku.aar" "$SHIZUKU_URL"
  (cd "$B" && unzip -o -q shizuku.aar classes.jar && mv classes.jar libs/shizuku-classes.jar)
fi
# ShizukuProvider 在独立的 provider 包里；清单声明了它，缺失会导致启动即崩溃
if [ ! -f "$B/libs/shizuku-provider-classes.jar" ]; then
  echo "    下载 Shizuku Provider ${SHIZUKU_VER}"
  curl -fsSL -o "$B/shizuku-provider.aar" "$SHIZUKU_PROVIDER_URL"
  (cd "$B" && unzip -o -q shizuku-provider.aar classes.jar && mv classes.jar libs/shizuku-provider-classes.jar)
fi

echo "==> 1/6 编译 Java"
rm -rf "$B/classes" && mkdir -p "$B/classes"
javac --release 11 -encoding UTF-8 -classpath "$AJ" \
      -d "$B/classes" $(find "$APP/src" -name '*.java')

echo "==> 2/6 汇编 dex（含 Shizuku API）"
rm -rf "$B/dex" && mkdir -p "$B/dex"
"$BT/d8" --min-api 30 --lib "$AJ" --output "$B/dex" \
      $(find "$B/classes" -name '*.class') "$B/libs/shizuku-classes.jar" "$B/libs/shizuku-provider-classes.jar"

echo "==> 3/6 编译资源"
"$BT/aapt2" compile --dir "$APP/res" -o "$B/res.zip"

echo "==> 4/6 链接 APK"
"$BT/aapt2" link -o "$B/base.apk" \
      --manifest "$APP/AndroidManifest.xml" -I "$AJ" \
      --min-sdk-version 30 --target-sdk-version 36 \
      --version-code 4 --version-name 1.3 \
      -R "$B/res.zip" --auto-add-overlay

echo "==> 5/6 装入 dex 并签名"
cp "$B/base.apk" "$B/unsigned.apk"
(cd "$B/dex" && zip -q -j "../unsigned.apk" classes.dex)
"$BT/zipalign" -f -p 4 "$B/unsigned.apk" "$B/aligned.apk"

if [ ! -f "$KS" ]; then
  echo "    生成签名密钥 $KS（口令 $KS_PASS）"
  keytool -genkeypair -keystore "$KS" -alias temproot \
          -storepass "$KS_PASS" -keypass "$KS_PASS" \
          -keyalg RSA -keysize 2048 -validity 10000 \
          -dname "CN=temp-root" >/dev/null 2>&1
fi

"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
      --out "$OUT" "$B/aligned.apk"

echo "==> 6/6 完成"
"$BT/apksigner" verify "$OUT" && echo "    $OUT"
