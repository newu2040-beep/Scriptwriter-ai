package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.ChatMessage
import com.example.data.ChatRepository
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val allMessages: StateFlow<List<ChatMessage>> = chatRepository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val apiKeyFlow = settingsRepository.preferredApiKey
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    val isDarkModeFlow = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun sendMessage(text: String, systemInstruction: String? = "You are a professional scriptwriter and dialogue assistant.") {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Add user message
            val userMsg = ChatMessage(text = text, role = "user")
            chatRepository.insert(userMsg)

            try {
                // Get API Key (DataStore first, fallback to BuildConfig)
                var apiKey = apiKeyFlow.value ?: BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty()) apiKey = BuildConfig.GEMINI_API_KEY // Safeguard

                if (apiKey.isEmpty()) {
                    _errorMessage.value = "API key is missing. Please set one in Settings."
                    _isLoading.value = false
                    return@launch
                }

                // Construct conversion history (exclude system instructions from here unless required, Gemini has a dedicated field for it)
                val history = chatRepository.allMessages.first()
                val apiContents = history.map { msg ->
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.text))
                    )
                }.toMutableList()
                
                // Add current message
                apiContents.add(Content(role = "user", parts = listOf(Part(text = text))))

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = systemInstruction?.let { Content(parts = listOf(Part(text = it))) }
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    val modelMsg = ChatMessage(text = responseText, role = "model")
                    chatRepository.insert(modelMsg)
                } else {
                    _errorMessage.value = "Failed to generate response or response was empty."
                }
            } catch (e: Exception) {
                if (e.message?.contains("429") == true) {
                    _errorMessage.value = "Quota limit reached. Please try again later or provide your own API key in Settings."
                } else {
                    _errorMessage.value = "Error: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatRepository.clear()
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    fun setApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsRepository.saveApiKey(apiKey.trim())
        }
    }
    
    fun clearApiKey() {
        viewModelScope.launch {
            settingsRepository.clearApiKey()
        }
    }
    
    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(isDark)
        }
    }
}

class ViewModelFactory(
    private val chatRepo: ChatRepository,
    private val settingsRepo: SettingsRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(chatRepo, settingsRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
