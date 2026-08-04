# zcs_sdk_plugin

A Flutter plugin that provides a wrapper around the [ZCS SDK](https://www.szzcs.com/), allowing developers to interact with ZCS-compatible hardware devices from Flutter apps.

> ⚠️ **Disclaimer:** This plugin is an unofficial integration of the ZCS SDK. It does not include the SDK itself and is not affiliated with or endorsed by ZCS. You must obtain and configure the original SDK separately as per ZCS's licensing and documentation.

---

## ✨ Features

### 🖨️ **Universal Dynamic Printing Engine**
- **Single method prints any document type** - receipts, invoices, reports, warehouse dispatches, etc.
- **Automatic field skipping** - null or empty fields are automatically omitted
- **Flexible layout styles** - choose from `simple`, `detailed`, or `compact`
- **Smart formatting** - auto-text wrapping, truncation, and alignment
- **Dynamic QR codes** - generate QR codes from any field value
- **Dual copy printing** - print customer and merchant copies with one call
- **Nested map support** - handle complex data structures recursively
- **UTF-8 support** - properly renders "KSh" and other special characters

### 🔧 **Device Management**
- Initialize ZCS hardware devices
- Open/close device connections
- Query device status
- **Get device serial number** - retrieve unique device identifier
- QR code scanning support

This plugin provides a powerful, flexible API for ZCS POS hardware integration in Flutter apps.

---

## 🛠 Installation

Add this to your `pubspec.yaml`:

```yaml
dependencies:
  zcs_sdk_plugin: ^<latest-version>
```

Then run:

```bash
flutter pub get
```

📌 Replace `<latest-version>` with the latest version from pub.dev.

---

## ⚙️ Platform Setup

### Android
1. Ensure you have added the official ZCS SDK `.aar` or `.jar` files to your Android project
2. Add any required permissions to your `AndroidManifest.xml` (as specified by ZCS)
3. Initialize the SDK in your app as needed (see example below)

### iOS
⚠️ Currently only Android is supported. iOS support may be added in the future if ZCS provides a native iOS SDK.

---

## 🚀 Quick Start

```dart
import 'package:zcs_sdk_plugin/zcs_sdk_plugin.dart';

final plugin = ZcsSdkPlugin();

// 1. Initialize device
await plugin.initializeDevice();
await plugin.openDevice();

// 2. Print any document dynamically
await plugin.printDynamic({
  'businessName': 'mesh Logistics',
  'header': 'DISPATCH RECEIPT',
  'subHeader': 'Warehouse #3 - Nairobi',
  'fields': {
    'Customer Name': 'John Mwangi',
    'Loader': 'Peter Otieno',
    'Weight': '320 KG',
    'Pieces': '15',
  },
  'items': [
    {'Item': '20L Water', 'Qty': '2', 'Unit': '50', 'Total': '100'},
    {'Item': 'Delivery Fee', 'Qty': '1', 'Unit': '30', 'Total': '30'},
  ],
  'totals': {
    'Subtotal': '130',
    'Tax': '0',
    'Total': 'KSH 130',
  },
  'footer': 'Thank you for choosing mesh',
  'qrCodeField': 'dispatchId',
  'dispatchId': 'DSP-2025-001',
  'layoutStyle': 'detailed',
}, bothCopies: true);

// 3. Close device
await plugin.closeDevice();
```

---

## 📖 Documentation

### Universal Dynamic Printing

The `printDynamic()` method can print any type of document. Simply provide a map with the fields you need:

#### Available Fields

| Field | Type | Description |
|-------|------|-------------|
| `businessName` | String | Business name (centered, bold, large) |
| `header` | String | Document title (centered, bold) |
| `subHeader` | String | Subtitle (centered) |
| `fields` | Map | Key-value pairs to print |
| `items` | List | Items to render as table |
| `totals` | Map | Financial totals section |
| `footer` | String | Footer message (centered, small) |
| `qrCodeField` | String | Field name to encode as QR |
| `layoutStyle` | String | `simple`, `detailed`, or `compact` |

**All fields are optional** - the engine automatically skips any field that is null, empty, or not provided.

### Examples

### Print Raw Text

The simplest way to print - just pass a raw text string:

```dart
final plugin = ZcsSdkPlugin();
await plugin.initializeDevice();
await plugin.openDevice();

// Print raw text
await plugin.printRawText('Hello, World!\nThis is a test print.\n');
```

This method:
- Prints plain text without any formatting structure
- Supports newline characters (`\n`) for multiple lines
- Uses default font size (24) and left alignment
- Perfect for simple printing needs

#### Restaurant Receipt
```dart
await plugin.printDynamic({
  'businessName': 'Mama Oliech Restaurant',
  'header': 'ORDER RECEIPT',
  'fields': {
    'Order #': 'ORD-1234',
    'Date': '2025-01-15',
    'Time': '14:30',
  },
  'items': [
    {'Item': 'Fish Fillet', 'Qty': '2', 'Unit': '450', 'Total': '900'},
    {'Item': 'Ugali', 'Qty': '2', 'Unit': '80', 'Total': '160'},
  ],
  'totals': {
    'Subtotal': '1,180',
    'Service Charge': '118',
    'Total': 'KSH 1,298',
  },
  'footer': 'Enjoy your meal!',
}, bothCopies: false);
```

#### Simple Invoice
```dart
await plugin.printDynamic({
  'businessName': 'Tech Solutions Ltd',
  'header': 'INVOICE',
  'fields': {
    'Invoice #': 'INV-2025-0042',
    'Customer': 'ABC Company',
  },
  'items': [
    {'Item': 'Web Hosting', 'Qty': '1', 'Unit': '5000', 'Total': '5000'},
  ],
  'totals': {
    'Subtotal': '7,000',
    'VAT (16%)': '1,120',
    'Total': 'KSH 8,120',
  },
  'layoutStyle': 'simple',
}, bothCopies: false);
```

#### Minimal Receipt
```dart
await plugin.printDynamic({
  'header': 'PAYMENT RECEIVED',
  'fields': {
    'Amount': 'KSH 1,500',
    'Reference': 'PAY-7891',
  },
  'footer': 'Thank you',
}, bothCopies: false);
```

For more examples and detailed documentation, see:
- **[DYNAMIC_PRINTING_GUIDE.md](DYNAMIC_PRINTING_GUIDE.md)** - Complete guide with examples
- **[IMAGE_PRINTING_GUIDE.md](IMAGE_PRINTING_GUIDE.md)** - Printing images & logos (`printImage`, receipt logos)
- **[example/lib/tmp_rovodev_dynamic_print_demo.dart](example/lib/tmp_rovodev_dynamic_print_demo.dart)** - Interactive demo app

---

## 📦 Available Methods

| Method | Description |
|--------|-------------|
| `initializeDevice()` | Initializes the printer device |
| `openDevice()` | Opens a connection to the device |
| `printDynamic(args, {bothCopies})` | **Universal method** - prints any document type |
| `getDeviceStatus()` | Returns device status |
| `getSerialNumber()` | **NEW** - Retrieves the unique device serial number |
| `closeDevice()` | Closes the connection to the device |
| `scanQRCode()` | Scans a QR code |
| `stopQRScan()` | Stops QR scanning |

---

## 🎯 Key Advantages

✅ **Single Universal Method** - One `printDynamic()` method handles all document types  
✅ **Maximum Flexibility** - Print receipts, invoices, reports, dispatches, or any custom document  
✅ **Automatic Field Skipping** - Null/empty fields are automatically omitted  
✅ **Smart Formatting** - Proper alignment, truncation, and text wrapping  
✅ **Professional Output** - Clean, consistent formatting for all documents  
✅ **Easy to Use** - Simple map-based API  
✅ **Well Documented** - Comprehensive guide and examples provided  

---

## 📄 License

This plugin is released under the MIT License. See [LICENSE](LICENSE) for details.

---

## 🔍 Disclaimer

This Flutter plugin is an independent wrapper created to simplify integration of the ZCS SDK into Flutter apps.

- It does not include or redistribute the ZCS SDK
- It does not modify or reverse-engineer the SDK
- It is not affiliated with or endorsed by the creators of the ZCS SDK

Please consult the official ZCS documentation and license terms at https://www.szzcs.com before using the SDK in your application.
