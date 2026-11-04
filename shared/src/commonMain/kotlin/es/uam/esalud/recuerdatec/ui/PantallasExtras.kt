package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.uam.esalud.recuerdatec.asistente.Asistente
import es.uam.esalud.recuerdatec.asistente.AsistenteLocal
import es.uam.esalud.recuerdatec.asistente.AsistenteRemoto
import es.uam.esalud.recuerdatec.asistente.ContextoAsistente
import es.uam.esalud.recuerdatec.logica.adherencia
import es.uam.esalud.recuerdatec.logica.porcentajeAciertos
import es.uam.esalud.recuerdatec.model.AnimoRegistro
import es.uam.esalud.recuerdatec.model.PerfilIA
import es.uam.esalud.recuerdatec.util.ahoraMillis
import es.uam.esalud.recuerdatec.util.fechaDeMillis
import es.uam.esalud.recuerdatec.util.fechaHaceDias
import es.uam.esalud.recuerdatec.util.horaTexto
import es.uam.esalud.recuerdatec.util.hoyIso
import es.uam.esalud.recuerdatec.util.minutosAhora
import es.uam.esalud.recuerdatec.util.nuevoId

// ---------- seguimiento (cuidador) / resultados (paciente) ----------

@Composable
fun PantallaSeguimiento(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    val paciente by e.repo.paciente.collectAsState()
    val cuida = e.esCuidador()
    val semana = (0..6).map { fechaHaceDias(it) }
    val resultados = paciente.resultados.sortedByDescending { it.fechaMillis }
    val animos = paciente.animos.sortedByDescending { it.fechaMillis }

    Marco(if (cuida) "Seguimiento" else "Mis resultados", alVolver = { e.volver() }) {
        Tarjeta {
            Titulo("Ejercicios")
            Parrafo("Sesiones: ${resultados.size}. Aciertos: ${porcentajeAciertos(resultados)}%")
            resultados.take(6).forEach {
                Parrafo("${fechaDeMillis(it.fechaMillis)}  ${it.ejercicio}: ${it.aciertos}/${it.intentos} en ${it.segundos} s")
            }
            if (resultados.isEmpty()) Parrafo("Aun no hay ejercicios hechos.")
        }
        if (cuida) {
            Tarjeta {
                Titulo("Medicacion (ultimos 7 dias)")
                Parrafo("Tomas confirmadas: ${adherencia(datos.medicamentos, paciente.tomas, semana)}%")
            }
            Tarjeta {
                Titulo("Actividad")
                Parrafo(
                    if (paciente.ultimaInteraccion == 0L) "El paciente aun no ha usado la app."
                    else "Ultimo uso: ${fechaDeMillis(paciente.ultimaInteraccion)}",
                )
            }
        }
        Tarjeta {
            Titulo("Estado de animo")
            if (animos.isEmpty()) Parrafo("Sin registros todavia.")
            animos.take(7).forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Carita(it.valor, 40.dp)
                    Parrafo(fechaDeMillis(it.fechaMillis))
                }
            }
        }
    }
}

// ---------- animo ----------

@Composable
fun PantallaAnimo(e: AppState) {
    val paciente by e.repo.paciente.collectAsState()
    var elegido by remember { mutableStateOf(0) }
    val etiquetas = listOf("Mal", "Regular", "Bien", "Muy bien")

    Marco("¿Como te sientes?", alVolver = { e.volver() }) {
        Parrafo("Toca la cara que mejor te represente ahora.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            (1..4).forEach { v ->
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        elegido = v
                        e.repo.modificarPaciente { it.copy(animos = it.animos + AnimoRegistro(nuevoId(), ahoraMillis(), v)) }
                    }.padding(4.dp),
                ) {
                    Carita(v, 64.dp, marcada = elegido == v)
                    Parrafo(etiquetas[v - 1])
                }
            }
        }
        if (elegido != 0) {
            Tarjeta(fondo = Paleta.lila) {
                Parrafo(
                    if (elegido <= 2) "Gracias por contarlo. Si lo necesitas, habla con tu cuidador/a."
                    else "¡Me alegro! Gracias por contarlo.",
                )
            }
        }
        Parrafo("Registros guardados: ${paciente.animos.size}")
    }
}

