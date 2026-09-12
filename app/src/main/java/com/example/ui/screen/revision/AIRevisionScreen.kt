package com.example.ui.screen.revision

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiWorkflowRepository
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryEmeraldGreen
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.TextMutedSecondary
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.theme.TextWhitePrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class RevisionMessage(
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AIRevisionScreen(
    projectId: Long,
    aiWorkflowRepository: AiWorkflowRepository,
    onNavigateBack: () -> Unit
) {
    val messages = remember {
        mutableStateListOf(
            RevisionMessage(
                isUser = false,
                text = "Hello! I am your AI Timeline Editor Assistant. Tell me what changes you want to make (e.g., 'Make the music more upbeat', 'Shorten clips for faster pace', or 'Change text to Summer Memories')."
            )
        )
    }

    var inputMessage by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val quickSuggestions = listOf(
        "Change text overlay to 'Golden Hour Moments'",
        "Shorten clips for faster dynamic pace",
        "Replace music with upbeat rhythm track",
        "Add an extra cinematic B-roll scene"
    )

    fun sendRevision(prompt: String) {
        if (prompt.isBlank() || isSubmitting) return
        messages.add(RevisionMessage(isUser = true, text = prompt))
        inputMessage = ""
        isSubmitting = true

        scope.launch {
            delay(1200)
            val resultFeedback = aiWorkflowRepository.executeAiRevision(projectId, prompt)
            messages.add(RevisionMessage(isUser = false, text = "$resultFeedback\n\nTimeline has been updated."))
            isSubmitting = false
        }
    }

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MagistoryElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Revision Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhitePrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhitePrimary
                        )
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
        ) {
            // Chat history list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                    ) {
                        if (!msg.isUser) {
                            Surface(
                                shape = CircleShape,
                                color = MagistoryPrimaryPurple.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Top)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MagistoryElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (msg.isUser) MagistoryPrimaryPurple else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (msg.isUser) MagistoryPrimaryPurple else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth(fraction = 0.82f)
                        ) {
                            Text(
                                text = msg.text,
                                fontSize = 13.sp,
                                color = TextWhitePrimary,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (msg.isUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2B2446),
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Top)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextMutedSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSubmitting) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MagistoryElectricCyan,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Agent analyzing timeline and applying changes...",
                                fontSize = 12.sp,
                                color = MagistoryElectricCyan
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MagistoryElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Suggested Revisions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MagistoryElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickSuggestions.forEach { suggestion ->
                        Surface(
                            onClick = { sendRevision(suggestion) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF261F3C),
                            border = BorderStroke(0.5.dp, Color(0xFF3E335C))
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                color = TextWhitePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Chat input bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF13101E))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = { Text("Ask AI to revise timeline...", color = TextMutedSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MagistoryPrimaryPurple,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedTextColor = TextWhitePrimary,
                        unfocusedTextColor = TextWhitePrimary,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_revision_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = { sendRevision(inputMessage) },
                    enabled = inputMessage.isNotBlank() && !isSubmitting,
                    shape = CircleShape,
                    color = if (inputMessage.isNotBlank() && !isSubmitting) MagistoryPrimaryPurple else Color(0xFF28213D),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputMessage.isNotBlank() && !isSubmitting) Color.White else Color(0xFF5E547C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
