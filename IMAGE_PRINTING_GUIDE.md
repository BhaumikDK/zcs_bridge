# Image & Logo Printing Guide

## Overview

The ZCS SDK Plugin can print **images and logos** (PNG, JPG, BMP, etc.) directly to the receipt printer. It uses the SDK's native bitmap printing (`Printer.setPrintAppendBitmap`) under the hood.

There are two ways to print an image:

1. **`printImage()`** — print a standalone image/logo on its own.
2. **Logo in `printDynamic()`** — embed a logo at the top of a receipt, above the business name.

## Key Features

✅ **Any standard image format** — PNG, JPG, BMP, etc. (anything Android's `BitmapFactory` can decode)
✅ **Raw bytes over the channel** — images travel as `Uint8List` → `byte[]`, no base64 overhead
✅ **Automatic scaling** — fit to paper width (~384 dots for 58mm, ~576 for 80mm), aspect ratio preserved
✅ **Optional explicit width** — control the printed size in printer dots
✅ **Alignment** — `left`, `center` (default), or `right`
✅ **Safe logo embedding** — a bad logo image is skipped, the rest of the receipt still prints
✅ **Works with any image source** — assets, files, or network (just supply the bytes)

## Prerequisites

The device must be initialized and opened before printing:

```dart
final plugin = ZcsSdkPlugin();
await plugin.initializeDevice();
await plugin.openDevice();
```

---

## 1. `printImage()` — Standalone Image / Logo

### Signature

```dart
Future<Map<String, dynamic>> printImage(
  Uint8List imageBytes, {
  String align = 'center',
  int? width,
})
```

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `imageBytes` | `Uint8List` | required | Encoded image data (PNG/JPG/BMP/…) |
| `align` | `String` | `'center'` | Horizontal alignment: `'left'`, `'center'`, or `'right'` |
| `width` | `int?` | `null` | Target width in **printer dots**. If omitted, the image is fit to the paper width. Values larger than the paper width are clamped. |

### Returns

A map like `{'success': true, 'message': 'Image printed successfully'}`.
On failure a `SmartPosException` is thrown.

### Example — logo from an asset

```dart
import 'package:flutter/services.dart' show rootBundle;
import 'package:zcs_sdk_plugin/zcs_sdk_plugin.dart';

final plugin = ZcsSdkPlugin();
await plugin.initializeDevice();
await plugin.openDevice();

final data = await rootBundle.load('assets/logo.png');
final bytes = data.buffer.asUint8List();

await plugin.printImage(bytes, align: 'center', width: 300);
```

> Remember to declare the asset in your app's `pubspec.yaml`:
> ```yaml
> flutter:
>   assets:
>     - assets/logo.png
> ```

### Example — image from a file

```dart
import 'dart:io';

final bytes = await File('/storage/emulated/0/Download/logo.png').readAsBytes();
await plugin.printImage(bytes);
```

### Example — image from the network

```dart
import 'package:http/http.dart' as http;

final res = await http.get(Uri.parse('https://example.com/logo.png'));
await plugin.printImage(res.bodyBytes, align: 'center');
```

---

## 2. Logo Inside `printDynamic()`

To print a logo at the very top of a receipt (above the business name), add these keys to the `printDynamic` args map:

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `logoBytes` | `Uint8List` | — | Encoded image data. If present, printed at the top. |
| `logoWidth` | `int` | fit to paper | Target width in printer dots (optional) |
| `logoAlign` | `String` | `'center'` | `'left'`, `'center'`, or `'right'` (optional) |

### Example

```dart
final logo = (await rootBundle.load('assets/logo.png')).buffer.asUint8List();

await plugin.printDynamic({
  'logoBytes': logo,        // Uint8List
  'logoWidth': 300,         // optional
  'logoAlign': 'center',    // optional
  'businessName': 'YatriKart',
  'header': 'TAX INVOICE',
  'fields': {
    'Invoice #': 'INV-2026-001',
    'Date': '2026-07-17',
  },
  'items': [
    {'Item': 'Bus Ticket', 'Qty': '1', 'Unit': '250', 'Total': '250'},
  ],
  'totals': {'Total': 'INR 250'},
  'footer': 'Thank you for travelling with us!',
}, bothCopies: false);
```

When `bothCopies: true`, the logo is printed on **both** the customer and merchant copies.

---

## Sizing Guidance

Sizes are expressed in **printer dots**, not pixels or millimetres:

| Paper width | Full printable width | Typical logo width |
|-------------|----------------------|--------------------|
| 58 mm | ~384 dots | 200–300 dots |
| 80 mm | ~576 dots | 300–450 dots |

- If you omit `width`, the image is scaled to fit the full paper width (only downscaling — small images are not blown up).
- If you pass `width`, the image is scaled to exactly that width (clamped to the paper's maximum), preserving aspect ratio.
- For sharp logos, prepare source images that are **monochrome / high contrast**. Thermal printers print in black and white — colours and fine gradients render poorly.

---

## Error Handling

```dart
try {
  await plugin.printImage(bytes, width: 300);
} on SmartPosException catch (e) {
  print('Print failed: ${e.message}');
}
```

Common error causes:

| Message | Cause |
|---------|-------|
| `Device must be initialized first` | Call `initializeDevice()` before printing |
| `Device must be opened first` | Call `openDevice()` before printing |
| `Image data cannot be null or empty` | `imageBytes` was empty |
| `Failed to decode image data ...` | Bytes were not a valid/supported image format |
| `Out of paper` | Printer is out of paper |

> Note: inside `printDynamic`, a logo that fails to decode is **silently skipped** (logged as a warning) so the rest of the receipt still prints.

---

## How It Works (Internals)

1. Flutter sends the raw `Uint8List` over the method channel; it arrives in Java as `byte[]`.
2. `BitmapFactory.decodeByteArray` decodes it into an Android `Bitmap`.
3. The bitmap is scaled to fit the paper width (or the requested `width`), preserving aspect ratio.
4. `Printer.setPrintAppendBitmap(bitmap, alignment)` appends it to the print buffer.
5. `Printer.setPrintStart()` flushes the buffer to paper (for `printImage`; `printDynamic` batches everything first).

Relevant source:
- Dart API: [lib/zcs_sdk_plugin.dart](lib/zcs_sdk_plugin.dart)
- Method channel: [lib/zcs_sdk_plugin_method_channel.dart](lib/zcs_sdk_plugin_method_channel.dart)
- Native implementation: [android/src/main/java/com/example/zcs_sdk_plugin/ZcsSdkPlugin.java](android/src/main/java/com/example/zcs_sdk_plugin/ZcsSdkPlugin.java) (`printImage`, `appendImage`)
