#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="${ROOT_DIR}/test/final"

mkdir -p "${OUTPUT_DIR}/synthetic" "${OUTPUT_DIR}/real" "${OUTPUT_DIR}/public"

if ! command -v xcrun >/dev/null 2>&1; then
  echo "当前脚本需要 macOS/Xcode 的 Swift 工具生成精确 PNG 夹具。" >&2
  exit 1
fi

xcrun swift "${ROOT_DIR}/scripts/generate_test_fixtures.swift" "${OUTPUT_DIR}/synthetic"

for image in 1.png.jpg 2.jpg 3.jpg 4.png 5.png; do
  if [[ -f "${ROOT_DIR}/test_pic/${image}" ]]; then
    cp "${ROOT_DIR}/test_pic/${image}" "${OUTPUT_DIR}/real/${image}"
  fi
done

curl --fail --location --silent --show-error \
  "https://www.gstatic.com/webp/gallery/4.webp" \
  --output "${OUTPUT_DIR}/public/google_webp_gallery_4.webp"

echo "测试图片已生成到 ${OUTPUT_DIR}"
find "${OUTPUT_DIR}" -type f -maxdepth 2 -print | sort