// ---------- asistente ----------

@Composable
fun PantallaAsistente(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    val paciente by e.repo.paciente.collectAsState()
    val config by e.repo.config.collectAsState()
    val mensajes = remember { mutableStateListOf<Pair<Boolean, String>>() }
    var texto by remember { mutableStateOf("") }
    var pensando by remember { mutableStateOf(false) }
    val asistente: Asistente = remember(config.urlAsistente) {
        if (config.urlAsistente.isBlank()) AsistenteLocal() else AsistenteRemoto(config.urlAsistente)
    }

    fun preguntar(pregunta: String) {
        if (pregunta.isBlank() || pensando) return
        mensajes.add(true to pregunta)
        texto = ""
        pensando = true
        e.ejecutar {
            try {
                val ctx = ContextoAsistente(
                    datos.perfil, datos.recuerdos, datos.agenda, datos.medicamentos, paciente.tomas,
                    hoyIso(), horaTexto(), minutosAhora(),
                )
                mensajes.add(false to asistente.responder(pregunta, ctx))
            } finally {
                pensando = false
            }
        }
    }

    Marco("Asistente", alVolver = { e.volver() }) {
        if (mensajes.isEmpty()) {
            Parrafo("Pregunta lo que necesites o toca una de estas opciones:")
            BotonGrande("¿Que dia es hoy?", { preguntar("¿Que dia es hoy?") })
            BotonGrande("¿Que tengo hoy?", { preguntar("¿Que tengo hoy?") }, color = Paleta.morado)
            BotonGrande("¿Que medicinas me tocan?", { preguntar("¿Que medicinas me tocan?") }, color = Paleta.verde)
        }
        mensajes.forEach { (mio, t) ->
            Tarjeta(fondo = if (mio) Paleta.azul else androidx.compose.ui.graphics.Color.White) {
                Parrafo(t)
            }
        }
        if (pensando) Parrafo("Pensando...", Paleta.azulOscuro)
        Campo(texto, { texto = it }, "Escribe tu pregunta")
        BotonGrande("Enviar", { preguntar(texto) }, activo = !pensando)
    }
}

// ---------- perfil del asistente (cuidador) ----------

@Composable
fun PantallaPerfilIA(e: AppState) {
    val actual = remember { e.repo.cuidador.value.perfil }
    var p by remember { mutableStateOf(actual) }
    fun campo(v: PerfilIA) {
        p = v
    }

    Marco("Perfil del asistente", alVolver = { e.volver() }) {
        Parrafo("Esta informacion ayuda al asistente a responder de forma cercana y personalizada.")
        Campo(p.nombrePaciente, { campo(p.copy(nombrePaciente = it)) }, "Nombre del paciente")
        Campo(p.nombreCuidador, { campo(p.copy(nombreCuidador = it)) }, "Tu nombre (cuidador/a)")
        Campo(p.telefonoCuidador, { campo(p.copy(telefonoCuidador = it)) }, "Tu telefono", teclado = KeyboardType.Phone)
        Campo(p.familiares, { campo(p.copy(familiares = it)) }, "Familiares (ej. Maria, hija; Luis, nieto)", lineas = 2)
        Campo(p.aficiones, { campo(p.copy(aficiones = it)) }, "Aficiones y gustos", lineas = 2)
        Campo(p.lugares, { campo(p.copy(lugares = it)) }, "Lugares importantes", lineas = 2)
        Campo(p.tono, { campo(p.copy(tono = it)) }, "Tono preferido (ej. cercano y tranquilo)")
        Campo(p.notas, { campo(p.copy(notas = it)) }, "Otras notas", lineas = 3)
        BotonGrande("Guardar", {
            e.repo.modificarCuidador { it.copy(perfil = p) }
            e.mensaje = "Perfil guardado."
            e.volver()
        }, color = Paleta.verde)
    }
}
