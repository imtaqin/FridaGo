package com.imtaqin.corvo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.imtaqin.corvo.ui.CorvoRoot
import com.imtaqin.corvo.ui.PermissionGate
import com.imtaqin.corvo.ui.theme.CorvoTheme

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
