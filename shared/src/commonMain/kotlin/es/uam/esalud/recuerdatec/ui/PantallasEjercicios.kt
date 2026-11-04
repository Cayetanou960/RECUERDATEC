package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.uam.esalud.recuerdatec.logica.PreguntaQuien
import es.uam.esalud.recuerdatec.logica.nuevoJuego
import es.uam.esalud.recuerdatec.logica.ocultarFallo
import es.uam.esalud.recuerdatec.logica.preguntasQuien
import es.uam.esalud.recuerdatec.logica.tocarCarta
import es.uam.esalud.recuerdatec.model.ResultadoEjercicio
import es.uam.esalud.recuerdatec.util.ahoraMillis
import es.uam.esalud.recuerdatec.util.nuevoId
import kotlinx.coroutines.delay

@Composable
fun PantallaEjercicios(e: AppState) {
    Marco("Ejercicios", alVolver = { e.volver() }) {
        Parrafo("Unos minutos al dia ayudan a mantener la memoria activa.")
        BotonGrande("Encuentra las parejas", { e.ir(Destino.Parejas) })
        BotonGrande("¿Quien es?", { e.ir(Destino.QuienEs) }, color = Paleta.morado)
        BotonGrande("Mis resultados", { e.ir(Destino.Seguimiento) }, color = Paleta.verde)
    }
}

@Composable
fun PantallaParejas(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    var partida by remember { mutableStateOf(0) }
    var juego by remember(partida) { mutableStateOf(nuevoJuego(datos.recuerdos, ahoraMillis())) }
    var guardado by remember(partida) { mutableStateOf(false) }

    LaunchedEffect(juego.seleccion) {
        if (juego.hayFallo) {
            delay(1000)
            juego = ocultarFallo(juego)
        }
    }
    LaunchedEffect(juego.completado) {
        if (juego.completado && !guardado) {
            guardado = true
            val segundos = ((ahoraMillis() - juego.inicioMillis) / 1000).toInt()
            val r = ResultadoEjercicio(nuevoId(), "Parejas", ahoraMillis(), juego.aciertos, juego.intentos, segundos)
            e.repo.modificarPaciente { it.copy(resultados = it.resultados + r) }
        }
    }

    Marco("Encuentra las parejas", alVolver = { e.volver() }) {
        Parrafo("Intentos: ${juego.intentos}")
        juego.cartas.chunked(3).forEach { fila ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { c ->
                    val color = when {
                        c.resuelta -> Paleta.verde
                        c.visible -> Paleta.morado
                        else -> Paleta.azulOscuro
                    }
                    BotonGrande(
                        if (c.visible || c.resuelta) c.par else "?",
                        { juego = tocarCarta(juego, c.id) },
                        Modifier.weight(1f).heightIn(min = 90.dp),
                        color,
                    )
                }
            }
        }
        if (juego.completado) {
            Tarjeta(fondo = Paleta.lila) {
                Titulo("¡Muy bien!")
                Parrafo("Has encontrado todas las parejas en ${juego.intentos} intentos.")
            }
            BotonGrande("Jugar otra vez", { partida++ }, color = Paleta.verde)
        }
    }
}

@Composable
fun PantallaQuienEs(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    var partida by remember { mutableStateOf(0) }
    val preguntas: List<PreguntaQuien> = remember(partida) { preguntasQuien(datos.recuerdos) }
    var indice by remember(partida) { mutableStateOf(0) }
    var aciertos by remember(partida) { mutableStateOf(0) }
    var respuesta by remember(partida, indice) { mutableStateOf<String?>(null) }
    var inicio by remember(partida) { mutableStateOf(ahoraMillis()) }
    var guardado by remember(partida) { mutableStateOf(false) }

    Marco("¿Quien es?", alVolver = { e.volver() }) {
        if (preguntas.isEmpty()) {
            Parrafo("Para jugar, tu cuidador/a debe añadir recuerdos con foto y el nombre de la persona.")
        } else if (indice >= preguntas.size) {
            LaunchedEffect(partida) {
                if (!guardado) {
                    guardado = true
                    val seg = ((ahoraMillis() - inicio) / 1000).toInt()
                    val r = ResultadoEjercicio(nuevoId(), "Quien es", ahoraMillis(), aciertos, preguntas.size, seg)
                    e.repo.modificarPaciente { it.copy(resultados = it.resultados + r) }
                }
            }
            Tarjeta(fondo = Paleta.lila) {
                Titulo("¡Terminado!")
                Parrafo("Has acertado $aciertos de ${preguntas.size}.")
            }
            BotonGrande("Jugar otra vez", { partida++ }, color = Paleta.verde)
        } else {
            val p = preguntas[indice]
            Parrafo("Pregunta ${indice + 1} de ${preguntas.size}")
            FotoRecuerdo(p.recuerdo.foto, p.correcta, Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 320.dp))
            Titulo("¿Quien es esta persona?")
            p.opciones.forEach { op ->
                val r = respuesta
                val color = when {
                    r == null -> Paleta.azulOscuro
                    op == p.correcta -> Paleta.verde
                    op == r -> Paleta.rojo
                    else -> Paleta.azul
                }
                BotonGrande(op, {
                    if (respuesta == null) {
                        respuesta = op
                        if (op == p.correcta) aciertos++
                    }
                }, color = color)
            }
            if (respuesta != null) {
                Parrafo(if (respuesta == p.correcta) "¡Correcto!" else "Era ${p.correcta}.")
                BotonGrande("Siguiente", { indice++ }, color = Paleta.morado)
            }
        }
    }
}
