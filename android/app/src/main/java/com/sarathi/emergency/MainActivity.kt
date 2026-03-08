package com.sarathi.emergency

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sarathi.emergency.ui.navigation.NavGraph
import com.sarathi.emergency.ui.theme.DarkNavy
import com.sarathi.emergency.ui.theme.SarathiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SarathiApp

        setContent {
            SarathiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkNavy
                ) {
                    NavGraph(
                        api = app.api,
                        sessionManager = app.sessionManager
                    )
                }
            }
        }
    }
}
