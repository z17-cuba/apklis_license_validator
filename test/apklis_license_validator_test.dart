import 'package:apklis_license_validator/apklis_license_validator.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const MethodChannel channel = MethodChannel('apklis_license_validator');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    handler(MethodCall methodCall) async {
      return {
        'paid': false,
        'username': 'example',
      };
    }

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, handler);
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      channel,
      null,
    );
  });

  test('purchaseLicense with packageId', () async {
    const packageId = 'cu.uci.android.apklis';
    final status = await ApklisLicenseValidator.purchaseLicense(packageId);
    expect(status.paid, false);
    expect(status.username, 'example');
  });

  test('purchaseLicense without packageId', () async {
    final status = await ApklisLicenseValidator.purchaseLicense();
    expect(status.paid, false);
    expect(status.username, 'example');
  });
}
