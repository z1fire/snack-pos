package io.github.z1fire.snackpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.em

class MainActivity : ComponentActivity() {
    private val vm: ShelfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(C.Page.toArgb(), C.Page.toArgb()),
            navigationBarStyle = SystemBarStyle.light(C.Page.toArgb(), C.Page.toArgb()),
        )
        setContent {
            // Text inherits bodyLarge; a relative line height keeps small labels as tight as the design's.
            MaterialTheme(typography = Typography(bodyLarge = TextStyle(fontFamily = Inter, lineHeight = 1.4.em, color = C.Ink))) {
                App(vm)
            }
        }
    }
}
