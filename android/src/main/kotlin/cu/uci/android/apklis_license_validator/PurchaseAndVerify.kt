package cu.uci.android.apklis_license_validator

import android.Manifest
import android.accounts.AccountManager
import android.content.Context
import android.os.RemoteException
import android.util.Log
import androidx.annotation.RequiresPermission
import cu.uci.android.apklis_license_validator.api_helpers.ApiResult
import cu.uci.android.apklis_license_validator.api_helpers.ApiService
import cu.uci.android.apklis_license_validator.models.LicenseRequest
import cu.uci.android.apklis_license_validator.models.PaymentRequest
import cu.uci.android.apklis_license_validator.models.QrCode
import cu.uci.android.apklis_license_validator.models.VerifyLicenseResponse
import cu.uci.android.apklis_license_validator.signature_helpers.SignatureVerificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
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
                            val qrCode : QrCode = paymentResult.data

                            // Verify signature only on successful response
                            val isSignatureValid = verifySignatureIfPresent(
                                context,
                                qrCode.toJsonString(),
                                paymentResult.headers
                            )

                            if (!isSignatureValid) {
                                Log.w(TAG, "Signature verification failed for successful response")
                                return buildMap<String, Any> {
                                    put("error", "Invalid response signature")
                                    put("username", deviceIdAndUsername?.second ?: "")
                                }
                            } else {
                                // Handle WebSocket connection and QR dialog
                                return handleWebSocketAndQrDialog(
                                    context,
                                    qrCode,
                                    deviceIdAndUsername
                                )

                            }
                        }
                        is ApiResult.Error -> {
                            val errorMessage = "Error al efectuar el pago ${paymentResult.code}: ${paymentResult.message}"
                            Log.e(TAG, errorMessage)
                            buildMap<String, Any> {
                                put("error", paymentResult.message)
                                put("username", deviceIdAndUsername?.second ?: "")
                                paymentResult.code?.let { put("status_code", it) }
                            }
                        }
                        is ApiResult.Exception -> {
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


        private suspend fun handleWebSocketAndQrDialog(
            context: Context,
            qrCode: QrCode,
            deviceIdAndUsername: Pair<String?, String?>?
        ): Map<String, Any> = suspendCoroutine { continuation ->

            // Flag to track if continuation has been resumed
            var isResumed = false

            val webSocketClient = WebSocketClient(object : WebSocketEventListener {
                override fun onConnected() {
                    Log.d(TAG, "WebSocket connected successfully")
                    // Now show the QR dialog since WebSocket is connected
                    if (!isResumed) {
                        Log.d(TAG, "WebSocket now shows QR")
                            showQrDialogAfterConnection(context, qrCode, continuation, deviceIdAndUsername) { resumed ->
                            isResumed = resumed
                        }
                    }
                }

                override fun onDisconnected(reason: String?) {
                    Log.d(TAG, "WebSocket disconnected: $reason")
                }

                override fun onError(error: String) {
                    Log.e(TAG, "WebSocket connection error: $error")
                    // Resume with error if WebSocket fails to connect
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(buildMap<String, Any> {
                            put("error", "WebSocket connection failed: $error")
                            put("username", deviceIdAndUsername?.second ?: "")
                        })
                    }
                }
            })

            try {
                // Initialize and connect WebSocket
                webSocketClient.init(getAccountCode(context), deviceIdAndUsername?.first ?: "")
                webSocketClient.connectAndSubscribe()
                WebSocketHolder.client = webSocketClient


            } catch (e: Exception) {
                Log.e(TAG, "Error initializing WebSocket: ${e.message}")
                if (!isResumed) {
                    isResumed = true
                    continuation.resume(buildMap<String, Any> {
                        put("error", "Failed to initialize WebSocket: ${e.message}")
                        put("username", deviceIdAndUsername?.second ?: "")
                    })
                }
            }
        }

        private fun showQrDialogAfterConnection(
            context: Context,
            qrCode: QrCode,
            continuation: Continuation<Map<String, Any>>,
            deviceIdAndUsername: Pair<String?, String?>?,
            onResumed: (Boolean) -> Unit
        ) {
            val qrData = qrCode.toJsonString()
            val qrDialogManager = QrDialogManager(context)

            qrDialogManager.showQrDialog(qrCode, qrData) { success ->
                onResumed(true)
                if (success) {
                    Log.d(TAG, "Dialog shown successfully")
                    continuation.resume(buildMap<String, Any> {
                        put("success", true)
                        put("username", deviceIdAndUsername?.second ?: "")
                    })
                } else {
                    val errorMessage = "Fallo al mostrar el diálogo"
                    Log.e(TAG, errorMessage)
                    continuation.resume(buildMap<String, Any> {
                        put("error", errorMessage)
                        put("username", deviceIdAndUsername?.second ?: "")
                    })
                }
            }
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
