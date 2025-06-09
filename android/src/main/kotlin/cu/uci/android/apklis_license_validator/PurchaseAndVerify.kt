package cu.uci.android.apklis_license_validator

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.gson.Gson
import cu.uci.android.apklis_license_validator.api_helpers.ApiResult
import cu.uci.android.apklis_license_validator.api_helpers.ApiService
import cu.uci.android.apklis_license_validator.models.LicenseRequest
import cu.uci.android.apklis_license_validator.models.PaymentRequest
import cu.uci.android.apklis_license_validator.models.Qr
import cu.uci.android.apklis_license_validator.models.VerifyLicenseResponse
import cu.uci.android.apklis_license_validator.signature_helpers.SignatureVerificationService
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PurchaseAndVerify {
    companion object {
        private const val TYPE = "cu.uci.android.apklis"
        private const val TAG = "PurchaseAndVerify"
        private const val SIGNATURE_HEADER_NAME = "signature"

        private val signatureVerificationService = SignatureVerificationService()


        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        suspend fun purchaseLicense(context: Context, licenseUuid: String): Map<String, Any>? {
            val deviceIdAndUsername :  Pair<String?, String?>? = getDeviceIdAndUsername(context)
            try {
                WebSocketClient().apply {
                    val accessToken = getAccountAccessToken(context)

                    val paymentResult = ApiService().payLicenseWithTF(
                        PaymentRequest(deviceIdAndUsername?.first ?: ""),
                        licenseUuid,
                        accessToken
                    )

                    return when (paymentResult) {
                        is ApiResult.Success -> {

                            // Verify signature only on successful response
                            val isSignatureValid = verifySignatureIfPresent(
                                context,
                                paymentResult.data.toJsonString(),
                                paymentResult.headers
                            )

                            if (!isSignatureValid) {
                                Log.w(TAG, "Signature verification failed for successful response")
                                return buildMap<String, Any> {
                                    put("error", "Invalid response signature")
                                    put("username", deviceIdAndUsername?.second ?: "")
                                }
                            } else {
                                val qrData = paymentResult.data.qr
                                if (qrData != null) {
                                    val qrObject = parseQrString(qrData)

                                    if (qrObject != null) {
                                        
                                        val qrDialogManager = QrDialogManager(context)
                    
                                        return suspendCoroutine { continuation ->
                                            qrDialogManager.showQrDialog(
                                                qrObject,
                                                qrData
                                            ) { success ->
                                                if (success) {
                                                    Log.d(TAG, "Dialog shown successfully")
                                                } else {
                                                    val errorMessage = "Fallo al mostrar el diálogo"
                                                    Log.e(TAG, errorMessage)
                                                    continuation.resume(mapOf("error" to errorMessage))
                                                }
                                            }
                                        }
                                    } else {
                                        val errorMessage =
                                            "Fallo al mostrar el QR - no se pudo generar el QR"
                                        Log.e(TAG, errorMessage)
                                        mapOf("error" to errorMessage)
                                    }
                                } else {
                                    val errorMessage = "Fallo al mostrar el QR - no se obtuvo el QR"
                                    Log.e(TAG, errorMessage)
                                    mapOf("error" to errorMessage)
                                }
                            }

                        }
                        is ApiResult.Error -> {

                            init(getAccountCode(context), deviceIdAndUsername?.first ?: "")
                            connectAndSubscribe()

                            val errorMessage = "Error al efectuar el pago ${paymentResult.code}: ${paymentResult.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", paymentResult.message)
                                put("username", deviceIdAndUsername?.second ?: "")
                                paymentResult.code?.let { put("status_code", it) }
                            }
                        }
                        is ApiResult.Exception -> {

                            init(getAccountCode(context), deviceIdAndUsername?.first ?: "")
                            connectAndSubscribe()

                            val errorMessage = "Excepción al efectuar el pago: ${paymentResult.throwable.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", errorMessage)
                                put("username", deviceIdAndUsername?.second ?: "")
                            }
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
            val deviceIdAndUsername :  Pair<String?, String?>? = getDeviceIdAndUsername(context)

            try {
                 val accessToken = getAccountAccessToken(context)

                    val verificationResult = ApiService().verifyCurrentLicense(
                        LicenseRequest( packageId,deviceIdAndUsername?.first ?: ""),
                        accessToken
                    )

                   return when (verificationResult) {
                        is ApiResult.Success -> {

                            val verifyLicenseResponse : VerifyLicenseResponse = verificationResult.data

                            // Verify signature only on successful response
                            val isSignatureValid = verifySignatureIfPresent(
                                context,
                                verifyLicenseResponse.toJsonString(),
                                verificationResult.headers
                            )

                            if (!isSignatureValid) {
                                Log.w(TAG, "Signature verification failed for successful response")
                                return buildMap<String, Any> {
                                    put("error", "Invalid response signature")
                                    put("username", deviceIdAndUsername?.second ?: "")
                                }
                            } else {
                                Log.d(TAG, "Hay una licencia: $verificationResult");
                                val hasPaidLicense = verificationResult.data.license.isNotEmpty()
                                buildMap<String, Any> {
                                    put("license", verificationResult.data.license)
                                    put("paid", hasPaidLicense)
                                    put("username", deviceIdAndUsername?.second ?: "")
                                }
                            }

                        }
                        is ApiResult.Error -> {
                            val errorMessage = "Fallo al efectuar la verificación ${verificationResult.code}: ${verificationResult.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", verificationResult.message)
                                put("username", deviceIdAndUsername?.second ?: "")
                                verificationResult.code?.let { put("status_code", it) }
                            }
                        }
                        is ApiResult.Exception -> {
                            val errorMessage = "Fallo al efectuar la verificación: ${verificationResult.throwable.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", errorMessage)
                                put("username", deviceIdAndUsername?.second ?: "")
                            }
                        }
                    }


            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            return null
        }

        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        private fun getDeviceIdAndUsername(context: Context): Pair<String?, String?>? {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(TYPE)
            val account = accounts.firstOrNull()

            return if (account != null) {
                val deviceId = accountManager.getUserData(account, "device_id")
                val username = account.name
                Pair(deviceId, username)
            } else {
                null
            }
        }

        @RequiresPermission(Manifest.permission.GET_ACCOUNTS)
        private fun getAccountCode(context: Context): String {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(TYPE)

            val account = accounts.firstOrNull()

            return if (account != null) {
                val code: String = accountManager.getUserData(account, "code")
                code
            } else {
                ""
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


        /**
         * Verifies the signature if present in the response headers
         */
        private  fun verifySignatureIfPresent(
            context: Context,
            responseString: String,
            headers: Map<String, String>?
        ): Boolean {
            val signatureValue = headers?.get(SIGNATURE_HEADER_NAME)

            if (signatureValue.isNullOrEmpty()) {
                Log.w(TAG, "No signature header found in response")
                return false
            }


            return signatureVerificationService.verifySignature(
                context,
                responseString.toByteArray(),
              signatureValue
            )
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