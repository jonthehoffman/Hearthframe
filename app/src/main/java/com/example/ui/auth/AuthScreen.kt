package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R

@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
      ) {
        // Brand Badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier =
              Modifier.size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Filled.Tv,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
          )
        }

        // Hero Banner Artwork with warm gradient scrim
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(16f / 9f)
              .clip(MaterialTheme.shapes.large)
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_family_hearth_banner),
            contentDescription = stringResource(R.string.cd_hero_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.55f),
                      )
                  )
                )
          )
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.94f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
          ) {
            Text(
              text = "Shared November Cloud Frame",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        }

        // Editorial Title & Description
        Text(
          text = stringResource(R.string.auth_headline),
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = stringResource(R.string.auth_subhead),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Three Key Benefits
        Card(
          colors =
            CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
          shape = MaterialTheme.shapes.medium,
        ) {
          Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            BenefitRow(
              icon = Icons.Filled.CloudSync,
              title = "Zero-Touch Cloud Sync",
              body = "Everyone drops up to 20 photos at once straight into the family album.",
            )
            BenefitRow(
              icon = Icons.Filled.AutoAwesomeMotion,
              title = "Auto-Sorted by Day & Moment",
              body = "Automatically grouped by gathering date, contributor, and moment.",
            )
            BenefitRow(
              icon = Icons.Filled.Tv,
              title = "Live Living-Room TV Display",
              body = "Launch full-screen TV Slideshow mode—new family shots appear live.",
            )
          }
        }

        if (errorMessage != null) {
          Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              text = errorMessage ?: "",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.fillMaxWidth().padding(14.dp),
            )
          }
        }

        GoogleSignInButton(
          onAuthSuccess = {
            errorMessage = null
            onAuthSuccess()
          },
          onAuthError = { msg -> errorMessage = msg },
        )

        Spacer(Modifier.height(8.dp))
      }
    }
  }
}

@Composable
private fun BenefitRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primaryContainer,
      shape = CircleShape,
      modifier = Modifier.size(38.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
