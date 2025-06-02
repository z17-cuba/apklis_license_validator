
package cu.uci.android.apklis_license_validator

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        when (call.method) {
            "purchaseLicense" -> {
                try {
                    val licenseUuid = call.arguments<String>() ?: ""

                    // Launch a coroutine to handle the suspend function
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response: Map<String, Any>? =
                                PurchaseAndVerify.purchaseLicense(
                                    context,  licenseUuid,
                                )

                            Log.d(TAG, "response: $response")

                            if(response != null){
                                // An error occurred during payment
                                if(response["error"] != null){
                                    withContext(Dispatchers.Main) {
                                        // Register a "success" callback to be returned to the main app
                                        result.success(response)
                                    }
                                    return@launch
                                }

                                //   TODO parsear esto con el mensaje del Websocket
                                // Switch to main thread for UI updates
                                withContext(Dispatchers.Main) {
                                    result.success({})
                                }
                            }

                        } catch (e: Exception) {
                            // Log the exception and return an error to Flutter
                            Log.e(TAG, "Error purchasing license", e)
                            withContext(Dispatchers.Main) {
                                result.error(
                                    "PURCHASE_ERROR",
                                    "Failed to purchase license: ${e.message}",
                                    null
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Log the exception and return an error to Flutter
                    Log.e(TAG, "Error purchasing license", e)
                    result.error("PURCHASE_ERROR", "Fallo al pagar licencia: ${e.message}", null)
                }
            }

            "verifyCurrentLicense" -> {
                try {
                    val packageId = call.arguments<String>() ?: ""

                    // Launch a coroutine to handle the suspend function
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response: Map<String, Any>? =
                                PurchaseAndVerify.verifyCurrentLicense(context, packageId)

                            Log.d(TAG, "response: $response")

                            if(response != null){
                                withContext(Dispatchers.Main) {
                                    // Register a "success" callback to be returned to the main app
                                    result.success(response)
                                }
                            }

                        } catch (e: Exception) {
                            // Log the exception and return an error to Flutter
                            Log.e(TAG, "Error verifying license", e)
                            withContext(Dispatchers.Main) {
                                result.error(
                                    "VERIFY_ERROR",
                                    "Fallo al verificar licencia: ${e.message}",
                                    null
                                )
                            }
                        }
                    }

                } catch (e: Exception) {
                    // Log the exception and return an error to Flutter
                    Log.e(TAG, "Error verifying license", e)
                    result.error(
                        "VERIFY_ERROR",
                        "Fallo al verificar licencia: ${e.message}",
                        null
                    )
                }
            }

            else -> {
                result.notImplemented()
            }
        }
    }
    override fun onDetachedFromEngine( binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
