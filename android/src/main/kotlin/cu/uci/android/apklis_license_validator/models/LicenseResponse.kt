package cu.uci.android.apklis_license_validator.models

import com.google.gson.annotations.SerializedName

//TODO esto esta mal y tengo que revisar en Postman una licencia que este bien para el verify
data class LicenseResponse(
    val count: Int,
    val next: String?, // Nullable, since it's null in the JSON
    val previous: String?, // Nullable, since it's null in the JSON
    val results: List<License>
)

data class License(
    val uuid: String,
    @SerializedName("license_group") val licenseGroup: LicenseGroup,
    val price: Double,
    @SerializedName("expire_in") val expireIn: Boolean,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
)

data class LicenseGroup(
    val uuid: String,
    @SerializedName("package_name") val packageName: String,
    val name: String
)
