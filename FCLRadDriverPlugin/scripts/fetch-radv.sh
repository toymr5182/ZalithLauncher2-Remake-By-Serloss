#!/usr/bin/env bash
set -euo pipefail

VERSION="v1.8.0"
ZIP_NAME="radv-xclipse-27.0.0-devel-269238e.zip"
SHA256="0545b1b24817cc0ed4a9e4fc440a7080cbaf69a9ce5d1e4bb22b0e697129c223"
URL="https://github.com/JimVulkan/radv-xclipse/releases/download/${VERSION}/${ZIP_NAME}"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB_DIR="${ROOT_DIR}/app/src/main/jniLibs/arm64-v8a"
ASSET_DIR="${ROOT_DIR}/app/src/main/assets"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

curl -fsSL -o "${TMP_DIR}/${ZIP_NAME}" "${URL}"
(
    cd "${TMP_DIR}"
    echo "${SHA256}  ${ZIP_NAME}" | sha256sum -c -
)

mkdir -p "${LIB_DIR}" "${ASSET_DIR}"
python3 - "${TMP_DIR}/${ZIP_NAME}" "${LIB_DIR}" "${ASSET_DIR}" <<'PY'
import os
import shutil
import sys
import zipfile

zip_path, lib_dir, asset_dir = sys.argv[1:4]
targets = {
    "vulkan.radeon.so": lib_dir,
    "NOTICE.txt": asset_dir,
    "meta.json": asset_dir,
}

with zipfile.ZipFile(zip_path) as archive:
    for name, dest_dir in targets.items():
        os.makedirs(dest_dir, exist_ok=True)
        with archive.open(name) as src, open(os.path.join(dest_dir, name), "wb") as out:
            shutil.copyfileobj(src, out)
        print(f"extracted {name} -> {dest_dir}")
PY
