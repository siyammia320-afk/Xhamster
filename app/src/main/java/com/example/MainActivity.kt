package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProxyApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ProxyApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("ProxyPrefs", Context.MODE_PRIVATE)

    var server by remember { mutableStateOf(sharedPrefs.getString("SERVER", "") ?: "") }
    var port by remember { mutableStateOf(sharedPrefs.getString("PORT", "") ?: "") }
    var username by remember { mutableStateOf(sharedPrefs.getString("USERNAME", "") ?: "") }
    var password by remember { mutableStateOf(sharedPrefs.getString("PASSWORD", "") ?: "") }
    var isConnected by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpnService(context, server, port, username, password)
            isConnected = true
            Toast.makeText(context, "Proxy Connected", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Optionally check for permission on launch
    LaunchedEffect(Unit) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            // Un-comment to ask immediately on app launch instead of waiting for Connect click
            // vpnLauncher.launch(intent)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Proxy Connect",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = server,
            onValueChange = { server = it },
            label = { Text("Server IP / Hostname") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                sharedPrefs.edit().apply {
                    putString("SERVER", server)
                    putString("PORT", port)
                    putString("USERNAME", username)
                    putString("PASSWORD", password)
                    apply()
                }
                Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Configuration")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isConnected) {
                Button(
                    onClick = {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            // Permission is needed
                            vpnLauncher.launch(intent)
                        } else {
                            // Permission already granted
                            startVpnService(context, server, port, username, password)
                            isConnected = true
                            Toast.makeText(context, "Proxy Connected", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect")
                }
            } else {
                Button(
                    onClick = {
                        stopVpnService(context)
                        isConnected = false
                        Toast.makeText(context, "Proxy Disconnected", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Off")
                }
            }
        }
    }
}

fun startVpnService(context: Context, server: String, port: String, user: String, pass: String) {
    val intent = Intent(context, ProxyVpnService::class.java).apply {
        putExtra("SERVER", server)
        putExtra("PORT", port)
        putExtra("USERNAME", user)
        putExtra("PASSWORD", pass)
    }
    context.startService(intent)
}

fun stopVpnService(context: Context) {
    val intent = Intent(context, ProxyVpnService::class.java).apply {
        action = "DISCONNECT"
    }
    context.startService(intent)
}
