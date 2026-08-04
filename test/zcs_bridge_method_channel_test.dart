import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:zcs_bridge/zcs_bridge.dart';


void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelZcsBridge platform = MethodChannelZcsBridge();
  const MethodChannel channel = MethodChannel('zcs_bridge');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
