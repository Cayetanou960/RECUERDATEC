package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.uam.esalud.recuerdatec.util.deBase64
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Pantalla base: barra superior con "Volver" y contenido con scroll, centrado y de ancho limitado. */
@Composable
fun Marco(
    titulo: String,
    alVolver: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Paleta.crema), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (alVolver != null) {
                    TextButton(onClick = alVolver) {
                        Text("< Volver", color = Paleta.azulOscuro, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Paleta.azulOscuro,
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = contenido,
            )
        }
    }
}

@Composable
fun BotonGrande(
    texto: String,
    alPulsar: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    color: Color = Paleta.azulOscuro,
    activo: Boolean = true,
) {
    Button(
        onClick = alPulsar,
        modifier = modifier.heightIn(min = 64.dp),
        enabled = activo,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
    ) {
        Text(texto, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun BotonTexto(texto: String, alPulsar: () -> Unit, color: Color = Paleta.azulOscuro) {
    TextButton(onClick = alPulsar) {
        Text(texto, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Campo(
    valor: String,
    alCambiar: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    lineas: Int = 1,
    secreto: Boolean = false,
    teclado: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCambiar,
        label = { Text(etiqueta) },
        modifier = modifier,
        singleLine = lineas == 1,
        minLines = lineas,
        visualTransformation = if (secreto) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secreto) KeyboardType.Password else teclado),
    )
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    alPulsar: (() -> Unit)? = null,
    fondo: Color = Color.White,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier.fillMaxWidth().let { if (alPulsar != null) it.clip(RoundedCornerShape(18.dp)).clickable { alPulsar() } else it }
    Card(
        modifier = m,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = fondo),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = contenido)
    }
}

@Composable
fun Titulo(texto: String) {
    Text(texto, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Paleta.azulOscuro)
}

@Composable
fun Parrafo(texto: String, color: Color = Paleta.texto) {
    Text(texto, fontSize = 18.sp, color = color)
}

/** Rejilla de botones grandes a dos columnas. */
@Composable
fun RejillaSecciones(secciones: List<Pair<String, () -> Unit>>) {
    val colores = listOf(Paleta.azulOscuro, Paleta.morado, Paleta.verde)
    var n = 0
    secciones.chunked(2).forEach { fila ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            fila.forEach { (texto, accion) ->
                BotonGrande(texto, accion, Modifier.weight(1f).heightIn(min = 110.dp), colores[n % colores.size])
                n++
            }
            if (fila.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** Logo: dos medias lunas (memoria) en azul y lila. */
@Composable
fun Logo(tam: Dp = 96.dp) {
    Canvas(Modifier.size(tam)) {
        drawArc(Paleta.morado, 90f, 180f, true, Offset.Zero, Size(size.width, size.height))
        drawArc(Paleta.azul, 270f, 180f, true, Offset.Zero, Size(size.width, size.height))
        drawCircle(Paleta.verde, size.minDimension * 0.16f, Offset(size.width / 2, size.height / 2))
    }
}

/** Carita para registrar el animo: 1 = mal ... 4 = muy bien. */
@Composable
fun Carita(valor: Int, tam: Dp = 72.dp, marcada: Boolean = false) {
    val color = when (valor) {
        1 -> Paleta.rojo
        2 -> Paleta.naranja
        3 -> Paleta.azul
        else -> Paleta.verde
    }
    Canvas(Modifier.size(tam)) {
        val r = size.minDimension / 2
        drawCircle(color, r)
        if (marcada) drawCircle(Paleta.azulOscuro, r, style = Stroke(width = r * 0.12f))
        val ojo = r * 0.1f
        drawCircle(Paleta.texto, ojo, Offset(size.width * 0.34f, size.height * 0.38f))
        drawCircle(Paleta.texto, ojo, Offset(size.width * 0.66f, size.height * 0.38f))
        val ancho = size.width * 0.4f
        val x = (size.width - ancho) / 2
        val trazo = Stroke(width = r * 0.1f, cap = StrokeCap.Round)
        when (valor) {
            4 -> drawArc(Paleta.texto, 20f, 140f, false, Offset(x, size.height * 0.42f), Size(ancho, size.height * 0.34f), style = trazo)
            3 -> drawArc(Paleta.texto, 30f, 120f, false, Offset(x, size.height * 0.52f), Size(ancho, size.height * 0.16f), style = trazo)
            2 -> drawLine(Paleta.texto, Offset(x, size.height * 0.7f), Offset(x + ancho, size.height * 0.7f), strokeWidth = r * 0.1f, cap = StrokeCap.Round)
            else -> drawArc(Paleta.texto, 210f, 120f, false, Offset(x, size.height * 0.62f), Size(ancho, size.height * 0.16f), style = trazo)
        }
    }
}

/** Foto de un recuerdo (JPEG en Base64) o un recuadro con la inicial si no hay foto. */
@Composable
fun FotoRecuerdo(base64: String, inicial: String, modifier: Modifier = Modifier) {
    val imagen = remember(base64) {
        if (base64.isBlank()) null else runCatching { deBase64(base64).decodeToImageBitmap() }.getOrNull()
    }
    if (imagen != null) {
        Image(
            bitmap = imagen,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(modifier.clip(RoundedCornerShape(14.dp)).background(Paleta.lila), contentAlignment = Alignment.Center) {
            Text(inicial.take(1).uppercase(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Paleta.morado)
        }
    }
}
