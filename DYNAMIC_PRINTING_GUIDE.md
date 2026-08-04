# Dynamic Printing Engine Guide

## Overview

The ZCS SDK Plugin now features a **universal dynamic printing engine** that can print any type of document (receipts, invoices, reports, warehouse dispatches, etc.) using a single method: `printDynamic()`.

This refactored approach eliminates the need for multiple fixed print methods and provides maximum flexibility for printing any document structure.

## Key Features

✅ **Single Universal Method** - One `printDynamic()` method handles all document types  
✅ **Automatic Field Skipping** - Null or empty fields are automatically skipped  
✅ **Flexible Layout Styles** - Choose from `simple`, `detailed`, or `compact` layouts  
✅ **Nested Map Support** - Handle complex data structures recursively  
✅ **Smart Text Handling** - Auto-wrapping and truncation for long text  
✅ **Dynamic QR Codes** - Generate QR codes from any field value  
✅ **Dual Copy Printing** - Print both customer and merchant copies with one call  
✅ **UTF-8 Support** - Properly renders "KSh" and other special characters  
✅ **Paper Size Optimized** - Default 58mm paper width with proper formatting

## Basic Usage

```dart
import 'package:zcs_sdk_plugin/zcs_sdk_plugin.dart';

final plugin = ZcsSdkPlugin();

// Initialize device
await plugin.initializeDevice();
await plugin.openDevice();

// Print any document dynamically
await plugin.printDynamic({
  'businessName': 'Your Business Name',
  'header': 'DOCUMENT TITLE',
  'subHeader': 'Optional subtitle',
  'fields': {
    'Key 1': 'Value 1',
    'Key 2': 'Value 2',
  },
  'items': [
    {'Item': 'Product A', 'Qty': '2', 'Unit': '100', 'Total': '200'},
  ],
  'totals': {
    'Total': 'KSH 200',
  },
  'footer': 'Thank you!',
  'qrCodeField': 'transactionId',
  'transactionId': 'TXN-001',
  'layoutStyle': 'detailed',
}, bothCopies: false);
```

## Document Structure

### Available Fields

| Field | Type | Description | Alignment |
|-------|------|-------------|-----------|
| `businessName` | String | Business/company name | Center, Bold, Large |
| `header` | String | Document title/type | Center, Bold |
| `subHeader` | String | Subtitle or additional info | Center |
| `fields` | Map<String, dynamic> | Key-value pairs to print | Left (key), Right (value) |
| `items` | List<Map> | Items table with columns | Table format |
| `totals` | Map<String, dynamic> | Financial totals | Left (label), Right (amount) |
| `footer` | String | Footer message | Center, Small |
| `qrCodeField` | String | Name of field to encode as QR | - |
| `layoutStyle` | String | Layout style: `simple`, `detailed`, `compact` | - |

### All Fields Are Optional

The engine automatically skips any field that is:
- Not provided
- `null`
- Empty string
- Empty map/list

## Examples

### 1. Warehouse Dispatch Receipt

```dart
await plugin.printDynamic({
  'businessName': 'mesh Logistics',
  'header': 'DISPATCH RECEIPT',
  'subHeader': 'Warehouse #3 - Nairobi',
  'fields': {
    'Customer Name': 'John Mwangi',
    'Loader': 'Peter Otieno',
    'Collected By': 'Samuel Kariuki',
    'Collected ID': 'EMP203',
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
}, bothCopies: true); // Print both customer and merchant copies
```

**Output:**
```
     mesh Logistics
     DISPATCH RECEIPT
   Warehouse #3 - Nairobi

Customer Name:      John Mwangi
Loader:            Peter Otieno
Collected By:   Samuel Kariuki
Collected ID:           EMP203
Weight:                 320 KG
Pieces:                     15

--------------------------------
Item           Qty Unit  Total
--------------------------------
20L Water        2   50    100
Delivery Fee     1   30     30
--------------------------------
Subtotal:                   130
Tax:                          0
Total:                 KSH 130

         [QR CODE: DSP-2025-001]

  Thank you for choosing mesh
```

### 2. Restaurant Receipt

