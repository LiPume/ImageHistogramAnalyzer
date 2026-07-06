#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REFERENCE_DIR="${ROOT_DIR}/references/open_source"

mkdir -p "${REFERENCE_DIR}"

sync_repository() {
    local name="$1"
    local url="$2"
    local destination="${REFERENCE_DIR}/${name}"

    if [[ -d "${destination}/.git" ]]; then
        local current_url
        current_url="$(git -C "${destination}" remote get-url origin)"

        if [[ "${current_url}" != "${url}" ]]; then
            echo "错误：${destination} 的 origin 为 ${current_url}，预期为 ${url}。" >&2
            return 1
        fi

        if [[ -n "$(git -C "${destination}" status --porcelain)" ]]; then
            echo "跳过更新 ${name}：本地存在未提交变更，请先自行处理。"
            return 0
        fi

        echo "更新 ${name}..."
        git -C "${destination}" pull --ff-only
        return 0
    fi

    if [[ -e "${destination}" ]]; then
        echo "错误：${destination} 已存在，但不是 Git 仓库。" >&2
        return 1
    fi

    echo "拉取 ${name}..."
    git clone "${url}" "${destination}"
}

sync_repository "pixel-proc" "https://github.com/sixo/pixel-proc.git"
sync_repository "histogram" "https://github.com/billthefarmer/histogram.git"
sync_repository "awesome-android-agent-skills" "https://github.com/new-silvermoon/awesome-android-agent-skills.git"

echo "参考仓库已准备在 ${REFERENCE_DIR}"
