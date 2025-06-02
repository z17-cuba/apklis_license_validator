package cu.uci.android.apklis_license_validator.models


import kotlinx.parcelize.Parcelize
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

@Parcelize
data class Qr(
    @SerializedName("id_transaccion") val idTransaccion: String,
    @SerializedName("importe") val importe: Double,
    @SerializedName("moneda") val moneda: String,
    @SerializedName("numero_proveedor") val numeroProveedor: String,
    @SerializedName("version") val version: String
) : Parcelable

