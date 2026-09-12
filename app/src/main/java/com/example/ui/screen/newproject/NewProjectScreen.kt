package com.example.ui.screen.newproject

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop32
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MagistoryDeepOnyx
import com.example.ui.theme.MagistoryElectricCyan
import com.example.ui.theme.MagistoryPrimaryPurple
import com.example.ui.theme.MagistorySunsetCoral
import com.example.ui.theme.TextMutedSecondary
import com.example.ui.theme.TextWhitePrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewProjectScreen(
    onNavigateBack: () -> Unit,
    onStartAiGeneration: (prompt: String, aspectRatio: String, resolution: String) -> Unit,
    onCreateBlankProject: (title: String, aspectRatio: String, resolution: String) -> Unit,
    isPremiumUser: Boolean
) {
    var selectedMode by remember { mutableStateOf(0) } // 0 = AI-Assisted, 1 = Manual Blank
    var promptText by remember { mutableStateOf("Aesthetic morning routine with pour-over coffee, sunlit kitchen, cozy lo-fi beat") }
    var projectTitle by remember { mutableStateOf("My Video Project") }
    var selectedAspectRatio by remember { mutableStateOf("16:9") } // "16:9", "9:16", "1:1"
    var selectedResolution by remember { mutableStateOf("1080p") } // "1080p", "4K"

    val presetPrompts = listOf(
        "☕ Morning Pour-Over Coffee Aesthetic Vlog",
        "🌆 Cyberpunk Neon Tokyo Rain Teaser 4K",
        "🍝 Artisan Garlic Butter Pasta Cooking Reel",
        "🎧 Minimalist Matte Black Tech Review",
        "🏖️ Coastal Sunset Drone Cinematic Edit"
    )

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Video Project",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhitePrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Mode Selector Tabs
            TabRow(
                selectedTabIndex = selectedMode,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedMode]),
                        color = MagistoryPrimaryPurple
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedMode == 0) MagistoryPrimaryPurple else TextMutedSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "AI Autonomous Agent",
                                fontWeight = if (selectedMode == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMode == 0) TextWhitePrimary else TextMutedSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_ai_mode")
                )
                Tab(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedMode == 1) MagistoryPrimaryPurple else TextMutedSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Manual Blank",
                                fontWeight = if (selectedMode == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedMode == 1) TextWhitePrimary else TextMutedSecondary
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_manual_mode")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (selectedMode == 0) {
                // AI Prompt Input
                Text(
                    text = "Describe your video concept",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhitePrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The AI agent will generate the script, search royalty-free video/audio, and organize multi-track timeline clips.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMutedSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("e.g., Tokyo night walk with neon street reflections and lo-fi synth music...", color = TextMutedSecondary) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(16.dp),
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
                        .testTag("ai_prompt_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Suggestion chips
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = MagistoryElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Inspiration Presets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MagistoryElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetPrompts.forEach { preset ->
                        Surface(
                            onClick = { promptText = preset.drop(3) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 12.sp,
                                color = TextWhitePrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            } else {
                // Manual Blank Title Input
                Text(
                    text = "Project Name",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhitePrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = projectTitle,
                    onValueChange = { projectTitle = it },
                    placeholder = { Text("Enter project title...", color = TextMutedSecondary) },
                    singleLine = true,
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
                        .testTag("manual_title_input")
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Aspect Ratio Selector
            Text(
                text = "Aspect Ratio",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhitePrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AspectRatioOption(
                    ratio = "16:9",
                    label = "Widescreen",
                    sub = "YouTube",
                    icon = Icons.Default.Crop169,
                    isSelected = selectedAspectRatio == "16:9",
                    onClick = { selectedAspectRatio = "16:9" },
                    modifier = Modifier.weight(1f)
                )
                AspectRatioOption(
                    ratio = "9:16",
                    label = "Vertical",
                    sub = "Shorts / Reels",
                    icon = Icons.Default.CropPortrait,
                    isSelected = selectedAspectRatio == "9:16",
                    onClick = { selectedAspectRatio = "9:16" },
                    modifier = Modifier.weight(1f)
                )
                AspectRatioOption(
                    ratio = "1:1",
                    label = "Square",
                    sub = "Social Feed",
                    icon = Icons.Default.CropSquare,
                    isSelected = selectedAspectRatio == "1:1",
                    onClick = { selectedAspectRatio = "1:1" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resolution Selector (Free 1080p vs Premium 4K)
            Text(
                text = "Target Resolution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhitePrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ResolutionOption(
                    res = "1080p",
                    title = "1080p Full HD",
                    desc = "Standard • Fast client render",
                    isPremiumOnly = false,
                    isSelected = selectedResolution == "1080p",
                    onClick = { selectedResolution = "1080p" },
                    modifier = Modifier.weight(1f)
                )

                ResolutionOption(
                    res = "4K",
                    title = "4K Ultra HD",
                    desc = "60 FPS • MediaCodec HDR",
                    isPremiumOnly = !isPremiumUser,
                    isSelected = selectedResolution == "4K",
                    onClick = { selectedResolution = "4K" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Submit Button
            Button(
                onClick = {
                    if (selectedMode == 0) {
                        val validPrompt = if (promptText.isNotBlank()) promptText else "Aesthetic Morning Routine"
                        onStartAiGeneration(validPrompt, selectedAspectRatio, selectedResolution)
                    } else {
                        val validTitle = if (projectTitle.isNotBlank()) projectTitle else "Untitled Video"
                        onCreateBlankProject(validTitle, selectedAspectRatio, selectedResolution)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MagistoryPrimaryPurple
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("create_project_action_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (selectedMode == 0) Icons.Default.AutoAwesome else Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (selectedMode == 0) "Generate Autonomous Timeline" else "Create Blank Timeline",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioOption(
    ratio: String,
    label: String,
    sub: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MagistoryPrimaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MagistoryPrimaryPurple else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = ratio,
                tint = if (isSelected) MagistoryPrimaryPurple else TextMutedSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ratio,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextWhitePrimary
            )
            Text(
                text = sub,
                fontSize = 10.sp,
                color = TextMutedSecondary
            )
        }
    }
}

@Composable
private fun ResolutionOption(
    res: String,
    title: String,
    desc: String,
    isPremiumOnly: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MagistoryPrimaryPurple.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MagistoryPrimaryPurple else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextWhitePrimary
                )
                if (isPremiumOnly) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MagistorySunsetCoral.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = "PRO",
                            color = MagistorySunsetCoral,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextMutedSecondary
            )
        }
    }
}
