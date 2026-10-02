package com.jose.calendario

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.init(applicationContext)
        setContent {
            val tema = temaDe(Store.usuario.tema)
            CompositionLocalProvider(LocalTema provides tema) {
                MaterialTheme(colorScheme = tema.scheme) {
                    Aplicacion()
                }
            }
        }
    }
}
