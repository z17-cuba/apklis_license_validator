package cu.uci.android.apklis_license_validator

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresPermission
import cu.uci.android.apklis_license_validator.api_helpers.ApiResult
import cu.uci.android.apklis_license_validator.api_helpers.ApiService
import cu.uci.android.apklis_license_validator.models.LicenseRequest
import cu.uci.android.apklis_license_validator.models.PaymentRequest
import cu.uci.android.apklis_license_validator.models.Qr
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaseAndVerify {
    companion object {
        private const val TYPE = "cu.uci.android.apklis"
        private const val TAG = "PurchaseAndVerify"


        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        suspend fun purchaseLicense(context: Context, licenseUuid: String): Map<String, Any>? {
            val deviceId : String? = getDeviceId(context)
            try {
                WebSocketClient().apply {
                    init(deviceId ?: "")
                    connectAndSubscribe()
                    // TODO: pay with TF

                    val accessToken = getAccountAccessToken(context)

                    val paymentResult = ApiService().payLicenseWithTF(
                        PaymentRequest(deviceId ?: ""),
                        licenseUuid,
                        accessToken
                    )

                    return when (paymentResult) {
                        is ApiResult.Success -> {
                            val qrData = paymentResult.data.qr
                            val qrObject = parseQrString(qrData)
                            val qrDialogManager = QrDialogManager(context)

                            if (qrObject != null) {
                                return suspendCoroutine { continuation ->
                                    qrDialogManager.showQrDialog(qrObject, qrData) { success ->
                                        if (success) {
                                            Log.d(TAG, "Dialog shown successfully")
                                            continuation.resume(mapOf("license" to "123-asd"))
                                        } else {
                                            val errorMessage = "Fallo al mostrar el diálogo"
                                            Log.e(TAG, errorMessage)
                                            continuation.resume(mapOf("error" to errorMessage))
                                        }
                                    }
                                }
                            } else {
                                val errorMessage = "Fallo al mostrar el QR - no se pudo generar el QR"
                                Log.e(TAG, errorMessage)
                                mapOf("error" to errorMessage)
                            }
                        }
                        is ApiResult.Error -> {
                            val errorMessage = "Fallo al efectuar el pago ${paymentResult.code}: ${paymentResult.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", paymentResult.message)
                                paymentResult.code?.let { put("status_code", it) }
                            }
                        }
                        is ApiResult.Exception -> {
                            val errorMessage = "Fallo al efectuar el pago: ${paymentResult.throwable.message}"
                            Log.e(TAG, errorMessage)
                            mapOf("error" to errorMessage)
                        }
                    }

                    // TODO: Unsubscribe and close from WebSocketClient when payment message arrives
                }
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }

        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
       suspend fun verifyCurrentLicense(context: Context, packageId: String): Map<String, Any>? {
            val deviceId : String? = getDeviceId(context)

            try {
                 val accessToken = getAccountAccessToken(context)

                    val verificationResult = ApiService().verifyCurrentLicense(
                        LicenseRequest( packageId,deviceId ?: ""),
                        accessToken
                    )

                   return when (verificationResult) {
                        is ApiResult.Success -> {
                            Log.d(TAG, "Hay una licencia: $verificationResult");
                            // TODO esto esta hardcodeado
                            mapOf("license" to "123-asd")

                        }
                        is ApiResult.Error -> {
                            val errorMessage = "Fallo al efectuar la verificación ${verificationResult.code}: ${verificationResult.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", verificationResult.message)
                                verificationResult.code?.let { put("status_code", it) }
                            }
                        }
                        is ApiResult.Exception -> {
                            val errorMessage = "Fallo al efectuar la verificación: ${verificationResult.throwable.message}"
                            Log.e(TAG, errorMessage)
                            mapOf("error" to errorMessage)
                        }
                    }


            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }

        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        private fun getDeviceId(context: Context): String? {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(TYPE)
            val account = accounts.firstOrNull()

            return if (account != null) {
                val deviceId = accountManager.getUserData(account, "device_id")
                deviceId
            } else {
                null
            }
        }

        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        private fun getAccountAccessToken(context: Context): String {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(TYPE)

            val account = accounts.firstOrNull()

            return if (account != null) {
                val accessToken: String = accountManager.getUserData(account, "access_token")
                accessToken
            } else {
                ""
            }
        }
    }
    }


fun parseQrString(qrString: String): Qr? {
    return try {
        // Remove the outer braces and split by commas
        val cleanString = qrString.trim('{', '}')
        val pairs = cleanString.split(",")

        val dataMap = mutableMapOf<String, String>()

        for (pair in pairs) {
            val keyValue = pair.split(":", limit = 2)
            if (keyValue.size == 2) {
                dataMap[keyValue[0].trim()] = keyValue[1].trim()
            }
        }

        Qr(
            idTransaccion = dataMap["id_transaccion"] ?: "",
            importe = dataMap["importe"]?.toDoubleOrNull() ?: 0.0,
            moneda = dataMap["moneda"] ?: "",
            numeroProveedor = dataMap["numero_proveedor"] ?: "",
            version = dataMap["version"] ?: ""
        )
    } catch (e: Exception) {
        null
    }
}