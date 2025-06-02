package cu.uci.android.apklis_license_validator

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import cu.uci.android.apklis_license_validator.models.Qr
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

// QR Dialog Activity
@Suppress("DEPRECATION")
class QrDialogActivity : Activity() {
    companion object {
        private const val TAG = "QrDialogActivity"
    }

    private lateinit var qr: Qr
    private lateinit var qrData: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get QR from intent
        qr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             intent.getParcelableExtra("qr", Qr::class.java)
         } else {
             intent.getParcelableExtra("qr")
         } ?: run {
             Log.e(TAG, "No QR received")
             finish()
             return
         }

        // Get QR data from intent
       if (intent.getStringExtra("qr_data") != null) {
           qrData = intent.getStringExtra("qr_data")!!
        } else {
           Log.e(TAG, "No QR Data received")
           finish()
           return
       }


        // Set dialog theme
        setTheme(android.R.style.Theme_DeviceDefault_Light_Dialog)

        // Create and show dialog
        // Inflate custom dialog layout
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null)

        // Find views
        val amountTextView = dialogView.findViewById<TextView>(R.id.amount_text)
        "Monto: $${qr.importe} CUP".also { amountTextView.text = it }
        val qrImageView = dialogView.findViewById<ImageView>(R.id.qr_image_view)
        val openAppButton = dialogView.findViewById<Button>(R.id.open_app_button)
        val closeButton = dialogView.findViewById<Button>(R.id.close_button)

        // Generate QR code content
//        val qrContent = generateQrContent(qr)

        // Generate and display QR code
        val qrBitmap = QrCodeGenerator.generateQrCode(qrData)
        qrImageView.setImageBitmap(qrBitmap)

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Set button listeners
        openAppButton.setOnClickListener {
            openTransfermovil( qrData)
            dialog.dismiss()
            finish()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        // Finish activity when dialog is dismissed
        dialog.setOnDismissListener {
            finish()
        }

        dialog.show()
    }

    private fun openTransfermovil(qrData: String) {
//        val transfermovilPackageName = "cu.etecsa.cubacel.tr.tm"
        //TODO borrar este packageName
        val transfermovilPackageName = "cu.etecsa.cubacel.tr.tmtest"

        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, qrData)
                type = "text/plain"
                setPackage(transfermovilPackageName)
            }

            startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Transfermóvil app not installed", e)
            Toast.makeText(this, "No se encuentra instalada la aplicación Transfermóvil", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Transfermóvil app", e)
            Toast.makeText(this, "No se pudo abrir la aplicación Transfermóvil", Toast.LENGTH_SHORT).show()
        }
    }

}

class QrDialogManager(private val context: Context) {
    companion object {
        private const val TAG = "QrDialogManager"
    }

    fun showQrDialog(qr: Qr, data: String, callback: (Boolean) -> Unit) {
        Handler(Looper.getMainLooper()).post {
            try {
                val intent = Intent(context, QrDialogActivity::class.java).apply {
                    putExtra("qr", qr)
                    putExtra("qr_data", data)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                callback(true)
            } catch (e: Exception) {
                Log.e(TAG, "Error launching QR dialog activity", e)
                callback(false)
            }
        }
    }
}


object QrCodeGenerator {
     private const val TAG = "QrCodeGenerator"

    fun generateQrCode(content: String, size: Int = 512): Bitmap {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error generating QR code", e)
            createErrorBitmap(size)
        }
    }

    private fun createErrorBitmap(size: Int): Bitmap {
        val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)
        bitmap.eraseColor(Color.GRAY)
        return bitmap
    }
}