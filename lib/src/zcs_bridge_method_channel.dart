import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'zcs_bridge_platform_interface.dart';

class MethodChannelZcsBridge extends ZcsBridgePlatform {
  @visibleForTesting
  final channel = const MethodChannel('zcs_bridge');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await channel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<bool> initializeDevice() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('initializeDevice'),
      );
      return result['success'] == true;
    } on PlatformException catch (e) {
      throw SmartPosException('Device initialization failed: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> openDevice() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('openDevice'),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Device opening failed: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> closeDevice() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('closeDevice'),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Device closing failed: ${e.message}');
    }
  }

  /// Universal dynamic printing method
  ///
  /// Prints any type of document (receipt, report, invoice, dispatch, etc.)
  /// based on the provided args map. Fields not provided are automatically skipped.
  ///
  /// [args] - Map containing document data:
  ///   - businessName: Business name (centered, bold, large)
  ///   - header: Document header/title (centered, bold)
  ///   - subHeader: Document subtitle (centered)
  ///   - fields: Map of key-value pairs to print
  ///   - items: List of items to print as table
  ///   - totals: Map of totals (Subtotal, Tax, Total, etc.)
  ///   - footer: Footer text (centered, small)
  ///   - qrCodeField: Field name whose value should be rendered as QR code
  ///   - layoutStyle: 'simple', 'detailed', or 'compact' (default: 'detailed')
  ///
  /// [bothCopies] - If true, prints both customer and merchant copies with a pause between them
  /// [pauseBetweenCopies] - Number of seconds to pause between copies (default: 5 seconds)
  ///
  /// Example:
  /// ```dart
  /// await printDynamic({
  ///   'businessName': 'mesh Logistics',
  ///   'header': 'DISPATCH RECEIPT',
  ///   'subHeader': 'Warehouse #3 - Nairobi',
  ///   'fields': {
  ///     'Customer Name': 'John Mwangi',
  ///     'Loader': 'Peter Otieno',
  ///     'Weight': '320 KG',
  ///   },
  ///   'items': [
  ///     {'Item': '20L Water', 'Qty': '2', 'Unit': '50', 'Total': '100'},
  ///   ],
  ///   'totals': {'Total': 'KSH 130'},
  ///   'footer': 'Thank you for choosing mesh',
  ///   'qrCodeField': 'dispatchId',
  ///   'dispatchId': 'DSP-2025-001',
  /// }, bothCopies: true, pauseBetweenCopies: 5);
  /// ```
  @override
  Future<Map<String, dynamic>> printDynamic(
    Map<String, dynamic> args, {
    bool bothCopies = false,
    int pauseBetweenCopies = 5,
  }) async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('printDynamic', {
          'args': args,
          'bothCopies': bothCopies,
          'pauseBetweenCopies': pauseBetweenCopies,
        }),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to print document: ${e.message}');
    }
  }

  @override
  Future<String?> connectToDevice(String deviceId) async {
    try {
      final result = await channel.invokeMethod<String>('connectToDevice', {
        'deviceId': deviceId,
      });
      return result;
    } catch (e) {
      debugPrint('Error connecting to device: $e');
      return null;
    }
  }

  @override
  Future<List<String>> scanForDevices() async {
    try {
      final result = await channel.invokeMethod<List>('scanForDevices');
      return result?.cast<String>() ?? [];
    } catch (e) {
      debugPrint('Error scanning for devices: $e');
      return [];
    }
  }

  @override
  Future<bool> sendCommand(String command) async {
    try {
      final result = await channel.invokeMethod<bool>('sendCommand', {
        'command': command,
      });
      return result ?? false;
    } catch (e) {
      debugPrint('Error sending command: $e');
      return false;
    }
  }

  @override
  Future<String?> getDeviceInfo() async {
    try {
      final result = await channel.invokeMethod<String>('getDeviceInfo');
      return result;
    } catch (e) {
      debugPrint('Error getting device info: $e');
      return null;
    }
  }

  @override
  Future<bool> disconnect() async {
    try {
      final result = await channel.invokeMethod<bool>('disconnect');
      return result ?? false;
    } catch (e) {
      debugPrint('Error disconnecting: $e');
      return false;
    }
  }

  @override
  Future<Map<String, dynamic>?> getDeviceStatus() async {
    try {
      final result = await channel.invokeMethod<Map>('getDeviceStatus');
      return result?.cast<String, dynamic>();
    } catch (e) {
      debugPrint('Error getting device status: $e');
      return null;
    }
  }

  @override
  Future<String?> getSerialNumber() async {
    try {
      final result = await channel.invokeMethod<String>('getSerialNumber');
      return result;
    } catch (e) {
      debugPrint('Error getting serial number: $e');
      return null;
    }
  }

  @override
  Future<Map<String, dynamic>> printRawText(String text) async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('printRawText', {
          'text': text,
        }),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to print raw text: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> printImage(
    Uint8List imageBytes, {
    String align = 'center',
    int? width,
  }) async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('printImage', {
          'imageData': imageBytes,
          'align': align,
          'width': width,
        }),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to print image: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> openCashBox() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('openCashBox'),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to open cash drawer: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> scanQRCode() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('scanQRCode'),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to scan QR code: ${e.message}');
    }
  }

  @override
  Future<Map<String, dynamic>> stopQRScan() async {
    try {
      final Map<String, dynamic> result = Map<String, dynamic>.from(
        await channel.invokeMethod('stopQRScan'),
      );
      return result;
    } on PlatformException catch (e) {
      throw SmartPosException('Failed to stop QR scan: ${e.message}');
    }
  }
}

class SmartPosException implements Exception {
  final String message;

  SmartPosException(this.message);

  @override
  String toString() {
    return 'SmartPosException: $message';
  }
}
