package es.uam.esalud.recuerdatec.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

object Paleta {
    val crema = Color(0xFFF6F0E4)
    val azul = Color(0xFFB9CCDC)
    val azulOscuro = Color(0xFF2F4D66)
    val lila = Color(0xFFE4DAF0)
    val morado = Color(0xFF7B6CA8)
    val verde = Color(0xFF3E7F69)
    val texto = Color(0xFF222B33)
    val rojo = Color(0xFFB3483E)
    val naranja = Color(0xFFD99A3D)
}

/** `escala` multiplica el tamano de letra (accesibilidad: texto grande). */
@Composable
fun Tema(escala: Float, contenido: @Composable () -> Unit) {
    val base = LocalDensity.current
    val esquema = lightColorScheme(
        primary = Paleta.azulOscuro,
        onPrimary = Color.White,
        secondary = Paleta.morado,
        onSecondary = Color.White,
        background = Paleta.crema,
        onBackground = Paleta.texto,
        surface = Color.White,
        onSurface = Paleta.texto,
    )
    CompositionLocalProvider(LocalDensity provides Density(base.density, base.fontScale * escala)) {
        MaterialTheme(colorScheme = esquema, content = contenido)
    }
}
