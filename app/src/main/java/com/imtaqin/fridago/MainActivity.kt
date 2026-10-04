package com.imtaqin.fridago

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.imtaqin.fridago.ui.CorvoRoot
import com.imtaqin.fridago.ui.PermissionGate
import com.imtaqin.fridago.ui.theme.CorvoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CorvoTheme {
                PermissionGate {
                    CorvoRoot()
                }
            }
        }
    }
}
