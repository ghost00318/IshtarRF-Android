package com.ishtarrf.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ishtarrf.R
import com.ishtarrf.data.serial.ConnectionState
import com.ishtarrf.ui.components.StatusDot
import com.ishtarrf.ui.screens.ControlScreen
import com.ishtarrf.ui.screens.LibraryScreen
import com.ishtarrf.ui.screens.SettingsScreen
import com.ishtarrf.ui.screens.ToolsScreen
import com.ishtarrf.ui.screens.WaveformScreen

private data class Tab(val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("Control", Icons.Filled.Tune),
        Tab("Wave", Icons.AutoMirrored.Filled.ShowChart),
        Tab("Library", Icons.Filled.FolderOpen),
        Tab("Tools", Icons.Filled.Bolt),
        Tab("Settings", Icons.Outlined.Settings),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = "IshtarRF",
                            modifier = Modifier.size(34.dp).padding(end = 10.dp),
                        )
                        Column {
                            Text(
                                "IshtarRF",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            ConnectionLabel(state.connection)
                        }
                    }
                },
                actions = { ConnectButton(state.connection, state.deviceAttached, viewModel::toggleConnect) },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, style = MaterialTheme.typography.labelSmall) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> ControlScreen(state, viewModel)
                1 -> WaveformScreen(state, viewModel)
                2 -> LibraryScreen(state, viewModel)
                3 -> ToolsScreen(state, viewModel)
                else -> SettingsScreen(state, viewModel)
            }
        }
    }
}

@Composable
private fun ConnectionLabel(connection: ConnectionState) {
    val (color, text) = when (connection) {
        is ConnectionState.Connected -> MaterialTheme.colorScheme.secondary to "Connected · ${connection.deviceName}"
        ConnectionState.Connecting -> MaterialTheme.colorScheme.primary to "Connecting…"
        is ConnectionState.Error -> MaterialTheme.colorScheme.error to "Error"
        ConnectionState.Disconnected -> Color(0xFF8A8A8A) to "Disconnected"
    }
    StatusDot(color = color, label = text)
}

@Composable
private fun ConnectButton(
    connection: ConnectionState,
    deviceAttached: Boolean,
    onToggle: () -> Unit,
) {
    val connected = connection.isConnected
    Button(
        onClick = onToggle,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        colors = if (connected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Icon(
            if (connected) Icons.Filled.LinkOff
            else if (deviceAttached) Icons.Filled.SettingsInputAntenna else Icons.Filled.Usb,
            contentDescription = null,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(if (connected) "Disconnect" else "Connect")
    }
}
