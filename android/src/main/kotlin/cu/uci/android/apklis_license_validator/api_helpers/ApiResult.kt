package cu.uci.android.apklis_license_validator.api_helpers

// API Response wrapper for error handling
sealed class ApiResult<T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : ApiResult<T>()
    data class Exception<T>(val throwable: Throwable) : ApiResult<T>()
}