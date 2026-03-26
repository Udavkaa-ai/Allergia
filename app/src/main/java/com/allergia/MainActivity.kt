package com.allergia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.allergia.ui.AllergiaNavigation
import com.allergia.ui.theme.AllergiaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AllergiaTheme {
                AllergiaNavigation()
            }
        }
    }
}
