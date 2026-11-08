#!/usr/bin/env bash
set -eo pipefail

echo "================================================================="
echo "=== [T-010] Verification: buf breaking against main/base branch ==="
echo "================================================================="

# 1. Определение базовой ветки для проверки (main, master или git base ref)
TARGET_BRANCH="${TARGET_BRANCH:-${1:-main}}"

if git rev-parse --verify "origin/$TARGET_BRANCH" >/dev/null 2>&1; then
    BASE_REF="origin/$TARGET_BRANCH"
elif git rev-parse --verify "$TARGET_BRANCH" >/dev/null 2>&1; then
    BASE_REF="$TARGET_BRANCH"
elif git rev-parse --verify "origin/master" >/dev/null 2>&1; then
    BASE_REF="origin/master"
elif git rev-parse --verify "master" >/dev/null 2>&1; then
    BASE_REF="master"
else
    BASE_REF="HEAD~1"
fi

echo "Target base branch for compatibility verification: $BASE_REF"

# Проверяем, есть ли .proto файлы в базовой ветке
if ! git ls-tree -r "$BASE_REF" proto 2>/dev/null | grep -q '\.proto$'; then
    echo "[WARN] No .proto files found in base ref $BASE_REF. Skipping breaking verification."
    exit 0
fi

# 2. Негативный тест: намеренное внесение breaking change для проверки детектора
echo ""
echo "--- Step 1: Simulating intentional breaking change in proto contract ---"
PROTO_FILE="proto/nutritionist/profile/v1/profile.proto"

if [ ! -f "$PROTO_FILE" ]; then
    echo "Error: Target proto file $PROTO_FILE does not exist!"
    exit 1
fi

# Делаем резервную копию
cp "$PROTO_FILE" "${PROTO_FILE}.bak"

# Намеренно удаляем поле 'string id = 1;' и меняем номер тега у user_id (критический breaking change)
sed -i.tmp '/string id = 1;/d' "$PROTO_FILE" && rm -f "${PROTO_FILE}.tmp"

echo "Modified $PROTO_FILE: intentionally removed field 'id = 1'."

# Запускаем buf breaking: ожидаем падение (exit code != 0)
set +e
buf breaking proto --against ".git#branch=$BASE_REF,subdir=proto" > /tmp/buf-breaking-output.txt 2>&1
BREAKING_EXIT_CODE=$?
set -e

# Восстанавливаем оригинальный proto файл
mv "${PROTO_FILE}.bak" "$PROTO_FILE"

echo "Buf breaking check exited with code: $BREAKING_EXIT_CODE"
cat /tmp/buf-breaking-output.txt

if [ "$BREAKING_EXIT_CODE" -eq 0 ]; then
    echo "[FAILED] Critical error: buf breaking DID NOT detect intentional breaking change!"
    exit 1
fi

echo "[SUCCESS] Verified: Intentional breaking change was caught and failed the check as expected!"

# 3. Позитивный тест: проверка актуальных чистых контрактов ветки
echo ""
echo "--- Step 2: Running clean buf breaking check on actual branch contracts ---"
buf breaking proto --against ".git#branch=$BASE_REF,subdir=proto"

echo "[SUCCESS] Actual contracts are backward-compatible with $BASE_REF!"
