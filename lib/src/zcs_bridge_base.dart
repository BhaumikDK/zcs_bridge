import 'dart:typed_data';
import 'zcs_bridge_platform_interface.dart';

/// ZCS SDK Plugin - Main entry point
///
/// Provides access to ZCS POS hardware features including:
/// - Dynamic printing engine for any document type
/// - Device management
/// - QR code scanning
class ZcsBridge {
  Future<String?> getPlatformVersion() {
    return ZcsBridgePlatform.instance.getPlatformVersion();
  }

  Future<bool> initializeDevice() {
    return ZcsBridgePlatform.instance.initializeDevice();
  }

  Future<Map<String, dynamic>> openDevice() {
    return ZcsBridgePlatform.instance.openDevice();
  }

  Future<Map<String, dynamic>> closeDevice() {
    return ZcsBridgePlatform.instance.closeDevice();
  }

  /// Universal dynamic printing method
  ///
  /// Prints any type of document (receipt, report, invoice, warehouse dispatch, etc.)
  /// based on the provided args map. Fields not provided are automatically skipped.
  ///
  /// When [bothCopies] is true, the printer will:
  /// 1. Print the first copy (CUSTOMER COPY)
  /// 2. Pause for [pauseBetweenCopies] seconds (default: 5 seconds)
  /// 3. Print the second copy (MERCHANT COPY)
  ///
  /// This pause allows the user to cut the first copy before the second one prints.
  ///
  /// To print a logo at the top of the receipt, include these keys in [args]:
  ///   - 'logoBytes': a [Uint8List] of encoded image data (PNG/JPG/etc.)
  ///   - 'logoWidth': (optional) target width in printer dots
  ///   - 'logoAlign': (optional) 'left', 'center' (default) or 'right'
  ///
  /// Example:
  /// ```dart
  /// final plugin = ZcsBridge();
  /// await plugin.printDynamic({
  ///   'businessName': 'mesh Logistics',
  ///   'header': 'DISPATCH RECEIPT',
  ///   'subHeader': 'Warehouse #3 - Nairobi',
  ///   'fields': {
  ///     'Customer Name': 'John Mwangi',
  ///     'Loader': 'Peter Otieno',
  ///     'Collected By': 'Samuel Kariuki',
  ///     'Collected ID': 'EMP203',
  ///     'Weight': '320 KG',
  ///     'Pieces': '15',
  ///   },
  ///   'items': [
  ///     {'Item': '20L Water', 'Qty': '2', 'Unit': '50', 'Total': '100'},
  ///     {'Item': 'Delivery Fee', 'Qty': '1', 'Unit': '30', 'Total': '30'},
  ///   ],
  ///   'totals': {
  ///     'Subtotal': '130',
  ///     'Tax': '0',
  ///     'Total': 'KSH 130',
  ///   },
  ///   'footer': 'Thank you for choosing mesh',
  ///   'qrCodeField': 'dispatchId',
  ///   'dispatchId': 'DSP-2025-001',
  ///   'layoutStyle': 'detailed',
  /// }, bothCopies: true, pauseBetweenCopies: 5);
  /// ```
  Future<Map<String, dynamic>> printDynamic(
    Map<String, dynamic> args, {
    bool bothCopies = false,
    int pauseBetweenCopies = 5,
  }) {
    return ZcsBridgePlatform.instance.printDynamic(
      args,
      bothCopies: bothCopies,
      pauseBetweenCopies: pauseBetweenCopies,
    );
  }

  Future<String?> connectToDevice(String deviceId) {
    return ZcsBridgePlatform.instance.connectToDevice(deviceId);
  }

  Future<List<String>> scanForDevices() {
    return ZcsBridgePlatform.instance.scanForDevices();
  }

  Future<bool> sendCommand(String command) {
    return ZcsBridgePlatform.instance.sendCommand(command);
  }

  Future<String?> getDeviceInfo() {
    return ZcsBridgePlatform.instance.getDeviceInfo();
  }

