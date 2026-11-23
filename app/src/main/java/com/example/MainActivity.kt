package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.HearthRepository
import com.example.data.PreferencesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.HearthHomeScreen
import com.example.ui.theme.HearthframeTheme
import com.example.ui.viewmodel.HearthViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      HearthframeTheme {
        AppContent()
      }
    }
  }
}

@Composable
fun AppContent() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth -> currentUser = auth.currentUser }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthSuccess = { currentUser = Firebase.auth.currentUser }
    )
  } else {
    val uid = user.uid
    val displayName = user.displayName?.takeIf { it.isNotBlank() }
      ?: user.email?.substringBefore("@")
      ?: "Family Member"

    key(uid) {
      val viewModel: HearthViewModel =
        viewModel(
          key = uid,
          factory =
            viewModelFactory {
              initializer {
                val app =
                  checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                  }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = HearthRepository(db)
                val prefs = PreferencesRepository(app.applicationContext)
                HearthViewModel(
                  repository = repository,
                  preferencesRepository = prefs,
                  currentUserId = uid,
                  currentUserName = displayName,
                )
              }
            },
        )

      HearthHomeScreen(
        viewModel = viewModel,
        currentUserId = uid,
        currentUserName = displayName,
        currentUserEmail = user.email,
      )
    }
  }
}
