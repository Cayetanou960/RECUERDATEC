package es.uam.esalud.recuerdatec

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.uam.esalud.recuerdatec.ui.AppState
import es.uam.esalud.recuerdatec.ui.BotonTexto
import es.uam.esalud.recuerdatec.ui.Destino
import es.uam.esalud.recuerdatec.ui.PantallaAgenda
import es.uam.esalud.recuerdatec.ui.PantallaAjustes
import es.uam.esalud.recuerdatec.ui.PantallaAnimo
import es.uam.esalud.recuerdatec.ui.PantallaAsistente
import es.uam.esalud.recuerdatec.ui.PantallaCuenta
import es.uam.esalud.recuerdatec.ui.PantallaCuidadorHome
import es.uam.esalud.recuerdatec.ui.PantallaEditarEvento
import es.uam.esalud.recuerdatec.ui.PantallaEditarMedicamento
import es.uam.esalud.recuerdatec.ui.PantallaEditarRecuerdo
import es.uam.esalud.recuerdatec.ui.PantallaEjercicios
import es.uam.esalud.recuerdatec.ui.PantallaInicio
import es.uam.esalud.recuerdatec.ui.PantallaMedicacion
import es.uam.esalud.recuerdatec.ui.PantallaPacienteHome
import es.uam.esalud.recuerdatec.ui.PantallaParejas
import es.uam.esalud.recuerdatec.ui.PantallaPerfilIA
import es.uam.esalud.recuerdatec.ui.PantallaQuienEs
import es.uam.esalud.recuerdatec.ui.PantallaRecuerdos
import es.uam.esalud.recuerdatec.ui.PantallaSeguimiento
import es.uam.esalud.recuerdatec.ui.Paleta
import es.uam.esalud.recuerdatec.ui.Tarjeta
import es.uam.esalud.recuerdatec.ui.Tema
import es.uam.esalud.recuerdatec.util.ManejarAtras
import kotlinx.coroutines.delay

/** Punto de entrada comun a Android, escritorio y web. */
@Composable
fun App() {
    val estado = remember { AppState() }

    // Sincronizacion periodica con la nube (solo hace algo si hay sesion con espacio compartido).
    LaunchedEffect(Unit) {
        while (true) {
            estado.repo.sincronizar()
            delay(10_000)
        }
    }

    ManejarAtras(activo = estado.pila.size > 1) { estado.volver() }

    Tema(estado.escala) {
        Box(Modifier.fillMaxSize()) {
            Navegador(estado)
            val msg = estado.mensaje
            if (msg != null) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
                    Tarjeta(fondo = Paleta.lila) {
                        Text(msg, color = Paleta.texto)
                        BotonTexto("Entendido", { estado.mensaje = null })
                    }
                }
            }
        }
    }
}

@Composable
private fun Navegador(e: AppState) {
    when (val d = e.actual) {
        Destino.Inicio -> PantallaInicio(e)
        Destino.Cuenta -> PantallaCuenta(e)
        Destino.Ajustes -> PantallaAjustes(e)
        Destino.PacienteHome -> PantallaPacienteHome(e)
        Destino.CuidadorHome -> PantallaCuidadorHome(e)
        Destino.Recuerdos -> PantallaRecuerdos(e)
        is Destino.EditarRecuerdo -> PantallaEditarRecuerdo(e, d.id)
        Destino.Agenda -> PantallaAgenda(e)
        is Destino.EditarEvento -> PantallaEditarEvento(e, d.id)
        Destino.Medicacion -> PantallaMedicacion(e)
        is Destino.EditarMedicamento -> PantallaEditarMedicamento(e, d.id)
        Destino.Ejercicios -> PantallaEjercicios(e)
        Destino.Parejas -> PantallaParejas(e)
        Destino.QuienEs -> PantallaQuienEs(e)
        Destino.Seguimiento -> PantallaSeguimiento(e)
        Destino.Asistente -> PantallaAsistente(e)
        Destino.Animo -> PantallaAnimo(e)
        Destino.PerfilIA -> PantallaPerfilIA(e)
    }
}
