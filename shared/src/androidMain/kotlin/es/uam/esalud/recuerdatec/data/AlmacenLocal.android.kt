package es.uam.esalud.recuerdatec.data

import android.content.Context
import es.uam.esalud.recuerdatec.AppContext

actual fun crearAlmacenLocal(): AlmacenLocal = object : AlmacenLocal {
    private val prefs = AppContext.app.getSharedPreferences("recuerdatec", Context.MODE_PRIVATE)
    override fun leer(clave: String): String? = prefs.getString(clave, null)
    override fun escribir(clave: String, valor: String) {
        prefs.edit().putString(clave, valor).apply()
    }
    override fun borrar(clave: String) {
        prefs.edit().remove(clave).apply()
    }
}
