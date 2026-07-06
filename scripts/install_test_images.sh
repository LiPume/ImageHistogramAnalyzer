#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE_DIR="${ROOT_DIR}/test_pic"
REMOTE_DIR="/sdcard/Pictures/ImageHistogramAnalyzer"

if command -v adb >/dev/null 2>&1; then
    ADB="$(command -v adb)"
else
    SDK_DIR="$(sed -n 's/^sdk.dir=//p' "${ROOT_DIR}/local.properties" 2>/dev/null | head -1)"
    ADB="${SDK_DIR}/platform-tools/adb"
fi

if [[ ! -x "${ADB}" ]]; then
    echo "错误：未找到 adb，请先配置 Android SDK。" >&2
    exit 1
fi

if [[ ! -d "${IMAGE_DIR}" ]]; then
    echo "错误：未找到 ${IMAGE_DIR}。" >&2
    exit 1
fi

SERIAL="${ANDROID_SERIAL:-}"
if [[ -z "${SERIAL}" ]]; then
    SERIALS="$(${ADB} devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
    DEVICE_COUNT="$(printf '%s\n' "${SERIALS}" | awk 'NF { count++ } END { print count + 0 }')"
    if [[ "${DEVICE_COUNT}" -ne 1 ]]; then
        echo "错误：检测到 ${DEVICE_COUNT} 个可用设备，请设置 ANDROID_SERIAL。" >&2
        "${ADB}" devices
        exit 1
    fi
    SERIAL="${SERIALS}"
fi

ADB_DEVICE=("${ADB}" -s "${SERIAL}")
"${ADB_DEVICE[@]}" shell mkdir -p "${REMOTE_DIR}"

shopt -s nullglob
IMAGES=(
    "${IMAGE_DIR}"/*.jpg
    "${IMAGE_DIR}"/*.jpeg
    "${IMAGE_DIR}"/*.png
    "${IMAGE_DIR}"/*.webp
)

if [[ "${#IMAGES[@]}" -eq 0 ]]; then
    echo "错误：${IMAGE_DIR} 中没有 JPG、PNG 或 WebP 图片。" >&2
    exit 1
fi

for image in "${IMAGES[@]}"; do
    filename="$(basename "${image}")"
    remote_file="${REMOTE_DIR}/${filename}"
    echo "安装测试图片：${filename}"
    "${ADB_DEVICE[@]}" push "${image}" "${remote_file}" >/dev/null
    "${ADB_DEVICE[@]}" shell am broadcast \
        -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
        -d "file://${remote_file}" >/dev/null
done

echo "已安装 ${#IMAGES[@]} 张图片到设备 ${SERIAL} 的 ${REMOTE_DIR}。"
echo "重新打开系统图片选择器即可看到测试图片。"
