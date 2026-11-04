package es.uam.esalud.recuerdatec.util

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import es.uam.esalud.recuerdatec.AppContext
import java.io.ByteArrayOutputStream

actual fun llamarTelefono(numero: String): Boolean = try {
    val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + numero.filter { it.isDigit() || it == '+' }))
    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    AppContext.app.startActivity(i)
    true
} catch (e: Exception) {
    false
}

actual fun reducirFoto(bytes: ByteArray): ByteArray? {
    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val maximo = 360f
    val escala = minOf(1f, maximo / maxOf(bmp.width, bmp.height))
    val final = if (escala < 1f) {
        Bitmap.createScaledBitmap(
            bmp,
            (bmp.width * escala).toInt().coerceAtLeast(1),
            (bmp.height * escala).toInt().coerceAtLeast(1),
            true,
        )
    } else {
        bmp
    }
    val salida = ByteArrayOutputStream()
    final.compress(Bitmap.CompressFormat.JPEG, 70, salida)
    return salida.toByteArray()
}

@Composable
actual fun ManejarAtras(activo: Boolean, alAtras: () -> Unit) {
    BackHandler(enabled = activo, onBack = alAtras)
}
