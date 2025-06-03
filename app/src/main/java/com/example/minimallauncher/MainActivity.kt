package com.example.minimallauncher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.minimallauncher.ui.theme.MinimalLauncherTheme
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Forcer la barre de status/navigation en noir
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.isAppearanceLightStatusBars = false // texte clair
            controller.isAppearanceLightNavigationBars = false // texte clair
        }
        setContent {
            MinimalLauncherTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LauncherHome()
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LauncherHome() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)
    val shortcutKeys = listOf("shortcut1", "shortcut2", "shortcut3")
    var shortcutPackages by remember { mutableStateOf(shortcutKeys.map { prefs.getString(it, null) }) }
    var shortcutLabels by remember { mutableStateOf(shortcutKeys.map { getAppLabel(context, prefs.getString(it, null)) }) }
    var pendingShortcutIndex by remember { mutableStateOf<Int?>(null) }
    var showAppDialog by remember { mutableStateOf(false) }
    var appList by remember { mutableStateOf(listOf<AppInfo>()) }
    var showAllAppsDialog by remember { mutableStateOf(false) }
    var allAppsList by remember { mutableStateOf(listOf<AppInfo>()) }
    var showCountdown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(10) }
    var countdownActive by remember { mutableStateOf(false) }

    if (countdownActive) {
        LaunchedEffect(countdownActive) {
            for (i in 10 downTo 1) {
                countdownValue = i
                kotlinx.coroutines.delay(1000)
            }
            showCountdown = false
            countdownActive = false
        }
    }

    fun loadApps() {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val apps = resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label.lowercase() }
        appList = apps
    }

    fun loadAllApps() {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val apps = resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName
            )
        }.sortedBy { it.label.lowercase() }
        allAppsList = apps
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            shortcutKeys.forEachIndexed { i, key ->
                val label = shortcutLabels[i] ?: "Raccourci ${i + 1}"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .combinedClickable(
                            onClick = {
                                val pkg = shortcutPackages[i]
                                if (pkg == null) {
                                    pendingShortcutIndex = i
                                    loadApps()
                                    showAppDialog = true
                                } else {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    }
                                }
                            },
                            onLongClick = {
                                pendingShortcutIndex = i
                                loadApps()
                                showAppDialog = true
                            }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = label, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {
                                val down = awaitPointerEvent().changes.firstOrNull { it.pressed }
                                if (down != null) {
                                    showCountdown = true
                                    countdownActive = true
                                    val start = System.currentTimeMillis()
                                    var stillPressed = true
                                    while (stillPressed && (System.currentTimeMillis() - start) < 10000) {
                                        val event = awaitPointerEvent()
                                        stillPressed = event.changes.all { it.pressed }
                                    }
                                    showCountdown = false
                                    countdownActive = false
                                    if (stillPressed && (System.currentTimeMillis() - start) >= 10000) {
                                        loadAllApps()
                                        showAllAppsDialog = true
                                    }
                                }
                            }
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        if (showCountdown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                CountdownOverlay(countdownValue)
            }
        }
    }

    if (showAppDialog && pendingShortcutIndex != null) {
        AlertDialog(
            onDismissRequest = { showAppDialog = false },
            title = { Text("Choisir une application", color = Color.White) },
            containerColor = Color.Black,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(appList) { app ->
                        TextButton(
                            onClick = {
                                val editor = prefs.edit()
                                editor.putString(shortcutKeys[pendingShortcutIndex!!], app.packageName)
                                editor.apply()
                                shortcutPackages = shortcutKeys.map { prefs.getString(it, null) }
                                shortcutLabels = shortcutKeys.map { getAppLabel(context, prefs.getString(it, null)) }
                                showAppDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                app.label,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppDialog = false }) {
                    Text("Annuler", color = Color.White)
                }
            },
        )
    }

    if (showAllAppsDialog) {
        AlertDialog(
            onDismissRequest = { showAllAppsDialog = false },
            title = { Text("Toutes les applications", color = Color.White) },
            containerColor = Color.Black,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(allAppsList) { app ->
                        TextButton(
                            onClick = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launchIntent != null) {
                                    context.startActivity(launchIntent)
                                    showAllAppsDialog = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                app.label,
                                color = Color.White,
                                fontSize = 18.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAllAppsDialog = false }) {
                    Text("Fermer", color = Color.White)
                }
            },
        )
    }
}

data class AppInfo(val label: String, val packageName: String)

fun getAppLabel(context: Context, packageName: String?): String? {
    if (packageName == null) return null
    return try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(appInfo).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

@Composable
fun CountdownOverlay(count: Int) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val sweep = (count / 10f) * 360f
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "$count",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}