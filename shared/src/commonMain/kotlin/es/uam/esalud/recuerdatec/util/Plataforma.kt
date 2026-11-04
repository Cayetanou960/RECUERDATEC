package es.uam.esalud.recuerdatec.util

import androidx.compose.runtime.Composable

/** Abre el marcador del telefono con el numero. Devuelve false si la plataforma no puede. */
expect fun llamarTelefono(numero: String): Boolean

/** Reduce una foto a ~360 px y la comprime a JPEG. Devuelve null si no se puede usar. */
expect fun reducirFoto(bytes: ByteArray): ByteArray?

/** Intercepta el boton "atras" del sistema (solo Android). */
@Composable
expect fun ManejarAtras(activo: Boolean, alAtras: () -> Unit)
