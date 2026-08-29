#!/usr/bin/env bash
#
# Cắt một bản phát hành: tăng số phiên bản, build APK đã ký, đẩy lên GitHub
# Releases kèm APK. Nút "Kiểm tra cập nhật" trong app đọc đúng chỗ này.
#
#   ./tools/release.sh 1.1
#   ./tools/release.sh 1.1 "Sửa lỗi quét ổ cứng"

set -euo pipefail

VERSION="${1:-}"
NOTES="${2:-}"
if [ -z "$VERSION" ]; then
  echo "Dùng: ./tools/release.sh <phiên-bản> [ghi-chú]" >&2
  echo "Ví dụ: ./tools/release.sh 1.1 \"Sửa lỗi quét ổ cứng\"" >&2
  exit 1
fi

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
GRADLE_FILE="$PROJECT_DIR/app/build.gradle.kts"
APK_NAME="KaraokeTV-$VERSION.apk"

say() { printf '\n\033[1;35m==>\033[0m %s\n' "$1"; }

cd "$PROJECT_DIR"

if [ ! -f keystore.properties ]; then
  echo "Thiếu keystore.properties — không ký được bản release." >&2
  echo "Không có key cũ thì bản mới KHÔNG cài đè lên bản đang dùng được." >&2
  exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "Còn thay đổi chưa commit. Commit trước rồi hãy cắt release." >&2
  git status --short >&2
  exit 1
fi

# versionCode phải tăng đều, nếu không Android từ chối cài đè.
CURRENT_CODE="$(grep -E '^\s*versionCode = ' "$GRADLE_FILE" | grep -oE '[0-9]+')"
NEXT_CODE=$((CURRENT_CODE + 1))

say "Tăng phiên bản: $VERSION (versionCode $CURRENT_CODE -> $NEXT_CODE)"
/usr/bin/sed -i '' "s/^\( *versionCode = \).*/\1$NEXT_CODE/" "$GRADLE_FILE"
/usr/bin/sed -i '' "s/^\( *versionName = \).*/\1\"$VERSION\"/" "$GRADLE_FILE"

say "Build bản release đã ký"
./gradlew assembleRelease --console=plain -q

BUILT="app/build/outputs/apk/release/app-release.apk"
[ -f "$BUILT" ] || { echo "Không thấy APK ở $BUILT" >&2; exit 1; }
cp "$BUILT" "/tmp/$APK_NAME"

say "Commit và gắn thẻ v$VERSION"
git add "$GRADLE_FILE"
git commit -q -m "Phiên bản $VERSION"
git tag -f "v$VERSION"
git push -q origin main
git push -q -f origin "v$VERSION"

say "Tạo bản phát hành trên GitHub"
gh release delete "v$VERSION" --yes >/dev/null 2>&1 || true
gh release create "v$VERSION" "/tmp/$APK_NAME" \
  --title "Karaoke TV $VERSION" \
  --notes "${NOTES:-Bản $VERSION}"

# Chỉ giữ bản mới nhất trên trang phát hành. Thẻ git thì giữ nguyên: chúng không
# tốn chỗ và vẫn cho phép dựng lại bất kỳ phiên bản nào.
say "Dọn các bản phát hành cũ"
for old in $(gh release list --limit 100 --json tagName -q '.[].tagName'); do
  if [ "$old" != "v$VERSION" ]; then
    gh release delete "$old" --yes >/dev/null 2>&1 && echo "  đã xoá $old"
  fi
done

say "Xong"
echo "  APK:  /tmp/$APK_NAME"
echo "  Trang: $(gh repo view --json url -q .url)/releases/tag/v$VERSION"
echo
echo "  Trên box: Cài đặt -> Phiên bản -> Kiểm tra -> Tải về -> Cài đặt"
