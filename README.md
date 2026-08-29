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

Khi máy đoán ngược tên bài và ca sĩ — chuyện khó tránh vì file karaoke Việt dùng cả
"Tên bài - Ca sĩ" lẫn "Ca sĩ - Tên bài" — giữ OK trên bài hát rồi chọn
**"Đổi tên bài ↔ ca sĩ"**. Sửa một lần, giữ nguyên qua các lần quét lại.

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

## YouTube

Màn hình tìm kiếm có nút **"Tìm bài này trên YouTube"**: gõ tên bài rồi bấm, app
mở YouTube trên box ngay tại trang kết quả (tự thêm chữ "karaoke" vào từ khoá,
tắt được trong Cài đặt).

**Không quảng cáo đến từ app YouTube, không phải từ app này.** Chỉ cần đăng nhập
tài khoản Premium trong app YouTube trên box là xong — app karaoke không cần đăng
nhập gì, và cũng không có chỗ nào để đăng nhập.

Phát cố ý để nguyên trong player của YouTube thay vì kéo về ExoPlayer của app.
Đó là điều khoản của YouTube yêu cầu, và cũng là cách duy nhất giữ được Premium:
ad-free do player chính thức thực thi theo tài khoản đang đăng nhập, nên luồng
phát ở bất kỳ chỗ nào khác đều dính quảng cáo kể cả khi bạn đã trả tiền.

Đánh đổi thì có thật: bài mở từ YouTube nằm ngoài bộ phát của app, nên **không
chỉnh tông, không bỏ giọng ca sĩ, không vào hàng chờ được**. Ba thứ đó chỉ áp
dụng cho file trên ổ cứng.

## Chọn nguồn nhạc

Ba đường, app tự dò xem box hỗ trợ đường nào:

1. **Quét video trên máy** (khuyên dùng) — đọc thư viện video của hệ thống qua
   MediaStore, thấy được cả video trên USB. Chỉ cần quyền đọc video, xin bằng hộp
   thoại thường mà máy Android nào cũng có.
2. **Chọn thư mục** — cần box có sẵn trình chọn thư mục của hệ thống.
3. **Cấp quyền đọc ổ cứng** rồi quét thẳng đường dẫn — cần box có màn hình cấp
   quyền toàn tệp.

Đường 1 tồn tại vì đường 2 và 3 đều tắc trên máy thật: TV360 B866V2F chạy ATV12
**không có cả trình chọn thư mục lẫn màn hình cấp quyền**, nên hai nút kia dẫn đến
ngõ cụt. MediaStore thì luôn có.

## Tự cập nhật

Cài đặt → **Phiên bản** → Kiểm tra → Tải về → Cài đặt. App đọc bản phát hành mới
nhất trên GitHub Releases, tải APK rồi giao cho trình cài đặt của hệ thống. Lần đầu
box sẽ hỏi cấp quyền cài đặt cho app; bật xong quay lại bấm Cài đặt lần nữa.

APK tải về nằm ở `Download/` chứ không phải thư mục riêng của app, vì không phải
box nào cũng có trình cài đặt APK — emulator Android TV thì không có activity nào
nhận intent đó, và `startActivity` vẫn không báo lỗi khi không ai nhận. Khi gặp
trường hợp đó app nói thẳng đường dẫn để bạn mở bằng trình quản lý file.

Phía người phát hành:

```bash
./tools/release.sh 1.2 "Ghi chú thay đổi"
```

Script tăng versionCode (bắt buộc, thiếu là Android từ chối cài đè), build bản đã
ký, gắn thẻ, đẩy lên và đính APK vào release. Bản mới **phải ký bằng đúng keystore
cũ**, nếu không sẽ không cài đè được.

## Màn hình chính

Dựng theo bố cục home của Google TV: một hàng trên gồm phím tìm kiếm tròn, dải tab
(`TabRow`/`Tab`) cho các điểm đến, nhóm nút tròn tiện ích (`IconButton`) và tóm tắt
thư viện; bên dưới là dải hero tràn viền lấy ảnh từ video, rồi các hàng thẻ 16:9.

Hero hiện bài đang hát; lúc rảnh thì đưa ra bài nhà này hát nhiều nhất — lời mời
tốt hơn một khoảng trống.

Ảnh hero tràn hết mép màn hình vì hướng dẫn Google yêu cầu phần tử nền không bị cắt
theo vùng overscan; riêng chữ vẫn nằm trong lề an toàn.