```dart
await plugin.printDynamic({
  'businessName': 'Mama Oliech Restaurant',
  'header': 'ORDER RECEIPT',
  'subHeader': 'Table 5 - Waiter: Mary',
  'fields': {
    'Order #': 'ORD-1234',
    'Date': '2025-01-15',
    'Time': '14:30',
  },
  'items': [
    {'Item': 'Fish Fillet', 'Qty': '2', 'Unit': '450', 'Total': '900'},
    {'Item': 'Ugali', 'Qty': '2', 'Unit': '80', 'Total': '160'},
    {'Item': 'Sukuma Wiki', 'Qty': '1', 'Unit': '100', 'Total': '100'},
    {'Item': 'Soda', 'Qty': '2', 'Unit': '60', 'Total': '120'},
  ],
  'totals': {
    'Subtotal': '1,180',
    'Service Charge': '118',
    'Total': 'KSH 1,298',
  },
  'footer': 'Enjoy your meal!',
  'qrCodeField': 'orderId',
  'orderId': 'ORD-1234',
}, bothCopies: false);
```

### 3. Simple Invoice

```dart
await plugin.printDynamic({
  'businessName': 'Tech Solutions Ltd',
  'header': 'INVOICE',
  'subHeader': 'PIN: P051234567X',
  'fields': {
    'Invoice #': 'INV-2025-0042',
    'Date': '15/01/2025',
    'Customer': 'ABC Company',
    'Payment Terms': 'Net 30',
  },
  'items': [
    {'Item': 'Web Hosting', 'Qty': '1', 'Unit': '5000', 'Total': '5000'},
    {'Item': 'Domain Name', 'Qty': '1', 'Unit': '1200', 'Total': '1200'},
  ],
  'totals': {
    'Subtotal': '7,000',
    'VAT (16%)': '1,120',
    'Total': 'KSH 8,120',
  },
  'footer': 'Payment due within 30 days',
  'qrCodeField': 'invoiceId',
  'invoiceId': 'INV-2025-0042',
  'layoutStyle': 'simple',
}, bothCopies: false);
```

### 4. Compact Report (No Items)

```dart
await plugin.printDynamic({
  'businessName': 'Daily Report',
  'header': 'SHIFT SUMMARY',
  'fields': {
    'Cashier': 'Jane Doe',
    'Shift': 'Morning',
    'Cash Sales': 'KSH 45,000',
    'M-Pesa': 'KSH 32,500',
    'Total Sales': 'KSH 96,400',
  },
  'footer': 'Report Generated Automatically',
  'layoutStyle': 'compact',
}, bothCopies: false);
```

### 5. Minimal Receipt

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

## Layout Styles

### `detailed` (Default)
- Larger fonts
- More spacing
- Best for customer-facing receipts
- Font sizes: Header 45, Normal 24, Small 20

### `simple`
- Medium fonts
- Standard spacing
- Good for general purpose
- Font sizes: Header 40, Normal 22, Small 20

### `compact`
- Smaller fonts
- Minimal spacing
- Saves paper for internal use
- Font sizes: Header 35, Normal 20, Small 18

## Items Table Format

The `items` field expects a list of maps. The engine looks for these keys (case-insensitive):

- `Item` / `item` - Item name/description
- `Qty` / `qty` / `quantity` - Quantity
- `Unit` / `unit` / `price` - Unit price
- `Total` / `total` / `amount` - Total amount

Example:
```dart
'items': [
  {'Item': 'Product Name', 'Qty': '2', 'Unit': '100', 'Total': '200'},
  {'item': 'Another Item', 'qty': '1', 'unit': '50', 'total': '50'},
]
```

## QR Code Generation

To add a QR code to your document:

1. Set the `qrCodeField` to the name of the field containing the value
2. Provide the actual value in the args map

```dart
await plugin.printDynamic({
  'header': 'RECEIPT',
  'qrCodeField': 'transactionId',  // This is the field name
  'transactionId': 'TXN-12345',     // This is the value to encode
}, bothCopies: false);
```

The QR code will be:
- Centered on the page
- Size: 200x200 pixels (optimized for 58mm paper)
- Placed after totals (if any) and before footer

## Nested Maps

The engine supports nested maps in the `fields` section:

