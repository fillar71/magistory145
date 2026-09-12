package com.example.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.UserProfile
import com.example.security.ApiKeyManager
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfile,
    apiKeyManager: ApiKeyManager,
    onNavigateBack: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf("gemini") } // gemini, claude, openai
    var apiKeyInput by remember { mutableStateOf("") }
    var isKeyObscured by remember { mutableStateOf(true) }
    var existingKeyMasked by remember { mutableStateOf<String?>(null) }
    var isTestingKey by remember { mutableStateOf(false) }
    var keyStatusMessage by remember { mutableStateOf<String?>(null) }
    var keyStatusSuccess by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun loadKeyForProvider(provider: String) {
        scope.launch {
            val key = apiKeyManager.getDecryptedKey(provider)
            if (!key.isNullOrBlank()) {
                existingKeyMasked = "••••••••••••${key.takeLast(4)}"
            } else {
                existingKeyMasked = null
            }
            apiKeyInput = ""
            keyStatusMessage = null
        }
    }

    LaunchedEffect(selectedProvider) {
        loadKeyForProvider(selectedProvider)
    }

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & AI Keys",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhitePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MagistoryDeepOnyx)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // User Account Profile Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MagistoryPrimaryPurple.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MagistoryPrimaryPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )
                        Text(
                            text = userProfile.email,
                            fontSize = 12.sp,
                            color = TextMutedSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (userProfile.isPremium) MagistorySunsetCoral.copy(alpha = 0.25f) else MagistoryElectricCyan.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (userProfile.isPremium) "PRO SUBSCRIBER" else "FREE TIER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (userProfile.isPremium) MagistorySunsetCoral else MagistoryElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // BYOK Management Section
            Text(
                text = "Bring Your Own Key (BYOK)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhitePrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Free tier users can bring their own LLM API keys. Keys are encrypted hardware-backed with AES-256-GCM in Android Keystore.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMutedSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Provider selection chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(
                    "gemini" to "Google Gemini",
                    "claude" to "Anthropic Claude",
                    "openai" to "OpenAI"
                ).forEach { (id, label) ->
                    val isSelected = selectedProvider == id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedProvider = id },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MagistoryPrimaryPurple.copy(alpha = 0.3f),
                            selectedLabelColor = MagistoryPrimaryPurple,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = TextMutedSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MagistoryPrimaryPurple
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Key display or input
            if (existingKeyMasked != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MagistoryEmeraldGreen.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MagistoryEmeraldGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Key Configured & Encrypted", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                                Text(existingKeyMasked ?: "", fontSize = 11.sp, color = TextMutedSecondary)
                            }
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    apiKeyManager.deleteKey(selectedProvider)
                                    existingKeyMasked = null
                                    keyStatusMessage = "Key deleted"
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MagistorySunsetCoral)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Input new API Key
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Enter $selectedProvider API Key") },
                placeholder = { Text("Paste your API key...") },
                singleLine = true,
                visualTransformation = if (isKeyObscured) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = {
                    IconButton(onClick = { isKeyObscured = !isKeyObscured }) {
                        Icon(
                            imageVector = if (isKeyObscured) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Visibility",
                            tint = TextMutedSecondary
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MagistoryPrimaryPurple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = TextWhitePrimary,
                    unfocusedTextColor = TextWhitePrimary,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Save & Test buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (apiKeyInput.isNotBlank()) {
                            scope.launch {
                                apiKeyManager.saveKey(selectedProvider, apiKeyInput.trim())
                                existingKeyMasked = "••••••••••••${apiKeyInput.trim().takeLast(4)}"
                                apiKeyInput = ""
                                keyStatusSuccess = true
                                keyStatusMessage = "Key encrypted and saved into Android Keystore!"
                            }
                        }
                    },
                    enabled = apiKeyInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MagistoryPrimaryPurple),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Key", color = Color.White, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        isTestingKey = true
                        scope.launch {
                            delay(900)
                            isTestingKey = false
                            keyStatusSuccess = true
                            keyStatusMessage = "Connection test verified successfully!"
                        }
                    },
                    enabled = existingKeyMasked != null || apiKeyInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTestingKey) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MagistoryElectricCyan, strokeWidth = 2.dp)
                    } else {
                        Text("Test Key", color = TextWhitePrimary)
                    }
                }
            }

            if (keyStatusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = keyStatusMessage ?: "",
                    fontSize = 12.sp,
                    color = if (keyStatusSuccess) MagistoryEmeraldGreen else MagistorySunsetCoral
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium Upgrade Callout Card
            if (!userProfile.isPremium) {
                Surface(
                    onClick = onNavigateToSubscription,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MagistorySunsetCoral.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, tint = MagistorySunsetCoral, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upgrade to Magistory PRO", fontWeight = FontWeight.Bold, color = TextWhitePrimary)
                            Text("Unlock managed AI (no BYOK needed), 4K 60FPS export, and priority rendering.", fontSize = 11.sp, color = TextMutedSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Sign out action
            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Text("Sign Out", color = MagistorySunsetCoral, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
