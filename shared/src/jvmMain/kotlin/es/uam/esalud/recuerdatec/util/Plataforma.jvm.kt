package es.uam.esalud.recuerdatec.util

import androidx.compose.runtime.Composable
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** En escritorio no hay marcador de telefono: la pantalla muestra el numero. */
actual fun llamarTelefono(numero: String): Boolean = false

actual fun reducirFoto(bytes: ByteArray): ByteArray? {
    val origen = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
    val maximo = 360.0
    val escala = minOf(1.0, maximo / maxOf(origen.width, origen.height))
    val ancho = (origen.width * escala).toInt().coerceAtLeast(1)
    val alto = (origen.height * escala).toInt().coerceAtLeast(1)
    val destino = BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB)
    val g = destino.createGraphics()
    g.drawImage(origen, 0, 0, ancho, alto, null)
    g.dispose()
    val salida = ByteArrayOutputStream()
    if (!ImageIO.write(destino, "jpg", salida)) return null
    return salida.toByteArray()
}

@Composable
actual fun ManejarAtras(activo: Boolean, alAtras: () -> Unit) {
    // Sin boton "atras" del sistema en escritorio.
}
