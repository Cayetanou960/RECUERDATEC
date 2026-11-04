package es.uam.esalud.recuerdatec.util

import androidx.compose.runtime.Composable
import kotlinx.browser.window

actual fun llamarTelefono(numero: String): Boolean = try {
    window.location.href = "tel:" + numero.filter { it.isDigit() || it == '+' }
    true
} catch (e: Throwable) {
    false
}

/** En web no se reescala: solo se aceptan fotos pequenas (<150 KB). */
actual fun reducirFoto(bytes: ByteArray): ByteArray? = if (bytes.size > 150_000) null else bytes

@Composable
actual fun ManejarAtras(activo: Boolean, alAtras: () -> Unit) {
    // El navegador gestiona su propio boton "atras".
}
