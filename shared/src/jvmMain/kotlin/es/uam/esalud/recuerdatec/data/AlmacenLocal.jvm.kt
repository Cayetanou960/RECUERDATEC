package es.uam.esalud.recuerdatec.data

import java.io.File

actual fun crearAlmacenLocal(): AlmacenLocal = object : AlmacenLocal {
    private val carpeta = File(System.getProperty("user.home"), ".recuerdatec").apply { mkdirs() }
    private fun archivo(clave: String) = File(carpeta, clave.filter { it.isLetterOrDigit() || it == '_' } + ".json")
    override fun leer(clave: String): String? = archivo(clave).takeIf { it.exists() }?.readText()
    override fun escribir(clave: String, valor: String) {
        archivo(clave).writeText(valor)
    }
    override fun borrar(clave: String) {
        archivo(clave).delete()
    }
}
