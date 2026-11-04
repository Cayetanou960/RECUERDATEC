package es.uam.esalud.recuerdatec.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.uam.esalud.recuerdatec.model.ConfigNube
import es.uam.esalud.recuerdatec.model.Rol

@Composable
fun PantallaInicio(e: AppState) {
    val sesion by e.repo.sesion.collectAsState()
    val s = sesion
    Marco("RecuerdaTec") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Logo(110.dp) }
        Text("Apoyo diario para la memoria", fontSize = 20.sp, color = Paleta.azulOscuro)
        if (s != null) {
            Tarjeta {
                Parrafo("Sesion iniciada como ${s.nombre} (${if (s.rol == Rol.CUIDADOR) "cuidador/a" else "paciente"}).")
            }
            if (s.rol == Rol.PACIENTE && s.espacio == null) {
                BotonGrande("Vincular con mi cuidador/a", { e.ir(Destino.Cuenta) }, color = Paleta.verde)
            } else {
                BotonGrande("Entrar", { e.irRaiz(e.inicio()) })
            }
        } else {
            BotonGrande("Soy paciente", {
                e.rolLocal = Rol.PACIENTE
                e.irRaiz(Destino.PacienteHome)
            })
            BotonGrande("Soy cuidador/a", {
                e.rolLocal = Rol.CUIDADOR
                e.irRaiz(Destino.CuidadorHome)
            }, color = Paleta.morado)
        }
        BotonGrande("Cuenta y sincronizacion", { e.ir(Destino.Cuenta) }, color = Paleta.verde)
        BotonTexto("Ajustes", { e.ir(Destino.Ajustes) })
    }
}

@Composable
fun PantallaCuenta(e: AppState) {
    val sesion by e.repo.sesion.collectAsState()
    val config by e.repo.config.collectAsState()
    val estadoSync by e.repo.estadoSync.collectAsState()
    val s = sesion

    var registro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var rol by remember { mutableStateOf(Rol.CUIDADOR) }
    var codigo by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }

    Marco("Cuenta", alVolver = { e.volver() }) {
        if (!config.configurada) {
            Tarjeta(fondo = Paleta.lila) {
                Parrafo("Para sincronizar hay que configurar Firebase (clave API e id de proyecto).")
                BotonTexto("Ir a Ajustes", { e.ir(Destino.Ajustes) })
            }
        }
        if (s == null) {
            Titulo(if (registro) "Crear cuenta" else "Iniciar sesion")
            if (registro) {
                Campo(nombre, { nombre = it }, "Tu nombre")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonGrande(
                        "Cuidador/a", { rol = Rol.CUIDADOR },
                        Modifier.weight(1f),
                        if (rol == Rol.CUIDADOR) Paleta.morado else Paleta.azul,
                    )
                    BotonGrande(
                        "Paciente", { rol = Rol.PACIENTE },
                        Modifier.weight(1f),
                        if (rol == Rol.PACIENTE) Paleta.morado else Paleta.azul,
                    )
                }
            }
            Campo(email, { email = it }, "Correo electronico", teclado = KeyboardType.Email)
            Campo(pass, { pass = it }, "Contraseña (minimo 6 caracteres)", secreto = true)
            BotonGrande(
                if (registro) "Crear cuenta" else "Entrar",
                {
                    if (email.isBlank() || pass.isBlank() || (registro && nombre.isBlank())) {
                        e.mensaje = "Rellena todos los campos."
                    } else {
                        ocupado = true
                        e.ejecutar {
                            try {
                                if (registro) e.repo.registrar(email, pass, nombre.trim(), rol)
                                else e.repo.iniciarSesion(email, pass)
                                pass = ""
                                val nueva = e.repo.sesion.value
                                if (nueva != null && (nueva.rol == Rol.CUIDADOR || nueva.espacio != null)) {
                                    e.irRaiz(e.inicio())
                                }
                            } finally {
                                ocupado = false
                            }
                        }
                    }
                },
                color = Paleta.verde,
                activo = !ocupado,
            )
            BotonTexto(if (registro) "Ya tengo cuenta" else "Crear una cuenta nueva", { registro = !registro })
        } else {
            Tarjeta {
                Titulo(s.nombre)
                Parrafo(s.email)
                Parrafo("Rol: ${if (s.rol == Rol.CUIDADOR) "cuidador/a" else "paciente"}")
                if (estadoSync.isNotBlank()) Parrafo(estadoSync, Paleta.azulOscuro)
            }
            if (s.rol == Rol.CUIDADOR && s.codigo != null) {
                Tarjeta(fondo = Paleta.lila) {
                    Parrafo("Codigo para vincular al paciente:")
                    Text(s.codigo, fontSize = 40.sp, color = Paleta.morado, modifier = Modifier.padding(vertical = 4.dp))
                    Parrafo("El paciente lo escribe en su movil tras crear su cuenta.")
                }
            }
            if (s.rol == Rol.PACIENTE && s.espacio == null) {
                Titulo("Vincular con tu cuidador/a")
                Campo(codigo, { codigo = it.uppercase() }, "Codigo de 6 caracteres")
                BotonGrande("Vincular", {
                    ocupado = true
                    e.ejecutar {
                        try {
                            e.repo.vincular(codigo)
                            e.irRaiz(e.inicio())
                        } finally {
                            ocupado = false
                        }
                    }
                }, color = Paleta.verde, activo = !ocupado)
            } else {
                BotonGrande("Sincronizar ahora", { e.ejecutar { e.repo.sincronizar() } }, color = Paleta.verde)
            }
            BotonGrande("Cerrar sesion", {
                e.repo.cerrarSesion()
                e.rolLocal = null
                e.irRaiz(Destino.Inicio)
            }, color = Paleta.rojo)
        }
    }
}

@Composable
fun PantallaAjustes(e: AppState) {
    val config by e.repo.config.collectAsState()
    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var proyecto by remember(config) { mutableStateOf(config.projectId) }
    var url by remember(config) { mutableStateOf(config.urlAsistente) }

    Marco("Ajustes", alVolver = { e.volver() }) {
        Titulo("Tamaño del texto")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Normal" to 1f, "Grande" to 1.25f, "Muy grande" to 1.5f).forEach { (t, v) ->
                BotonGrande(
                    t, { e.cambiarEscala(v) }, Modifier.weight(1f),
                    if (e.escala == v) Paleta.morado else Paleta.azul,
                )
            }
        }
        Titulo("Firebase (sincronizacion)")
        Parrafo("Datos de tu proyecto en la consola de Firebase: Configuracion del proyecto.")
        Campo(apiKey, { apiKey = it }, "Clave API web")
        Campo(proyecto, { proyecto = it }, "Id del proyecto")
        Titulo("Asistente con IA (opcional)")
        Parrafo("Direccion del servidor backend. Si se deja vacio, el asistente funciona sin Internet con reglas sencillas.")
        Campo(url, { url = it }, "URL, por ejemplo http://192.168.1.20:8787")
        BotonGrande("Guardar", {
            e.repo.guardarConfig(ConfigNube(apiKey, proyecto, url))
            e.mensaje = "Ajustes guardados."
        }, color = Paleta.verde)
    }
}
