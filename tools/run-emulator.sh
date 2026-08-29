#!/usr/bin/env bash
#
# Chạy thử Karaoke TV trên emulator Android TV, không cần box thật.
#
#   ./tools/run-emulator.sh                    # dùng 5 clip mẫu tự tạo
#   ./tools/run-emulator.sh ~/Music/Karaoke    # nạp thư mục karaoke của bạn
#
# Điều khiển trong cửa sổ emulator: phím mũi tên = D-pad, Enter = OK,
# Esc = BACK. Giữ Enter ~1 giây trên bài hát để mở thêm lựa chọn.

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$ANDROID_HOME/platform-tools/adb"
EMULATOR="$ANDROID_HOME/emulator/emulator"
AVD_NAME="karaoketv"
SYSTEM_IMAGE="system-images;android-34;android-tv;arm64-v8a"
PKG="com.athr.karaoketv"
MEDIA_SRC="${1:-}"

say() { printf '\n\033[1;35m==>\033[0m %s\n' "$1"; }

# --- máy ảo -----------------------------------------------------------------
if ! "$EMULATOR" -list-avds | grep -qx "$AVD_NAME"; then
  say "Tạo máy ảo Android TV (lần đầu, có thể tải ~1GB)"
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "emulator" "$SYSTEM_IMAGE" >/dev/null
  echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd \
    -n "$AVD_NAME" -k "$SYSTEM_IMAGE" -d tv_1080p --force >/dev/null
fi

if ! "$ADB" devices | grep -q "^emulator-.*device$"; then
  say "Khởi động emulator (cửa sổ sẽ hiện ra)"
  "$EMULATOR" -avd "$AVD_NAME" -no-snapshot -no-boot-anim >/tmp/karaoke-emulator.log 2>&1 &
  say "Chờ máy ảo boot xong…"
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
    sleep 2
  done
else
  say "Đã có emulator đang chạy, dùng luôn"
fi

# --- build và cài -----------------------------------------------------------
say "Build bản debug"
(cd "$PROJECT_DIR" && ./gradlew assembleDebug --console=plain -q)

say "Cài lên emulator"
"$ADB" install -r "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" >/dev/null

# Trên box thật bạn bật quyền này bằng tay trong app; ở đây cấp sẵn cho nhanh.
"$ADB" shell appops set --uid "$PKG" MANAGE_EXTERNAL_STORAGE allow

# --- nhạc --------------------------------------------------------------------
if [ -n "$MEDIA_SRC" ]; then
  say "Nạp nhạc từ $MEDIA_SRC"
  "$ADB" shell mkdir -p /sdcard/Karaoke
  "$ADB" push "$MEDIA_SRC/." /sdcard/Karaoke/
elif [ "$("$ADB" shell 'find /sdcard/Karaoke -name "*.mp4" 2>/dev/null | wc -l' | tr -d '\r ')" = "0" ]; then
  if ! command -v ffmpeg >/dev/null; then
    say "Chưa có nhạc và không tìm thấy ffmpeg. Chạy lại kèm đường dẫn thư mục karaoke."
  else
    say "Tạo 5 clip mẫu với tên file karaoke Việt (để thử bóc tách tên và tìm kiếm)"
    TMP="$(mktemp -d)"
    mkdir -p "$TMP/Nhac Tre/Den Vau" "$TMP/Nhac Tre/Ho Quang Hieu" "$TMP/Bolero" "$TMP/Nhac Vang"
    # Giọng giả lập ở kênh trái, nhạc nền ở kênh phải — để thử nút bỏ giọng ca sĩ.
    gen() {
      ffmpeg -y -loglevel error \
        -f lavfi -i "testsrc=size=640x360:rate=15:duration=$3" \
        -f lavfi -i "sine=frequency=440:duration=$3" \
        -f lavfi -i "sine=frequency=220:duration=$3" \
        -filter_complex "[1:a][2:a]join=inputs=2:channel_layout=stereo[a]" \
        -map 0:v -map "[a]" -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest "$1/$2"
    }
    gen "$TMP/Nhac Tre/Den Vau" "12345 - Gần Như Là - Đen Vâu [Karaoke Beat Tone Nam].mp4" 180
    gen "$TMP/Bolero" "Karaoke Duyên Phận - Tone Nữ (Beat chuẩn) HD.mp4" 120
    gen "$TMP/Nhac Tre" "[MTV] 60123 Chuyện Của Mùa Đông.mp4" 90
    gen "$TMP/Nhac Tre/Ho Quang Hieu" "Hồ Quang Hiếu - Con Đường Tôi Yêu [Karaoke 1080p].mp4" 90
    gen "$TMP/Nhac Vang" "Vọng Cổ Buồn - Karaoke Beat.mp4" 60
    "$ADB" shell mkdir -p /sdcard/Karaoke
    "$ADB" push "$TMP/." /sdcard/Karaoke/ >/dev/null
    rm -rf "$TMP"
  fi
fi

# --- chạy --------------------------------------------------------------------
say "Mở app"
"$ADB" shell am force-stop "$PKG"
"$ADB" shell am start -n "$PKG/.MainActivity" >/dev/null

cat <<'EOF'

  Trong emulator:
    1. Chọn "Internal shared storage" rồi Enter  ->  quét ra 5 bài
    2. Enter tiếp lên "Xong"                     ->  màn hình chính
    3. "Tìm bài", gõ  gannhula  bằng bàn phím    ->  ra "Gần Như Là"
    4. Enter để chọn bài, Esc hai lần            ->  ẩn menu, xem video
    5. Enter                                     ->  thanh chỉnh tông / kênh tiếng

  Phím: mũi tên = D-pad, Enter = OK, Esc = BACK
  Chụp màn hình:  $ANDROID_HOME/platform-tools/adb exec-out screencap -p > shot.png
  Tắt máy ảo:     $ANDROID_HOME/platform-tools/adb emu kill

EOF
