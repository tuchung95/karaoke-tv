# Karaoke TV

Ứng dụng karaoke cho Android TV box, phát video karaoke trực tiếp từ ổ cứng USB
cắm vào box. Toàn bộ giao diện được thiết kế cho remote D-pad — không có chỗ nào
cần chuột hay cảm ứng.

## Trải nghiệm trên TV box

- **Video luôn chạy nền.** Menu là một lớp phủ mờ bên trên. Chọn bài tiếp theo
  không làm gián đoạn người đang hát. `BACK` ở màn hình chính ẩn menu để xem video.
- **Hàng chờ đúng kiểu phòng karaoke.** Nhấn `OK` một lần trên bài hát là thêm vào
  hàng chờ. Giữ `OK` để có thêm lựa chọn: hát ngay, ưu tiên, yêu thích.
  Bấm "Hát ngay" thì bài đang hát dở được đẩy lại lên đầu hàng chờ chứ không mất.
- **Báo bài tiếp theo** hiện lên 25 giây trước khi hết bài, để người kế chuẩn bị.
- **Chỉnh tông ±6 nửa cung** không đổi tốc độ (Sonic pitch shift của ExoPlayer).
  Tông được giữ nguyên qua các bài.
- **Bỏ giọng ca sĩ** cho đĩa VCD/DVD rip có giọng ở một kênh: chọn kênh trái,
  kênh phải hoặc trộn mono. Với file có nhiều luồng tiếng thì đổi luồng tiếng.
- **Tỉ lệ khung hình** vừa khung / phóng to / kéo đầy, cho video 4:3 cũ trên TV 16:9.
- **Màn hình không tự tắt** khi đang mở app.

## Tìm bài

- **Gõ không dấu.** Chỉ mục lưu sẵn dạng đã bỏ dấu, nên `gan nhu la` ra `Gần Như Là`,
  `gannhula` (không cần phím cách) cũng ra.
- **Viết tắt.** `gnl` ra `Gần Như Là`.
- **Mã số bài.** Phím số trên remote gõ thẳng vào ô tìm kiếm, kể cả khi đang xem video.
- **Giọng nói (vi-VN)** ngay trong app, kết quả cập nhật trực tiếp trong lúc nói.
- **Duyệt theo thư mục / thể loại / ca sĩ**, lấy từ cấu trúc thư mục trên ổ cứng và
  từ tên file.

Tên file được bóc tách tự động: mã số, tên bài, ca sĩ, tone nam/nữ; các chữ rác như
`Karaoke`, `Beat chuẩn`, `HD`, `1080p`, `[MTV]` bị loại khỏi tên hiển thị và khỏi chỉ mục.

## Quét ổ cứng

Android TV không giống điện thoại ở chỗ này, và app xử lý theo đúng thực tế của box:

- **Hầu hết box Android TV không có trình chọn thư mục.** Bản TV stock không cài
  DocumentsUI; thay vào đó `com.android.tv.frameworkpackagestubs` đăng ký nhận
  intent chọn thư mục rồi chỉ hiện toast "You don't have an app that can do this".
  Vì có stub trả lời nên `resolveActivity` vẫn báo là có và lệnh mở không hề báo
  lỗi — app phải nhận diện stub theo tên, nếu không sẽ có một cái nút bấm vào
  không ra gì. Đã kiểm chứng trên emulator Android TV 14.
- **Từ Android 11, File API không đọc được ổ USB** nếu không có quyền toàn quyền tệp.

Nên màn hình đầu tiên tự dò xem box có gì rồi mới hiện lựa chọn tương ứng:

| Tình huống | App làm gì |
|---|---|
| Có trình chọn thư mục thật | Hiện "Chọn thư mục…", chọn ổ ở dưới sẽ mở sẵn trình chọn ngay tại ổ đó |
| Không có (đa số box Android TV) | Hiện "Cấp quyền đọc ổ cứng" → mở màn hình All-files access của TV, bật Karaoke TV, quay lại là quét được |
| Android 10 trở xuống | Quét thẳng đường dẫn, nhanh nhất |