## Thanh trên

Nút tròn chỉ có icon hiện nhãn khi được focus. Nhãn vẽ bằng `Popup` chứ không chiếm
chỗ trong bố cục — nếu chiếm chỗ thì cả thanh sẽ xê dịch mỗi lần focus nhảy sang nút
khác.

## Phím khi đang xem video

| Phím | Tác dụng |
|---|---|
| ▲ ▼ | Thanh điều khiển |
| OK | Phát / tạm dừng |
| ◀ ▶ | Tua 10 giây |
| BACK | Về menu |

Âm thanh: focus và chọn dùng hiệu ứng có sẵn của hệ thống, nên khớp với mọi app
khác trên box và tự im khi người dùng tắt âm hệ thống. Tìm bằng giọng nói có tông
riêng lúc bắt đầu nghe, lúc nhận được kết quả và lúc thất bại — đó là thao tác duy
nhất không có gì chuyển động trên màn hình. Tắt được trong Cài đặt → Phát nhạc.

## Màu

Dùng thẳng **bảng màu baseline của Material 3** (dark), không pha thêm: mỗi vai trò
là một tông Google đã cân sẵn với các tông còn lại.

Theo [hệ màu Android TV](https://developer.android.com/design/ui/tv/guides/styles/color-system),
`ColorScheme` được điền **đủ cả 29 vai trò**. Bỏ trống không có nghĩa là để rỗng —
chúng rơi về màu mặc định, nên component nào chạm tới `tertiaryContainer` hay
`surfaceTint` sẽ tự sơn một màu không thuộc bảng nào.

Đo lại trên nền `#141218`, mốc WCAG 4.5:1 cho chữ thường và 3:1 cho ranh giới:

| Cặp màu | Tỉ lệ |
|---|---|
| onSurface | 14.35 |
| Accent | 10.94 |
| Primary / Muted | 10.91 |
| Danger | 10.89 |
| Success | 10.22 |
| Divider | 5.87 |
| OnPrimary trên Primary | 7.71 |

## Hàng chờ

Dạng immersive list: bài mà remote đang dừng trên đó phủ kín phía sau, hàng thẻ
chạy dọc dưới cùng, và năm thao tác nằm **một lần** ở khung trên thay vì lặp trên
từng dòng.

Hai bản trước đều sai. Bản đầu treo cả năm nút vào mỗi dòng — thành bức tường nút,
lại so le vì bài đầu không "lên" được và bài cuối không "xuống" được. Bản thứ hai
dùng navigation drawer, nhưng [Google định component đó cho 3–7 điểm đến của
app](https://developer.android.com/design/ui/tv/guides/components/navigation-drawer),
không phải danh sách không giới hạn.

## Cài đặt

Đây mới là chỗ navigation drawer đúng chỗ: 5 nhóm — Thư viện, Màn hình chính, Phát
nhạc, YouTube, Ứng dụng — nằm gọn trong khoảng 3–7 mà tài liệu khuyến nghị. Rail
thu gọn chỉ hiện icon, bung ra chữ khi focus chạm vào, nội dung ở khung phải.

## Tìm kiếm

Kết quả là lưới thẻ có thumbnail chứ không phải danh sách chữ — nhìn từ xa chọn
nhanh hơn.

## Đối chiếu chuẩn chất lượng app TV của Google

Soát theo [TV app quality](https://developer.android.com/docs/quality-guidelines/tv-app-quality),
mức Tier 3 (TV Ready) — mức bắt buộc:

| Mã | Yêu cầu | Trạng thái |
|---|---|---|
| TV-LM, TV-ML | Hiện trong launcher qua `LEANBACK_LAUNCHER` | Đạt |
| TV-LB, TV-BN | Banner 320×180 **có tên app**, icon 160×160 | Đã sửa — banner cũ chỉ có hình, không có chữ |
| TV-LO, TV-TR | Ngang, phủ kín màn hình, nền đục | Đạt |
| TV-OV | Không có gì bị cắt ở mép | Đã sửa — dòng full-width khi focus bị phóng to lấn vào vùng overscan |
| TV-DP, TV-DM | Điều khiển hoàn toàn bằng D-pad, không phụ thuộc phím Menu | Đạt |
| TV-DB | BACK dẫn về màn hình chính của TV | Đạt (bấm hai lần để tránh thoát nhầm) |
| TV-PS, TV-MT | minSdk ≤ 31, không đòi cảm ứng | Đạt (minSdk 26) |
| TV-PC | Phím giữa = phát/dừng, trái/phải = tua | Đã sửa — trước đó phím giữa mở thanh điều khiển |
| TV-PP | Phím PLAY/PAUSE đổi trạng thái phát | Đạt |
| TV-NP | Không phát tiếp khi rời app mà không có điều khiển hệ thống | Đã sửa — giờ dừng ở `onStop` |
| TV-BU, TV-BY | Chỉ chặn Ambient Mode khi đang phát | Đã sửa — trước đó giữ màn hình sáng vô điều kiện |
| TV-WB, TV-AP…TV-AA, TV-IC…TV-IX, TV-LG, TV-GP | Web, quảng cáo, PiP, game | Không áp dụng |

## Bố cục và giao diện

Dựng trên **Compose for TV Material** (`androidx.tv:tv-material`) — design system
chính chủ của Google cho TV — chứ không phải Material3 của điện thoại, và dùng
thẳng component của nó thay vì tự chế:

| Chỗ dùng | Component |
|---|---|
| Nút bấm | `Button` |
| Thẻ bài hát, thẻ ca sĩ / thể loại | `Card` |
| Dòng kết quả, hàng chờ, cài đặt, ổ đĩa | `ListItem` |
| Bật/tắt trong Cài đặt | `Switch` |
| Phím bàn phím ảo | `Button` |
| Màu, thang chữ | `MaterialTheme`, `darkColorScheme` |

Nghĩa là hiệu ứng focus (phóng to, đảo màu, viền, quầng sáng) và thang chữ 10-foot
lấy thẳng từ thư viện, không phải số tôi tự đặt; app focus trông giống mọi app
Android TV khác. Chỉ bảng màu là riêng.

Theo [hướng dẫn thiết kế Android TV](https://developer.android.com/design/ui/tv):
khung 960×540dp, lề an toàn 58dp hai bên và 28dp trên dưới, lưới 12 cột với gutter
20dp — 844dp nội dung chia vừa đúng 3 thẻ bài hát (268dp) hoặc 4 thẻ nhóm (196dp).

Ba nguyên tắc TV của Google được áp dụng cụ thể: **10-foot UI** (thang chữ TV, ít
chữ, phân cấp rõ), **điều hướng D-pad** (mọi thứ bấm được đều có focus thấy rõ, focus
ban đầu luôn được đặt sẵn nên cú bấm đầu tiên không bị mất), và **thiết bị dùng
chung** (Bố cục màn hình chính cho ẩn các hàng lịch sử hát khi cả nhà dùng chung box).

Màn hình chính tuỳ chỉnh được: Cài đặt → **Bố cục màn hình chính** cho phép ẩn/hiện
và đổi thứ tự từng hàng (Đang chờ, Hát gần đây, Yêu thích, Hát nhiều nhất, Thể loại,
Ca sĩ, Mới thêm).

Mọi màn hình dưới Trang chủ đều có thanh **Quay lại / Trang chủ / Xem video** ở trên
cùng, và nó cũng là nơi focus rơi vào khi mở màn hình — phím BACK vẫn làm được điều
đó, nhưng không phải remote nào cũng có phím BACK rõ ràng, và khách cầm remote giữa
buổi hát thì không biết cử chỉ đó tồn tại.

## Thử trên máy Mac trước, không cần box

```bash
./tools/run-emulator.sh                    # 5 clip mẫu tên tiếng Việt
./tools/run-emulator.sh ~/Music/Karaoke    # hoặc nạp thư mục của bạn
```

Script lo hết: tạo máy ảo Android TV nếu chưa có, boot, build, cài, cấp sẵn
quyền đọc ổ (trên box thật bước này bạn bấm trong app), nạp nhạc rồi mở app.

Trong cửa sổ emulator: **mũi tên = D-pad, Enter = OK, Esc = BACK**. Giữ Enter
khoảng một giây trên bài hát để mở thêm lựa chọn.

Máy ảo dựng sẵn ổ trong chứ không có cổng USB, nên nó kiểm được giao diện, tìm
kiếm, bóc tách tên file, hàng chờ, chỉnh tông và điều hướng remote — nhưng không
thay được việc cắm ổ cứng thật vào box.

Tắt máy ảo: `~/Library/Android/sdk/platform-tools/adb emu kill`

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
