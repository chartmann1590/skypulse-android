#!/usr/bin/env bash
# Runs inside the emulator step: installs the amazon RELEASE APK (as shipped) and walks its main screens.
set -u
export OUT=/tmp/amz-out; mkdir -p $OUT
D=.github/amazon
BT=$ANDROID_HOME/build-tools/$(ls $ANDROID_HOME/build-tools | sort -V | tail -1)
adb wait-for-device; adb shell getprop ro.build.version.release </dev/null | sed 's/^/android /' | tee $OUT/device.txt
adb shell pm list packages </dev/null | grep -iE "google|gms|vending|amazon" | tee -a $OUT/device.txt
A=$(ls amz-apk/*-amazon-release.apk | head -1)
PKG=$($BT/aapt2 dump packagename "$A" 2>/dev/null || cat $D/package.txt)
bash $D/amazon-test.sh "$A" "$PKG" $D/steps-release.txt release || true
# Amazon review (Nov 9 2026) flagged the "Ad-free rewards" button doing nothing on Fire. The amazon
# build must not show any rewards entry: fail loudly if any walked screen still names it.
if grep -il -E "ad-free rewards|open rewards|watch an ad|rewards" $OUT/release-*.txt > $OUT/rewards-check.txt 2>/dev/null; then
  echo "REWARDS CHECK: FAIL (rewards entry still visible in:)"; cat $OUT/rewards-check.txt
else
  echo "REWARDS CHECK: PASS (no rewards entry on any walked screen)" | tee $OUT/rewards-check.txt
fi
ls -la $OUT | tail -60
cat $OUT/*-fatal.txt 2>/dev/null | head -40
