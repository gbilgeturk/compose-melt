package dev.melt.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.melt.MeltConfig
import dev.melt.MeltRepeat
import dev.melt.MeltSpeed
import dev.melt.MeltStyle
import dev.melt.melt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SampleScreen()
            }
        }
    }
}

private val styles = listOf(
    "Drip" to MeltStyle.Drip,
    "Wax" to MeltStyle.Wax,
    "Goo" to MeltStyle.Goo,
    "Slump" to MeltStyle.Slump,
    "Wavy" to MeltStyle.Wavy,
)

private val speeds = listOf(
    "Slow" to MeltSpeed.Slow,
    "Normal" to MeltSpeed.Normal,
    "Fast" to MeltSpeed.Fast,
    "Very fast" to MeltSpeed.VeryFast,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SampleScreen() {
    var styleIndex by remember { mutableIntStateOf(0) }
    var speedIndex by remember { mutableIntStateOf(1) }
    var replay by remember { mutableIntStateOf(0) }
    val style = styles[styleIndex].second
    val speed = speeds[speedIndex].second

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("compose-melt", style = MaterialTheme.typography.headlineMedium)

            Text("Style", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                styles.forEachIndexed { i, (name, _) ->
                    FilterChip(
                        selected = i == styleIndex,
                        onClick = { styleIndex = i; replay++ },
                        label = { Text(name) },
                    )
                }
            }

            Text("Speed", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                speeds.forEachIndexed { i, (name, _) ->
                    FilterChip(
                        selected = i == speedIndex,
                        onClick = { speedIndex = i; replay++ },
                        label = { Text(name) },
                    )
                }
            }

            OutlinedButton(onClick = { replay++ }) { Text("Replay") }

            // 1. Melts as soon as it appears. key() recreates it so "Replay" starts over.
            key(replay) {
                Button(
                    onClick = {},
                    modifier = Modifier.melt(style = style, speed = speed),
                ) { Text("Modifier.melt()") }
            }

            Gap(110)

            // 2. Melt on demand.
            var melted by remember { mutableStateOf(false) }
            Button(
                onClick = { melted = !melted },
                modifier = Modifier.melt(style = style, speed = speed, enabled = melted),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
            ) { Text(if (melted) "Tap to restore" else "Tap to melt") }

            Gap(110)

            // 3. A card that starts melting by itself after a short delay.
            key(replay) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .melt(style = style, speed = speed, config = MeltConfig(delayMillis = 600, seed = 3)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7E57C2)),
                ) {
                    Text(
                        "Melting card",
                        modifier = Modifier.padding(24.dp),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Gap(130)

            // 4. Gradient pills looping forever, with different seeds.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                listOf(1, 2).forEach { seed ->
                    Box(
                        modifier = Modifier
                            .melt(
                                style = style,
                                speed = speed,
                                config = MeltConfig(repeat = MeltRepeat.Reverse, seed = seed),
                            )
                            .background(
                                brush = Brush.horizontalGradient(listOf(Color(0xFFFF8F00), Color(0xFFFF3D7F))),
                                shape = RoundedCornerShape(50),
                            )
                            .padding(horizontal = 28.dp, vertical = 16.dp),
                    ) {
                        Text("Looping", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Gap(130)

            // 5. Works on any composable: a FAB, plain Text, an icon-like shape.
            key(replay) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FloatingActionButton(
                        onClick = {},
                        modifier = Modifier.melt(style = style, speed = speed, config = MeltConfig(seed = 7)),
                        shape = CircleShape,
                        containerColor = Color(0xFF7C4DFF),
                        contentColor = Color.White,
                    ) { Text("+", fontSize = 28.sp) }

                    Text(
                        "MELT",
                        modifier = Modifier.melt(style = style, speed = speed, config = MeltConfig(seed = 8)),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFAA00),
                    )
                }
            }

            Gap(140)
        }
    }
}

@Composable
private fun Gap(heightDp: Int) {
    Spacer(Modifier.height(heightDp.dp))
}
