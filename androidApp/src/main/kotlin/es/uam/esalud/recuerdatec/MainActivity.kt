package es.uam.esalud.recuerdatec

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.app = applicationContext
        // FileKit necesita la actividad para abrir el selector de fotos.
        FileKit.init(this)
        setContent { App() }
    }
}
