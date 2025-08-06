package cu.uci.android.apklis_license_validator

import android.content.Context
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class ApklisLicenseValidatorPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var context: Context

    companion object {
        private const val TAG = "ApklisLicenseValidator"
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "apklis_license_validator")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val validator = ApklisLicenseValidator()
        when (call.method) {
            "purchaseLicense" -> {
                try {
                    val licenseUuid = call.arguments<String>() ?: ""
                    val validator = ApklisLicenseValidator()

                    validator.purchaseLicense(context, licenseUuid, object :
                        ApklisLicenseValidator.LicenseCallback {
                        override fun onSuccess(response: Map<String, Any>) {
                            // License purchased successfully
                            Log.d(TAG, "Purchase successful: $response")

                            // Return success to Flutter
                            result.success(response)
                        }

                        override fun onError(error: ApklisLicenseValidator.LicenseError) {
                            // Handle purchase error
                            Log.e(TAG, "Purchase failed: ${error.message}")

                            // Return error to Flutter
                            result.error(
                                "PURCHASE_ERROR",
                                "Failed to purchase license: ${error.message}",
                                mapOf(
                                    "code" to error.code,
                                    "message" to error.message
                                )
                            )
                        }
                    })

                } catch (e: Exception) {
                    // Log the exception and return an error to Flutter
                    Log.e(TAG, "Error purchasing license", e)
                    result.error(
                        "PURCHASE_ERROR",
                        "Failed to purchase license: ${e.message}",
                        null
                    )
                }
            }

            "verifyCurrentLicense" -> {
                try {
                    val packageId = call.arguments<String>() ?: ""

                    validator.verifyCurrentLicense(context, packageId, object :
                        ApklisLicenseValidator.LicenseCallback {
                        override fun onSuccess(response: Map<String, Any>) {
                            // License verification successful
                            Log.d(TAG, "Verification successful: $response")

                            // Return success to Flutter
                            result.success(response)
                        }

                        override fun onError(error: ApklisLicenseValidator.LicenseError) {
                            // Handle verification error
                            Log.e(TAG, "Verification failed: ${error.message}")

                            // Return error to Flutter
                            result.error(
                                "VERIFY_ERROR",
                                "Failed to verify license: ${error.message}",
                                mapOf(
                                    "code" to error.code,
                                    "message" to error.message
                                )
                            )
                        }
                    })

                } catch (e: Exception) {
                    // Log the exception and return an error to Flutter
                    Log.e(TAG, "Error verifying license", e)
                    result.error(
                        "VERIFY_ERROR",
                        "Failed to verify license: ${e.message}",
                        null
                    )
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}