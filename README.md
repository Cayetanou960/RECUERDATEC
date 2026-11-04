# RecuerdaTec

Aplicacion de apoyo a personas con Alzheimer o deterioro cognitivo leve y a sus cuidadores
(proyecto de E-SALUD, UAM). Hecha con **Kotlin Multiplatform + Compose Multiplatform**: un unico
codigo para **Android**, **escritorio** y **web**.

## Estructura

| Modulo | Contenido |
|---|---|
| `shared` | Toda la app: modelos, logica, datos locales y en la nube, asistente y pantallas |
| `androidApp` | Aplicacion Android (`MainActivity`, manifiesto, icono) |
| `desktopApp` | Aplicacion de escritorio (Windows, macOS, Linux) |
| `webApp` | Version web (WebAssembly) |
| `backend` | Servidor Node opcional para el asistente con IA |
| `firestore.rules` | Reglas de seguridad de Firestore |

## Que incluye

- **v1.0 (sin cuenta, todo en el dispositivo)**: seleccion de perfil (paciente / cuidador), recuerdos con foto,
  agenda, ejercicios de memoria (parejas y "¿Quien es?"), resultados y persistencia local.
- **v2.0 (con Firebase)**: cuentas con correo y contraseña, roles, vinculacion paciente-cuidador por
  codigo de 6 caracteres, sincronizacion con Firestore cada 10 s y medicacion con confirmacion de tomas.
- **v3.0 (parcial)**: asistente conversacional (reglas locales o IA con el backend), registro de
  estado de animo, panel de seguimiento y ajuste de tamaño de texto.

No incluido (queda como trabajo futuro): voz (dictado y lectura), zona segura con GPS,
notificaciones push/recordatorios con la app cerrada y vinculacion por QR.

## Abrirlo en Android Studio

1. `git clone https://github.com/Cayetanou960/RECUERDATEC.git` y abrir la carpeta en Android Studio
   (Ladybug o posterior, JDK 17).
2. Esperar a que termine la sincronizacion de Gradle.
3. **Android**: elegir la configuracion `androidApp` y pulsar Run (emulador o movil).
4. **Escritorio**: `./gradlew :desktopApp:run`
5. **Web**: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
6. Tests de la logica: `./gradlew :shared:jvmTest`

Si Gradle da errores de version, alinea `gradle/libs.versions.toml` con el de tu plantilla de
la asignatura (Kotlin, Compose Multiplatform y AGP deben ser compatibles entre si).

## Configurar Firebase (para cuentas y sincronizacion)

1. En <https://console.firebase.google.com> crear un proyecto.
2. **Authentication** > Metodo de acceso > activar **Correo electronico/contraseña**.
3. **Firestore Database** > Crear base de datos (modo produccion) y en la pestaña **Reglas** pegar el
   contenido de `firestore.rules` y publicar.
4. **Configuracion del proyecto** > General > aplicacion web (`</>`): copiar la **clave API** y el
   **id del proyecto**.
5. Pegarlos en la app: *Ajustes* (o rellenar `ConfigPorDefecto` en
   `shared/.../model/Modelos.kt` para que vengan ya puestos).

No hace falta `google-services.json` ni el SDK de Firebase: la app usa la API REST, asi funciona igual
en las tres plataformas.

Flujo: el cuidador crea cuenta (rol Cuidador) y obtiene un codigo. El paciente crea su cuenta
(rol Paciente) y escribe ese codigo en *Cuenta > Vincular*.

## Asistente con IA (opcional)

```
cd backend
ANTHROPIC_API_KEY=... node servidor.mjs
```

En *Ajustes* de la app poner la URL del servidor (por ejemplo `http://192.168.1.20:8787`; en el emulador
Android, `http://10.0.2.2:8787`). Sin URL, el asistente responde con reglas locales (hora, fecha, agenda,
medicacion, familiares). En Android las URL `http` sin cifrar solo valen para pruebas locales.

## Compilacion automatica

`.github/workflows/build.yml` compila escritorio, Android (APK de depuracion) y web, y pasa los tests en
cada `push`. El APK queda como artefacto de la ejecucion en la pestaña *Actions*.

## Limitaciones conocidas

- Las fotos se reducen a ~360 px y se guardan dentro del documento de Firestore (limite de 1 MB):
  unas 20-25 fotos por paciente. En web solo se aceptan fotos de menos de 150 KB.
- La sincronizacion es por consulta periodica (10 s), no en tiempo real.
- Es un proyecto academico: no es un producto sanitario ni sustituye a un profesional.
