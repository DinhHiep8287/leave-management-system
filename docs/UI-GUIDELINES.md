# UI Guidelines (Frontend)

Hướng dẫn thiết kế giao diện cho Leave Management System. Mục tiêu: **đẹp, rõ ràng,
nhất quán, mang cảm giác "người làm"** — tránh các dấu hiệu UI do AI sinh ra. Áp dụng
cho toàn bộ FE (Tuần 4, Part 3–6). Stack: React 18 + TS + Vite + Tailwind v3 + shadcn/ui
+ lucide-react.

## Triết lý

UI là **công cụ nội bộ** (dashboard, bảng, form) — ưu tiên sự rõ ràng và nhất quán, không
phải hiệu ứng. Để **typography + khoảng trắng + đường kẻ** gánh thẩm mỹ, **không** trang
trí bằng icon/emoji/gradient.

## Decisions đã chốt

| Hạng mục | Lựa chọn |
|---|---|
| Font | **Be Vietnam Pro** (qua `@fontsource/be-vietnam-pro`, không CDN runtime). Weights 400/500/600/700. |
| Accent | **Teal** (`teal-600` #0d9488, hover `teal-700`). KHÔNG dùng tím-AI. |
| Icon | **Text-led, tối thiểu.** Chỉ dùng lucide khi thật sự cần (điều hướng lịch, vài action chính). |
| Mật độ | Vừa phải (business tool) — thoáng nhưng không "airy" kiểu landing. |
| Dark mode | Có, qua class `.dark` + CSS vars của shadcn. |

## Tokens

- **Radius**: `--radius: 0.5rem` (8px); input/button 6px. Một hệ duy nhất.
- **Spacing**: thang bội số 4 (Tailwind mặc định). Card padding `p-5`/`p-6`, section `gap-6`.
- **Border/Shadow**: ưu tiên `border` mảnh (`border-border`) để phân tách; bóng đổ chỉ 1 cấp
  nhẹ (`shadow-sm`) cho popover/dropdown. KHÔNG đổ bóng nặng khắp nơi.
- **Accent (primary)**: teal-600 cho nút chính, link, nav active. Văn bản phụ dùng `muted-foreground`.

## Màu trạng thái đơn (badge: nền nhạt + chữ đậm, KHÔNG icon)

| Status | Màu |
|---|---|
| PENDING | amber (`amber-100` / `amber-800`) |
| APPROVED | green (`green-100` / `green-800`) |
| REJECTED | rose (`rose-100` / `rose-800`) |
| CANCELLED | slate (`slate-100` / `slate-700`) |

Accent teal tách biệt với green "đã duyệt" để không gây nhầm.

## Chính sách icon (quan trọng — tránh "icon spam")

- **Nav**: chữ, không icon (hoặc tối đa 1 icon rất tiết chế nếu cần).
- **Nút**: chữ rõ nghĩa ("Nộp đơn", "Duyệt", "Từ chối"), không gắn icon trang trí.
- **Trạng thái**: badge chữ + màu, **không** icon.
- **Được dùng icon**: điều hướng tháng ở calendar (chevron trái/phải), nút đóng dialog (×),
  loading spinner. Chỉ vậy.

## Quy tắc "no AI tells"

- KHÔNG font Inter mặc định; KHÔNG tím-AI / gradient tím-xanh.
- KHÔNG 3 card đều nhau y hệt nếu nội dung khác vai trò; phân cấp bằng kích thước/đậm nhạt.
- KHÔNG emoji trang trí; KHÔNG copy sáo ("Welcome back! 🎉").
- KHÔNG **em-dash (—)** trong text UI. Dùng dấu phẩy/gạch nối hoặc viết lại câu.
- Văn bản tiếng Việt tự nhiên, ngắn gọn, đúng ngữ cảnh nghiệp vụ.

## Accessibility

- Tương phản WCAG AA tối thiểu (kiểm cả light + dark).
- Tôn trọng `prefers-reduced-motion` (app này gần như không cần animation phức tạp).
- Mọi input có `<label>` liên kết; focus ring rõ ràng (giữ ring mặc định của shadcn).
