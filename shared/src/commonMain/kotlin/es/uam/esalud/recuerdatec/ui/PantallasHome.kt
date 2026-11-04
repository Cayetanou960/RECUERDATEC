package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import es.uam.esalud.recuerdatec.logica.eventosDeHoy
import es.uam.esalud.recuerdatec.logica.tomasPendientes
import es.uam.esalud.recuerdatec.model.Rol
import es.uam.esalud.recuerdatec.util.ahoraMillis
import es.uam.esalud.recuerdatec.util.fechaLarga
import es.uam.esalud.recuerdatec.util.hoyIso
import es.uam.esalud.recuerdatec.util.llamarTelefono
import es.uam.esalud.recuerdatec.util.minutosAhora

@Composable
fun PantallaPacienteHome(e: AppState) {
    val cuidador by e.repo.cuidador.collectAsState()
    val paciente by e.repo.paciente.collectAsState()
    val sesion by e.repo.sesion.collectAsState()
    val estadoSync by e.repo.estadoSync.collectAsState()

    LaunchedEffect(Unit) {
        if (ahoraMillis() - e.repo.paciente.value.ultimaInteraccion > 60_000) {
            e.repo.modificarPaciente { it.copy(ultimaInteraccion = ahoraMillis()) }
        }
    }

    val nombre = cuidador.perfil.nombrePaciente.ifBlank { paciente.nombre }
    val hoy = hoyIso()
    val pendientes = tomasPendientes(cuidador.medicamentos, paciente.tomas, hoy, minutosAhora())
    val eventos = eventosDeHoy(cuidador.agenda, hoy)

    Marco(if (nombre.isBlank()) "Hola" else "Hola, $nombre") {
        Text(fechaLarga(hoy), fontSize = 22.sp, color = Paleta.azulOscuro)
        if (pendientes.isNotEmpty()) {
            Tarjeta(fondo = Paleta.lila, alPulsar = { e.ir(Destino.Medicacion) }) {
                Titulo("Medicinas pendientes")
                pendientes.take(3).forEach { Parrafo("${it.hora}  ${it.medicamento.nombre} ${it.medicamento.dosis}") }
            }
        }
        if (eventos.isNotEmpty()) {
            Tarjeta(alPulsar = { e.ir(Destino.Agenda) }) {
                Titulo("Hoy")
                eventos.take(3).forEach { Parrafo((if (it.hora.isNotBlank()) it.hora + "  " else "") + it.titulo) }
            }
        }
        RejillaSecciones(
            listOf(
                "Mis recuerdos" to { e.ir(Destino.Recuerdos) },
                "Mi agenda" to { e.ir(Destino.Agenda) },
                "Mis medicinas" to { e.ir(Destino.Medicacion) },
                "Ejercicios" to { e.ir(Destino.Ejercicios) },
                "Asistente" to { e.ir(Destino.Asistente) },
                "Como me siento" to { e.ir(Destino.Animo) },
            ),
        )
        BotonGrande("Llamar a mi cuidador/a", {
            val tel = cuidador.perfil.telefonoCuidador.trim()
            if (tel.isEmpty()) {
                e.mensaje = "Todavia no hay telefono guardado. Pidele a tu cuidador/a que lo anada."
            } else if (!llamarTelefono(tel)) {
                e.mensaje = "Llama al $tel"
            }
        }, color = Paleta.verde)
        if (sesion != null && estadoSync.isNotBlank()) Parrafo(estadoSync, Paleta.azulOscuro)
        if (sesion == null) {
            BotonTexto("Cambiar de perfil", {
                e.rolLocal = null
                e.irRaiz(Destino.Inicio)
            })
        } else {
            BotonTexto("Cuenta", { e.ir(Destino.Cuenta) })
        }
        BotonTexto("Ajustes", { e.ir(Destino.Ajustes) })
    }
}

@Composable
fun PantallaCuidadorHome(e: AppState) {
    val cuidador by e.repo.cuidador.collectAsState()
    val paciente by e.repo.paciente.collectAsState()
    val sesion by e.repo.sesion.collectAsState()
    val estadoSync by e.repo.estadoSync.collectAsState()
    val hoy = hoyIso()
    val pendientes = tomasPendientes(cuidador.medicamentos, paciente.tomas, hoy, minutosAhora())
    val retrasadas = pendientes.count { it.retrasada }

    Marco("Panel del cuidador") {
        val nombre = cuidador.perfil.nombrePaciente.ifBlank { paciente.nombre }
        Text(fechaLarga(hoy), fontSize = 20.sp, color = Paleta.azulOscuro)
        Tarjeta {
            Titulo(if (nombre.isBlank()) "Paciente" else nombre)
            Parrafo("${cuidador.recuerdos.size} recuerdos, ${eventosDeHoy(cuidador.agenda, hoy).size} eventos hoy")
            if (retrasadas > 0) {
                Parrafo("Atencion: $retrasadas toma(s) con retraso.", Paleta.rojo)
            } else {
                Parrafo("Medicacion al dia.", Paleta.verde)
            }
        }
        RejillaSecciones(
            listOf(
                "Recuerdos" to { e.ir(Destino.Recuerdos) },
                "Agenda" to { e.ir(Destino.Agenda) },
                "Medicacion" to { e.ir(Destino.Medicacion) },
                "Seguimiento" to { e.ir(Destino.Seguimiento) },
                "Perfil del asistente" to { e.ir(Destino.PerfilIA) },
                "Ver como paciente" to {
                    if (sesion == null) {
                        e.rolLocal = Rol.PACIENTE
                        e.irRaiz(Destino.PacienteHome)
                    } else {
                        e.mensaje = "Con cuenta, el paciente usa su propio dispositivo."
                    }
                },
            ),
        )
        if (sesion != null) {
            if (estadoSync.isNotBlank()) Parrafo(estadoSync, Paleta.azulOscuro)
            sesion?.codigo?.let { Parrafo("Codigo de vinculacion: $it") }
            BotonTexto("Cuenta", { e.ir(Destino.Cuenta) })
        } else {
            BotonTexto("Cambiar de perfil", {
                e.rolLocal = null
                e.irRaiz(Destino.Inicio)
            })
        }
        BotonTexto("Ajustes", { e.ir(Destino.Ajustes) })
    }
}

