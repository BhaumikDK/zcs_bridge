import 'dart:typed_data';
import 'package:zcs_bridge/zcs_bridge.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockZcsBridge
    with MockPlatformInterfaceMixin
    implements ZcsBridgePlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
  
  @override
  Future<Map<String, dynamic>> closeDevice() {
    return Future.value({'success': true});
  }
  
  @override
  Future<String?> connectToDevice(String deviceId) {
    return Future.value('Connected to $deviceId');
  }
  
  @override
  Future<bool> disconnect() {
    return Future.value(true);
  }
  
  @override
  Future<String?> getDeviceInfo() {
    return Future.value('Mock Device Info');
  }
  
  @override
  Future<Map<String, dynamic>?> getDeviceStatus() {
    return Future.value({'status': 'ready'});
  }
  
  @override
  Future<bool> initializeDevice() {
    return Future.value(true);
  }
  
  @override
  Future<Map<String, dynamic>> openDevice() {
    return Future.value({'success': true});
  }
  
  @override
  Future<Map<String, dynamic>> printDynamic(
    Map<String, dynamic> args, {
    bool bothCopies = false,
    int pauseBetweenCopies = 5,
  }) {
    return Future.value({
      'success': true, 
      'bothCopies': bothCopies,
      'pauseBetweenCopies': pauseBetweenCopies,
    });
  }
  
  @override
  Future<List<String>> scanForDevices() {
    return Future.value(['device1', 'device2']);
  }
  
  @override
  Future<bool> sendCommand(String command) {
    return Future.value(true);
  }
  
  @override
  Future<String?> getSerialNumber() {
    return Future.value('MOCK_SERIAL_123456');
  }
  
  @override
  Future<Map<String, dynamic>> printRawText(String text) {
    return Future.value({'success': true, 'message': 'Raw text printed'});
  }

    @override
  Future<Map<String, dynamic>> openCashBox() {
    return Future.value({'success': true, 'message': 'Cash box opened'});
  }

  @override
  Future<Map<String, dynamic>> printImage(
    Uint8List imageBytes, {
    String align = 'center',
    int? width,
  }) {
    return Future.value({'success': true, 'message': 'Image printed'});
  }

  @override
  Future<Map<String, dynamic>> scanQRCode() {
   return Future.value({'success': true, 'message': 'QR code scanned'});
  }

  @override
  Future<Map<String, dynamic>> stopQRScan() {
    return Future.value({'success': true, 'message': 'QR scan stopped'});
  }
}

void main() {
  final ZcsBridgePlatform initialPlatform = ZcsBridgePlatform.instance;

  test('$MethodChannelZcsBridge is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelZcsBridge>());
  });


  test('getPlatformVersion', () async {
    ZcsBridge zcsBridge = ZcsBridge();
    MockZcsBridge fakePlatform = MockZcsBridge();
    ZcsBridgePlatform.instance = fakePlatform;

    expect(await zcsBridge.getPlatformVersion(), '42');
  });
}
