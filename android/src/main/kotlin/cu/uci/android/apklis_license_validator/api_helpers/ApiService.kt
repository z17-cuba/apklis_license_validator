package cu.uci.android.apklis_license_validator.api_helpers

import com.google.gson.Gson
import cu.uci.android.apklis_license_validator.models.LicenseRequest
import cu.uci.android.apklis_license_validator.models.LicenseResponse
import cu.uci.android.apklis_license_validator.models.PaymentRequest
import cu.uci.android.apklis_license_validator.models.PaymentResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.util.concurrent.TimeUnit

class ApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor(LoggingInterceptor())
        .build()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val TIMEOUT = 30_000L // 30 segundos
        private const val TAG = "PaymentApiServiceImpl" // Para el Logger
       private const val APKLIS_BASE_URL = "https://apitest.apklis.cu"
        private const val APKLIS_LICENSE_URL = "license/v1/license"
    }

    /// Make a request to the Apklis API to request the TF payment QR code for the payment of licenses
    //  Display the QR code or return an error/exception status if the request fails
    suspend fun payLicenseWithTF(request: PaymentRequest, licenseUUID: String, accessToken: String): ApiResult<PaymentResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(request)
                val requestBody = jsonBody.toRequestBody(mediaType)

                val httpRequest = Request.Builder()
                    .url("$APKLIS_BASE_URL/$APKLIS_LICENSE_URL/$licenseUUID/pay-with-transfermovil/")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(httpRequest).execute()

                when {
                    response.isSuccessful -> {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            try {
                                //TODO quitar este QR de mentiritas
                                val payResponse = gson.fromJson("{'qr': '{id_transaccion:APK00000000004050865,importe:15.0,moneda:CUP,numero_proveedor:57001,version:1}\"'}", PaymentResponse::class.java)
//                                val payResponse = gson.fromJson(responseBody, PaymentResponse::class.java)
                                ApiResult.Success(payResponse)
                            } catch (e: Exception) {
                                ApiResult.Exception(e)
                            }
                        } else {
                            ApiResult.Error("Empty response body", response.code)
                        }
                    }
                    else -> {
                        //TODO quitar este QR de mentiritas
                        val payResponse = gson.fromJson("{'qr': '{id_transaccion:APK00000000004050865,importe:15.0,moneda:CUP,numero_proveedor:57001,version:1}\"'}", PaymentResponse::class.java)
//                                val payResponse = gson.fromJson(responseBody, PaymentResponse::class.java)
                        ApiResult.Success(payResponse)
                       /*  val errorBody = response.body?.string()
                        ApiResult.Error(
                            message = errorBody ?: "HTTP ${response.code}: ${response.message}",
                            code = response.code
                        )*/
                    }
                }
            } catch (e: IOException) {
                ApiResult.Exception(e)
            } catch (e: Exception) {
                ApiResult.Exception(e)
            }
        }
    }

    /// Performs verification against the API to find out which license the user has active
    /// Returns the active license (if any)
    suspend fun verifyCurrentLicense(request: LicenseRequest, accessToken: String): ApiResult<LicenseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = gson.toJson(request)
                val requestBody = jsonBody.toRequestBody(mediaType)

                val httpRequest = Request.Builder()
                    .url("$APKLIS_BASE_URL/$APKLIS_LICENSE_URL/verify/")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(httpRequest).execute()

                when {
                    response.isSuccessful -> {
                        val responseBody = response.body?.string()
                        if (responseBody != null) {
                            try {
                                val licenseResponse = gson.fromJson(responseBody, LicenseResponse::class.java)
                                ApiResult.Success(licenseResponse)
                            } catch (e: Exception) {
                                ApiResult.Exception(e)
                            }
                        } else {
                            ApiResult.Error("Empty response body", response.code)
                        }
                    }
                    else -> {
                         val errorBody = response.body?.string()
                        ApiResult.Error(
                            message = errorBody ?: "HTTP ${response.code}: ${response.message}",
                            code = response.code
                        )
                    }
                }
            } catch (e: IOException) {
                ApiResult.Exception(e)
            } catch (e: Exception) {
                ApiResult.Exception(e)
            }
        }
    }


}
