package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.uam.esalud.recuerdatec.logica.proximosEventos
import es.uam.esalud.recuerdatec.logica.tomasPendientes
import es.uam.esalud.recuerdatec.model.EventoAgenda
import es.uam.esalud.recuerdatec.model.Medicamento
import es.uam.esalud.recuerdatec.model.Recuerdo
import es.uam.esalud.recuerdatec.model.Toma
import es.uam.esalud.recuerdatec.util.aBase64
import es.uam.esalud.recuerdatec.util.ahoraMillis
import es.uam.esalud.recuerdatec.util.fechaLarga
import es.uam.esalud.recuerdatec.util.fechaValida
import es.uam.esalud.recuerdatec.util.hoyIso
import es.uam.esalud.recuerdatec.util.minutosAhora
import es.uam.esalud.recuerdatec.util.nuevoId
import es.uam.esalud.recuerdatec.util.normalizarHora
import es.uam.esalud.recuerdatec.util.reducirFoto
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

// ---------- recuerdos ----------

@Composable
fun PantallaRecuerdos(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    val cuida = e.esCuidador()
    Marco(if (cuida) "Recuerdos" else "Mis recuerdos", alVolver = { e.volver() }) {
        if (cuida) BotonGrande("Añadir recuerdo", { e.ir(Destino.EditarRecuerdo(null)) }, color = Paleta.verde)
        if (datos.recuerdos.isEmpty()) {
            Parrafo(
                if (cuida) "Todavia no hay recuerdos. Añade fotos de personas, lugares o momentos importantes."
                else "Tu cuidador/a aun no ha añadido recuerdos.",
            )
        }
        datos.recuerdos.forEach { r ->
            Tarjeta(alPulsar = if (cuida) ({ e.ir(Destino.EditarRecuerdo(r.id)) }) else null) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    FotoRecuerdo(r.foto, r.titulo, Modifier.size(if (cuida) 80.dp else 120.dp))
                    androidx.compose.foundation.layout.Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Titulo(r.titulo)
                        if (r.persona.isNotBlank()) Parrafo(r.persona, Paleta.morado)
                        if (r.descripcion.isNotBlank()) Parrafo(r.descripcion)
                    }
                }
            }
        }
    }
}

