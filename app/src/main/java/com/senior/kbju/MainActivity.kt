package com.senior.kbju

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.senior.kbju.ui.navigation.AppRoot
import com.senior.kbju.ui.theme.SeniorKbjuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeniorKbjuTheme {
                AppRoot()
            }
        }
    }
}