Quét chạy nền theo lô. Đường SAF đọc thư mục bằng truy vấn hàng loạt chứ không đi
qua `DocumentFile`, nên vài nghìn file chỉ mất vài giây.

Số lần hát, bài yêu thích và thời lượng đã dò được giữ nguyên qua mỗi lần quét lại —
định danh bài dựa trên đường dẫn + tên + dung lượng, không dựa vào URI, nên cấp lại
quyền hay đổi cách truy cập cũng không mất.

Đuôi file nhận: mp4, mkv, avi, m4v, mov, ts, m2ts, mts, wmv, mpg, mpeg, flv, webm,
vob, dat, 3gp, ogv.

## Build

Máy này đã cài sẵn toolchain (OpenJDK 17 qua Homebrew, Android SDK ở
`~/Library/Android/sdk`). `local.properties` đã trỏ đúng `sdk.dir`.

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew assembleRelease   # -> app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug     # bản debug để chạy thử
```

Mở bằng Android Studio thì không cần export gì, IDE tự dùng JDK đi kèm.

### Ký APK

Bản release được ký bằng keystore riêng ở `keystore/karaoke-release.jks`, mật khẩu
nằm trong `keystore.properties`. Cả hai đều đã bị `.gitignore` loại ra.

**Giữ kỹ hai file này.** Android chỉ cho cài đè bản mới nếu nó được ký bằng đúng
key cũ; mất key thì phải gỡ app rồi cài lại từ đầu, và mất hết bài yêu thích cùng
lịch sử hát.

## Cài lên TV box (không cần adb)

1. Chép `app-release.apk` vào USB, hoặc tải về box qua trình duyệt / Send Files to TV.
2. Trên box mở **Cài đặt → Bảo mật & hạn chế → Nguồn không xác định**, bật cho
   trình duyệt hoặc trình quản lý file mà bạn dùng để mở APK.
3. Mở file APK bằng trình quản lý file (X-plore, File Commander, Total Commander…)
   rồi bấm Cài đặt.
4. Mở app. Trên box Android TV (kể cả TV360 B866V2F / ATV12) màn hình đầu sẽ hiện
   **"Cấp quyền đọc ổ cứng"** — bấm vào, tìm **Karaoke TV** trong danh sách, gạt bật,
   rồi bấm BACK quay lại app.
5. Chọn ổ USB trong mục "Ổ đĩa tìm thấy trên máy". App quét xong là hát được.

App xuất hiện ở màn hình chính của Android TV (`LEANBACK_LAUNCHER`) và cả launcher
thường, nên chạy được trên cả box Android TV lẫn box Android phổ thông.

- `minSdk` 26 (Android 8.0) — phủ gần hết TV box đang bán.
- `targetSdk`/`compileSdk` 35.

## Cấu trúc

```
data/library   Quét ổ cứng (SAF + đường dẫn trực tiếp), bóc tách tên file
data/db        Room: chỉ mục bài hát, tìm kiếm có xếp hạng
data/repo      Gộp chỉ mục + nguồn nhạc
player         ExoPlayer, hàng chờ, chỉnh tông, xử lý kênh tiếng
ui             Compose cho TV: home, tìm kiếm, duyệt, hàng chờ, phát, cài đặt
```

## Phím tắt trên remote

| Phím | Khi đang xem video | Trong menu |
|---|---|---|
| `OK` / `DOWN` | Mở thanh điều khiển | Chọn bài (thêm vào hàng chờ) |
| Giữ `OK` | — | Mở thêm lựa chọn cho bài hát |
| `UP` | Mở menu | Di chuyển |
| `BACK` | Mở menu | Quay lại, ở màn hình chính thì ẩn menu. Thoát app cần bấm hai lần |
| `0`–`9` | Mở tìm kiếm với số vừa gõ | Gõ vào ô tìm kiếm |
| Chữ cái | — | Gõ thẳng vào ô tìm kiếm (remote có bàn phím, app remote trên điện thoại) |
| `PLAY/PAUSE`, `NEXT` | Phát/dừng, bài kế | — |
