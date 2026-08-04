import 'dart:typed_data';

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'zcs_bridge_method_channel.dart';

abstract class ZcsBridgePlatform extends PlatformInterface {
  ZcsBridgePlatform() : super(token: _token);

  static final Object _token = Object();

  static ZcsBridgePlatform _instance = MethodChannelZcsBridge();

  /// The default instance of [ZcsBridgePlatform] to use.
  ///
  /// Defaults to [MethodChannelZcsBridge].
  static ZcsBridgePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [ZcsBridgePlatform] when
  /// they register themselves.
  static set instance(ZcsBridgePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('getPlatformVersion() has not been implemented.');
  }

  Future<bool> initializeDevice() {
    throw UnimplementedError('initializeDevice() has not been implemented.');
  }

  Future<Map<String, dynamic>> openDevice() {
    throw UnimplementedError('openDevice() has not been implemented.');
  }

  Future<Map<String, dynamic>> closeDevice() {
    throw UnimplementedError('closeDevice() has not been implemented.');
  }

  /// Universal dynamic printing method
  ///
  /// Prints any type of document dynamically based on provided args map.
  /// [args] - Map containing document data (businessName, header, fields, items, totals, footer, qrCodeField, etc.)
  /// [bothCopies] - If true, prints both customer and merchant copies with a pause between them
  /// [pauseBetweenCopies] - Number of seconds to pause between copies (default: 5 seconds)
  Future<Map<String, dynamic>> printDynamic(
    Map<String, dynamic> args, {
    bool bothCopies = false,
    int pauseBetweenCopies = 5,
  }) {
    throw UnimplementedError('printDynamic() has not been implemented.');
  }

  Future<String?> connectToDevice(String deviceId) {
    throw UnimplementedError('connectToDevice() has not been implemented.');
  }

  Future<List<String>> scanForDevices() {
    throw UnimplementedError('scanForDevices() has not been implemented.');
  }

  Future<bool> sendCommand(String command) {
    throw UnimplementedError('sendCommand() has not been implemented.');
  }

  Future<String?> getDeviceInfo() {
    throw UnimplementedError('getDeviceInfo() has not been implemented.');
  }

  Future<bool> disconnect() {
    throw UnimplementedError('disconnect() has not been implemented.');
  }

  Future<Map<String, dynamic>?> getDeviceStatus() {
    throw UnimplementedError('getDeviceStatus() has not been implemented.');
  }

  Future<String?> getSerialNumber() {
    throw UnimplementedError('getSerialNumber() has not been implemented.');
  }

  Future<Map<String, dynamic>> printRawText(String text) {
    throw UnimplementedError('printRawText() has not been implemented.');
  }

  /// Print an image/logo from raw encoded image bytes (PNG, JPG, BMP, etc.).
  Future<Map<String, dynamic>> printImage(
    Uint8List imageBytes, {
    String align = 'center',
    int? width,
  }) {
    throw UnimplementedError('printImage() has not been implemented.');
  }

  /// Opens the cash drawer connected to the printer (maps to mPrinter.openBox()).
  Future<Map<String, dynamic>> openCashBox() {
    throw UnimplementedError('openCashBox() has not been implemented.');
  }

  /// Activates the QR scanner and waits for a single scan (10s timeout).
  Future<Map<String, dynamic>> scanQRCode() {
    throw UnimplementedError('scanQRCode() has not been implemented.');
  }

  /// Powers off the QR scanner, cancelling any pending [scanQRCode] call.
  Future<Map<String, dynamic>> stopQRScan() {
    throw UnimplementedError('stopQRScan() has not been implemented.');
  }
}