```dart
'fields': {
  'Customer': 'John Doe',
  'Address': {
    'Street': '123 Main St',
    'City': 'Nairobi',
    'Country': 'Kenya',
  },
  'Phone': '+254 700 000000',
}
```

Nested fields will be indented and printed recursively.

## Text Handling

### Truncation
- Item names longer than 14 characters are truncated with "..."
- Key-value pairs that exceed line width are intelligently truncated

### Paper Width
- Default: 58mm (approximately 32 characters per line)
- Headers and footers are centered
- Keys are left-aligned, values are right-aligned
- Table columns are sized appropriately

## Both Copies Feature

When `bothCopies: true`, the engine prints:

1. **Customer Copy** (without label)
2. Separator line
3. **Merchant Copy** (with "MERCHANT COPY" label)

Both copies contain identical content.

```dart
await plugin.printDynamic({...}, bothCopies: true);
```

## Response Object

The method returns a map with:

```dart
{
  'success': true,
  'message': 'Document printed successfully',
  'copies': 2,  // or 1 if bothCopies is false
}
```

## Error Handling

```dart
try {
  final result = await plugin.printDynamic({...}, bothCopies: false);
  print('Success: ${result['message']}');
} catch (e) {
  print('Error: $e');
}
```

Common errors:
- `DEVICE_NOT_INITIALIZED` - Call `initializeDevice()` first
- `DEVICE_NOT_OPENED` - Call `openDevice()` first
- `INVALID_INPUT` - Args map cannot be null
- `Out of paper` - Printer has no paper

## Migration from Old Methods

### Old Way (Fixed Methods)
```dart
// Multiple fixed methods
await plugin.printText('Hello');
await plugin.printReceipt(receiptData);
await plugin.printQRCode('DATA');
```

### New Way (Dynamic Method)
```dart
// Single dynamic method
await plugin.printDynamic({
  'header': 'Hello',
  'fields': {...},
  'items': [...],
  'qrCodeField': 'id',
  'id': 'DATA',
}, bothCopies: false);
```

## Best Practices

1. **Always initialize first**
   ```dart
   await plugin.initializeDevice();
   await plugin.openDevice();
   ```

2. **Provide meaningful keys**
   ```dart
   'fields': {
     'Customer Name': 'John',  // Good
     'name': 'John',           // Less clear
   }
   ```

3. **Use consistent currency format**
   ```dart
   'Total': 'KSH 1,500'  // Consistent
   ```

4. **Include error handling**
   ```dart
   try {
     await plugin.printDynamic({...});
   } catch (e) {
     // Handle error
   }
   ```

5. **Test with minimal data first**
   ```dart
   // Start simple
   await plugin.printDynamic({
     'header': 'TEST',
   });
   ```

## Architecture

The dynamic printing engine consists of:

### Java Layer (Android)
- `printDynamic()` - Main entry point
- `printDocumentCopy()` - Renders one copy of the document
- `printFields()` - Prints key-value pairs
- `printItemsTable()` - Renders items as table
- `printTotals()` - Prints financial totals
- `printQRCodeHelper()` - Generates QR codes
- `printKeyValue()` - Formats single key-value line
- `createFormat()` - Creates text format objects
- `getStringValue()` - Safe value extraction

### Dart Layer (Flutter)
- `ZcsSdkPlugin.printDynamic()` - Public API
- `MethodChannelZcsSdkPlugin.printDynamic()` - Platform channel
- `ZcsSdkPluginPlatform.printDynamic()` - Platform interface

## Testing

A comprehensive demo is available at:
```
example/lib/tmp_rovodev_dynamic_print_demo.dart
```

Run the example to see:
- Warehouse dispatch receipts
- Restaurant receipts
- Invoices
- Compact reports
- Minimal receipts

## Support

For issues or questions:
1. Check device is initialized and opened
2. Verify paper is loaded
3. Test with minimal example first
4. Check logs for detailed error messages

## Summary

The dynamic printing engine provides:
- ✅ One method for all document types
- ✅ Maximum flexibility
- ✅ Automatic field handling
- ✅ Smart formatting
- ✅ Easy to use and maintain

Happy printing! 🖨️
