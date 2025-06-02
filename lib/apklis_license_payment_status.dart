/// La clase ApklisLicensePaymentStatus registra el estado de un pago de licencia.
///
/// Registra si está pagada o no (después de la operación).
/// Registra el nombre de usuario que realiza la acción de compra/chequeo.
/// Registra (si se solicita verificar la licencia activa) la licencia pagada
/// Registra (si hay error) el error y el statusCode asociado al mismo proveniente de la API
class ApklisLicensePaymentStatus {
  /// Para crear un instancia de la clase [ApklisLicensePaymentStatus]
  const ApklisLicensePaymentStatus({
    required this.paid,
    required this.username,
    this.license,
    this.error,
    this.statusCode,
  });

  /// El [paid]  almacena el estado del Payment en true | false
  final bool paid;

  /// El [username] almacena el nombre de usuario.
  final String? username;

  /// El [license]  almacena la licencia actual del usuario (si tiene)
  final String? license;

  /// El [error]  almacena el error de la petición (si tiene)
  final String? error;

  /// El [statusCode]  almacena el statusCode de error de la petición (si tiene)
  final int? statusCode;
}
