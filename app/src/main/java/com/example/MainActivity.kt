package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.audio.SoundSynthesizer
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.ui.game.GameScreen
import com.example.ui.game.GameViewModel
import com.example.ui.game.GameViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize local persistent database components
        val database = GameDatabase.getDatabase(this)
        val dao = database.gameDao()
        val repository = GameRepository(dao)

        // 2. Initialize sound effects synthesis logic
        val synth = SoundSynthesizer()

        // 3. Inject repositories via custom ViewModel Provider Factory
        val factory = GameViewModelFactory(repository, synth)
        val viewModel = ViewModelProvider(this, factory)[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
