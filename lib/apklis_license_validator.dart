import 'dart:async';

import 'package:apklis_license_validator/apklis_license_payment_status.dart';
import 'package:flutter/services.dart';

/// La clase ApklisLicenseValidator se encarga de la compra y validación de compras de licencias de Apklis.
///
class ApklisLicenseValidator {
  /// Establece la comunicación entre el código de Kotlin/Android y Flutter/Dart.
  static const channel = MethodChannel('apklis_license_validator');

  /// Devuelve `Future<ApklisLicensePaymentStatus>` con la información del estado de pago.
  static Future<ApklisLicensePaymentStatus> purchaseLicense([
    String? licenseId,
  ]) async {
    final Map? map = await channel.invokeMapMethod(
      'purchaseLicense',
      licenseId,
    );
    if (map == null) {
      return const ApklisLicensePaymentStatus(paid: false, username: null);
    }
    return ApklisLicensePaymentStatus(
      paid: (map['paid'] as bool?) ?? false,
      username: map['username'] as String?,
      error: map['error'] as String?,
      statusCode: map['status_code'] as int?,
    );
  }

  /// Devuelve `Future<ApklisLicensePaymentStatus>` con la información del estado de pago.
  static Future<ApklisLicensePaymentStatus> verifyUserLicense([
    String? packageId,
  ]) async {
    final Map? map = await channel.invokeMapMethod(
      'verifyCurrentLicense',
      packageId,
    );
    if (map == null) {
      return const ApklisLicensePaymentStatus(
        paid: false,
        username: null,
        license: null,
        error: null,
      );
    }
    return ApklisLicensePaymentStatus(
      paid: (map['paid'] as bool?) ?? false,
      username: map['username'] as String?,
      license: map['license'] as String?,
      error: map['error'] as String?,
      statusCode: map['status_code'] as int?,
    );
  }
}
