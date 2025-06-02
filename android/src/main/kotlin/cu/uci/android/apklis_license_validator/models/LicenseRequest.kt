package cu.uci.android.apklis_license_validator.models

import com.google.gson.annotations.SerializedName

data class LicenseRequest(
    @SerializedName("package_name") val packageName: String,
    val device: String
)