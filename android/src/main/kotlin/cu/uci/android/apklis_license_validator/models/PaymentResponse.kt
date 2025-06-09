package cu.uci.android.apklis_license_validator.models

data class PaymentResponse(
    val qr: String?
){
    fun toJsonString(): String {
        return """{"qr": "$qr"}"""
    }
}