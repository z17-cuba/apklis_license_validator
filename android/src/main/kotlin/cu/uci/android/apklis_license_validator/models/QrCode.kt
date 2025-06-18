package cu.uci.android.apklis_license_validator.models

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class QrCode(
    @SerializedName("id_transaccion") val idTransaccion: String,
    val importe: String,
    val moneda: String,
    @SerializedName("numero_proveedor") val numeroProveedor: String,
    val version: String,
)  : Parcelable {
    fun toJsonString(): String {
        return """{"id_transaccion": "$idTransaccion", "importe": "$importe", "moneda": "$moneda", "numero_proveedor": "$numeroProveedor", "version": "$version"}"""
    }

}