  Future<bool> disconnect() {
    return ZcsBridgePlatform.instance.disconnect();
  }

  Future<Map<String, dynamic>?> getDeviceStatus() {
    return ZcsBridgePlatform.instance.getDeviceStatus();
  }

  /// Get device serial number
  ///
  /// Returns the unique serial number of the ZCS POS device.
  /// Returns null if the device is not initialized or an error occurs.
  ///
  /// Example:
  /// ```dart
  /// final plugin = ZcsBridge();
  /// await plugin.initializeDevice();
  /// final serialNumber = await plugin.getSerialNumber();
  /// print('Device S/N: $serialNumber');
  /// ```
  Future<String?> getSerialNumber() {
    return ZcsBridgePlatform.instance.getSerialNumber();
  }

  /// Print raw text string
  ///
  /// Prints a raw text string directly to the printer.
  /// This is a simple method for printing plain text without any formatting structure.
  ///
  /// Example:
  /// ```dart
  /// final plugin = ZcsBridge();
  /// await plugin.initializeDevice();
  /// await plugin.openDevice();
  /// await plugin.printRawText('Hello, World!\nThis is a test print.\n');
  /// ```
  Future<Map<String, dynamic>> printRawText(String text) {
    return ZcsBridgePlatform.instance.printRawText(text);
  }

  /// Print an image or logo from raw encoded image bytes.
  ///
  /// Accepts standard encoded image bytes (PNG, JPG, BMP, etc.) as a
  /// [Uint8List]. The image is automatically scaled to fit the paper width
  /// while preserving its aspect ratio.
  ///
  /// [imageBytes] - The encoded image data.
  /// [align] - Horizontal alignment: 'left', 'center' (default) or 'right'.
  /// [width] - Optional target width in printer dots. If omitted, the image
  ///   is fit to the paper width (~384 dots for 58mm, ~576 dots for 80mm).
  ///   Values larger than the paper width are clamped.
  ///
  /// Requires the device to be initialized and opened first
  /// (call [initializeDevice] and [openDevice]).
  ///
  /// Example (printing an asset logo):
  /// ```dart
  /// final data = await rootBundle.load('assets/logo.png');
  /// final bytes = data.buffer.asUint8List();
  /// await plugin.printImage(bytes, align: 'center', width: 300);
  /// ```
  Future<Map<String, dynamic>> printImage(
    Uint8List imageBytes, {
    String align = 'center',
    int? width,
  }) {
    return ZcsBridgePlatform.instance.printImage(
      imageBytes,
      align: align,
      width: width,
    );
  }

  /// Opens the cash drawer attached to the printer.
  ///
  /// Requires the device to already be initialized and opened
  /// (call [initializeDevice] and [openDevice] first).
  ///
  /// Example:
  /// ```dart
  /// final plugin = ZcsBridge();
  /// await plugin.initializeDevice();
  /// await plugin.openDevice();
  /// await plugin.openCashBox();
  /// ```
  Future<Map<String, dynamic>> openCashBox() {
    return ZcsBridgePlatform.instance.openCashBox();
  }

  /// Activates the QR scanner and waits for a single scan.
  ///
  /// Returns a map with `success`, `message`, and `data` (the scanned
  /// content, empty on failure). The scan times out after 10 seconds if
  /// no QR code is detected. Call [stopQRScan] to cancel an active scan.
  ///
  /// Requires the device to be initialized first (call [initializeDevice]).
  ///
  /// Example:
  /// ```dart
  /// final plugin = ZcsBridge();
  /// await plugin.initializeDevice();
  /// final result = await plugin.scanQRCode();
  /// if (result['success'] == true) {
  ///   print('Scanned: ${result['data']}');
  /// }
  /// ```
  Future<Map<String, dynamic>> scanQRCode() {
    return ZcsBridgePlatform.instance.scanQRCode();
  }

  /// Powers off the QR scanner, cancelling any pending [scanQRCode] call.
  Future<Map<String, dynamic>> stopQRScan() {
    return ZcsBridgePlatform.instance.stopQRScan();
  }
}
