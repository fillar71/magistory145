package com.example.ui.screen.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SubscriptionScreen(
    isCurrentlyPremium: Boolean,
    onNavigateBack: () -> Unit,
    onUpgradeSuccess: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf("YEARLY") } // "MONTHLY" vs "YEARLY"
    var isPurchasing by remember { mutableStateOf(false) }
    var purchaseComplete by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = MagistoryDeepOnyx,
        topBar = {
            TopAppBar(
                title = {
                    Text("Magistory PRO", fontWeight = FontWeight.Bold, color = TextWhitePrimary)
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
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MagistorySunsetCoral.copy(alpha = 0.25f),
                                Color(0xFF1E142E)
                            )
                        )
                    )
                    .border(1.5.dp, MagistorySunsetCoral.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MagistorySunsetCoral,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Unlock Ultra HD & Managed AI",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = TextWhitePrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Elevate your video editing with 4K 60FPS export, zero-config Cloudflare AI, and unlimited tracks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pricing Cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                // Monthly Plan
                PlanCard(
                    title = "Monthly",
                    price = "$9.99",
                    period = "/ month",
                    subtitle = "Billed monthly",
                    badge = null,
                    isSelected = selectedPlan == "MONTHLY",
                    onClick = { selectedPlan = "MONTHLY" },
                    modifier = Modifier.weight(1f)
                )

                // Yearly Plan (Best Value)
                PlanCard(
                    title = "Yearly",
                    price = "$79.99",
                    period = "/ year",
                    subtitle = "$6.66/mo • Save 33%",
                    badge = "BEST VALUE",
                    isSelected = selectedPlan == "YEARLY",
                    onClick = { selectedPlan = "YEARLY" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Feature Comparison Table
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Feature Comparison", fontWeight = FontWeight.Bold, color = TextWhitePrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    FeatureRow("Autonomous Timeline Agent", "Requires BYOK Key", "Included (No Key Required)")
                    FeatureRow("Export Resolution", "1080p FHD Max", "4K Ultra HD (60 FPS)")
                    FeatureRow("Hardware MediaCodec", "Standard Speed", "Priority High-Throughput")
                    FeatureRow("Timeline Tracks", "3 Tracks Max", "Unlimited Multi-Tracks")
                    FeatureRow("Watermark", "None", "None")
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Upgrade Button
            if (isCurrentlyPremium || purchaseComplete) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MagistoryEmeraldGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, MagistoryEmeraldGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MagistoryEmeraldGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("You are an active PRO member!", fontWeight = FontWeight.Bold, color = MagistoryEmeraldGreen)
                    }
                }
            } else {
                Button(
                    onClick = {
                        isPurchasing = true
                        scope.launch {
                            delay(1200) // Simulate Google Play Billing
                            isPurchasing = false
                            purchaseComplete = true
                            onUpgradeSuccess()
                        }
                    },
                    enabled = !isPurchasing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MagistorySunsetCoral),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("subscribe_button")
                ) {
                    if (isPurchasing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (selectedPlan == "YEARLY") "Upgrade Yearly - $79.99" else "Upgrade Monthly - $9.99",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    title: String,
    price: String,
    period: String,
    subtitle: String,
    badge: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MagistorySunsetCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            2.dp,
            if (isSelected) MagistorySunsetCoral else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MagistorySunsetCoral
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Text(title, fontWeight = FontWeight.Bold, color = TextWhitePrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price, fontWeight = FontWeight.Black, color = TextWhitePrimary, fontSize = 22.sp)
                Text(period, fontSize = 11.sp, color = TextMutedSecondary, modifier = Modifier.padding(bottom = 2.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 10.sp, color = MagistoryElectricCyan)
        }
    }
}

@Composable
private fun FeatureRow(feature: String, free: String, pro: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(feature, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextWhitePrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Free: $free", fontSize = 11.sp, color = TextMutedSecondary)
            Text("PRO: $pro", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MagistorySunsetCoral)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(MaterialTheme.colorScheme.outline))
    }
}
