#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERIAL="${ANDROID_SERIAL:-}"

if [[ -z "${SERIAL}" ]]; then
    echo "错误：请先设置 ANDROID_SERIAL，例如 emulator-5554。" >&2
    exit 1
fi

if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    SDK_DIR="$(sed -n 's/^sdk.dir=//p' "${ROOT_DIR}/local.properties" | head -1)"
    ADB="${SDK_DIR}/platform-tools/adb"
fi

"${ADB}" -s "${SERIAL}" logcat -c
ANDROID_SERIAL="${SERIAL}" "${ROOT_DIR}/gradlew" \
    :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=com.lzx.imagehistogramanalyzer.performance.HistogramVersionBenchmarkTest \
    -Pandroid.testInstrumentationRunnerArguments.runPerformanceBenchmark=true

echo
echo "以下为中文 CSV 数据："
"${ADB}" -s "${SERIAL}" logcat -d -s HistogramBenchmark:I '*:S' \
    | sed -n 's/^.*HistogramBenchmark: //p'