@Composable
fun SelectorFoto(foto: String, alElegir: (String) -> Unit, alFallar: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val selector = rememberFilePickerLauncher(type = FileKitType.Image) { archivo ->
        if (archivo != null) {
            scope.launch {
                try {
                    val reducida = reducirFoto(archivo.readBytes())
                    if (reducida == null) alFallar("No se ha podido usar esa foto (puede ser demasiado grande).")
                    else alElegir(aBase64(reducida))
                } catch (ex: Exception) {
                    alFallar("No se ha podido leer la foto.")
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        FotoRecuerdo(foto, "+", Modifier.size(96.dp))
        BotonGrande(if (foto.isBlank()) "Elegir foto" else "Cambiar foto", { selector.launch() }, Modifier.weight(1f), Paleta.morado)
    }
}

@Composable
fun PantallaEditarRecuerdo(e: AppState, id: String?) {
    val existente = remember(id) { id?.let { i -> e.repo.cuidador.value.recuerdos.firstOrNull { it.id == i } } }
    var titulo by remember { mutableStateOf(existente?.titulo ?: "") }
    var persona by remember { mutableStateOf(existente?.persona ?: "") }
    var descripcion by remember { mutableStateOf(existente?.descripcion ?: "") }
    var foto by remember { mutableStateOf(existente?.foto ?: "") }

    Marco(if (id == null) "Nuevo recuerdo" else "Editar recuerdo", alVolver = { e.volver() }) {
        SelectorFoto(foto, { foto = it }, { e.mensaje = it })
        Campo(titulo, { titulo = it }, "Titulo (ej. Mi boda)")
        Campo(persona, { persona = it }, "Persona (ej. Maria, mi hija)")
        Campo(descripcion, { descripcion = it }, "Descripcion", lineas = 4)
        BotonGrande("Guardar", {
            if (titulo.isBlank()) {
                e.mensaje = "Escribe un titulo."
            } else {
                val nuevo = Recuerdo(id ?: nuevoId(), titulo.trim(), persona.trim(), descripcion.trim(), foto)
                e.repo.modificarCuidador { d ->
                    d.copy(recuerdos = if (id == null) d.recuerdos + nuevo else d.recuerdos.map { if (it.id == id) nuevo else it })
                }
                e.volver()
            }
        }, color = Paleta.verde)
        if (id != null) {
            BotonGrande("Eliminar", {
                e.repo.modificarCuidador { d -> d.copy(recuerdos = d.recuerdos.filter { it.id != id }) }
                e.volver()
            }, color = Paleta.rojo)
        }
    }
}

// ---------- agenda ----------

@Composable
fun PantallaAgenda(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    val cuida = e.esCuidador()
    val hoy = hoyIso()
    val eventos = proximosEventos(datos.agenda, hoy)
    Marco(if (cuida) "Agenda" else "Mi agenda", alVolver = { e.volver() }) {
        if (cuida) BotonGrande("Añadir evento", { e.ir(Destino.EditarEvento(null)) }, color = Paleta.verde)
        if (eventos.isEmpty()) Parrafo("No hay eventos proximos.")
        eventos.forEach { ev ->
            Tarjeta(
                fondo = if (ev.fecha == hoy) Paleta.lila else androidx.compose.ui.graphics.Color.White,
                alPulsar = if (cuida) ({ e.ir(Destino.EditarEvento(ev.id)) }) else null,
            ) {
                Parrafo(if (ev.fecha == hoy) "Hoy" else fechaLarga(ev.fecha), Paleta.morado)
                Titulo((if (ev.hora.isNotBlank()) ev.hora + "  " else "") + ev.titulo)
                if (ev.notas.isNotBlank()) Parrafo(ev.notas)
            }
        }
    }
}

@Composable
fun PantallaEditarEvento(e: AppState, id: String?) {
    val existente = remember(id) { id?.let { i -> e.repo.cuidador.value.agenda.firstOrNull { it.id == i } } }
    var titulo by remember { mutableStateOf(existente?.titulo ?: "") }
    var fecha by remember { mutableStateOf(existente?.fecha ?: hoyIso()) }
    var hora by remember { mutableStateOf(existente?.hora ?: "") }
    var notas by remember { mutableStateOf(existente?.notas ?: "") }

    Marco(if (id == null) "Nuevo evento" else "Editar evento", alVolver = { e.volver() }) {
        Campo(titulo, { titulo = it }, "Titulo (ej. Revision con el medico)")
        Campo(fecha, { fecha = it }, "Fecha (AAAA-MM-DD)", teclado = KeyboardType.Number)
        Campo(hora, { hora = it }, "Hora (HH:MM, opcional)", teclado = KeyboardType.Number)
        Campo(notas, { notas = it }, "Notas", lineas = 3)
        BotonGrande("Guardar", {
            val horaOk = if (hora.isBlank()) "" else normalizarHora(hora)
            when {
                titulo.isBlank() -> e.mensaje = "Escribe un titulo."
                !fechaValida(fecha) -> e.mensaje = "La fecha debe tener el formato AAAA-MM-DD, por ejemplo 2026-11-26."
                horaOk == null -> e.mensaje = "La hora debe tener el formato HH:MM, por ejemplo 09:30."
                else -> {
                    val nuevo = EventoAgenda(id ?: nuevoId(), titulo.trim(), fecha.trim(), horaOk, notas.trim())
                    e.repo.modificarCuidador { d ->
                        d.copy(agenda = if (id == null) d.agenda + nuevo else d.agenda.map { if (it.id == id) nuevo else it })
                    }
                    e.volver()
                }
            }
        }, color = Paleta.verde)
        if (id != null) {
            BotonGrande("Eliminar", {
                e.repo.modificarCuidador { d -> d.copy(agenda = d.agenda.filter { it.id != id }) }
                e.volver()
            }, color = Paleta.rojo)
        }
    }
}

// ---------- medicacion ----------

@Composable
fun PantallaMedicacion(e: AppState) {
    val datos by e.repo.cuidador.collectAsState()
    val paciente by e.repo.paciente.collectAsState()
    val cuida = e.esCuidador()
    val hoy = hoyIso()
    val pendientes = tomasPendientes(datos.medicamentos, paciente.tomas, hoy, minutosAhora())
    val hechasHoy = paciente.tomas.filter { it.fecha == hoy }.sortedBy { it.hora }

    Marco(if (cuida) "Medicacion" else "Mis medicinas", alVolver = { e.volver() }) {
        if (cuida) {
            BotonGrande("Añadir medicamento", { e.ir(Destino.EditarMedicamento(null)) }, color = Paleta.verde)
            if (datos.medicamentos.isEmpty()) Parrafo("Todavia no hay medicamentos.")
            datos.medicamentos.forEach { m ->
                Tarjeta(alPulsar = { e.ir(Destino.EditarMedicamento(m.id)) }) {
                    Titulo(m.nombre + if (m.dosis.isNotBlank()) "  (${m.dosis})" else "")
                    Parrafo("Horas: " + m.horas.joinToString(", ").ifBlank { "sin horas" })
                    if (m.instrucciones.isNotBlank()) Parrafo(m.instrucciones)
                }
            }
            Titulo("Hoy")
            Parrafo("Tomas confirmadas: ${hechasHoy.size}. Pendientes: ${pendientes.size}.")
        } else {
            Titulo("Pendientes de hoy")
            if (pendientes.isEmpty()) Parrafo("No tienes ninguna toma pendiente. ¡Muy bien!", Paleta.verde)
            pendientes.forEach { p ->
                Tarjeta(fondo = if (p.retrasada) androidx.compose.ui.graphics.Color(0xFFF6D9D5) else Paleta.lila) {
                    Titulo("${p.hora}  ${p.medicamento.nombre}")
                    if (p.medicamento.dosis.isNotBlank()) Parrafo(p.medicamento.dosis)
                    if (p.medicamento.instrucciones.isNotBlank()) Parrafo(p.medicamento.instrucciones)
                    if (p.retrasada) Parrafo("Ya ha pasado la hora.", Paleta.rojo)
                    BotonGrande("Ya la he tomado", {
                        val toma = Toma(nuevoId(), p.medicamento.id, p.medicamento.nombre, hoy, p.hora, ahoraMillis())
                        e.repo.modificarPaciente { it.copy(tomas = it.tomas + toma) }
                    }, color = Paleta.verde)
                }
            }
            if (hechasHoy.isNotEmpty()) {
                Titulo("Ya tomadas hoy")
                hechasHoy.forEach { Parrafo("${it.hora}  ${it.nombreMedicamento}") }
            }
        }
    }
}

@Composable
fun PantallaEditarMedicamento(e: AppState, id: String?) {
    val existente = remember(id) { id?.let { i -> e.repo.cuidador.value.medicamentos.firstOrNull { it.id == i } } }
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var dosis by remember { mutableStateOf(existente?.dosis ?: "") }
    var horas by remember { mutableStateOf(existente?.horas?.joinToString(", ") ?: "") }
    var instrucciones by remember { mutableStateOf(existente?.instrucciones ?: "") }

    Marco(if (id == null) "Nuevo medicamento" else "Editar medicamento", alVolver = { e.volver() }) {
        Campo(nombre, { nombre = it }, "Nombre")
        Campo(dosis, { dosis = it }, "Dosis (ej. 1 pastilla, 5 mg)")
        Campo(horas, { horas = it }, "Horas separadas por comas (ej. 08:00, 20:00)")
        Campo(instrucciones, { instrucciones = it }, "Instrucciones (ej. con el desayuno)", lineas = 3)
        BotonGrande("Guardar", {
            val lista = horas.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { normalizarHora(it) }
            when {
                nombre.isBlank() -> e.mensaje = "Escribe el nombre del medicamento."
                lista.isEmpty() -> e.mensaje = "Indica al menos una hora."
                lista.any { it == null } -> e.mensaje = "Las horas deben tener el formato HH:MM, por ejemplo 08:00."
                else -> {
                    val nuevo = Medicamento(id ?: nuevoId(), nombre.trim(), dosis.trim(), lista.filterNotNull().distinct().sorted(), instrucciones.trim())
                    e.repo.modificarCuidador { d ->
                        d.copy(medicamentos = if (id == null) d.medicamentos + nuevo else d.medicamentos.map { if (it.id == id) nuevo else it })
                    }
                    e.volver()
                }
            }
        }, color = Paleta.verde)
        if (id != null) {
            BotonGrande("Eliminar", {
                e.repo.modificarCuidador { d -> d.copy(medicamentos = d.medicamentos.filter { it.id != id }) }
                e.volver()
            }, color = Paleta.rojo)
        }
    }
}

