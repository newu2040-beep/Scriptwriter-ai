package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ChatRepository
import com.example.data.SettingsRepository
import com.example.ui.ChatViewModel
import com.example.ui.ScriptwriterApp
import com.example.ui.ViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    val database = AppDatabase.getDatabase(this)
    val chatRepository = ChatRepository(database.chatDao())
    val settingsRepository = SettingsRepository(this)
    val factory = ViewModelFactory(chatRepository, settingsRepository)
    
    setContent {
      val viewModel: ChatViewModel = viewModel(factory = factory)
      val forceDarkMode by viewModel.isDarkModeFlow.collectAsState()
      
      val isDarkTheme = forceDarkMode ?: isSystemInDarkTheme()
      
      MyApplicationTheme(darkTheme = isDarkTheme) {
        ScriptwriterApp(viewModel = viewModel)
      }
    }
  }
}

