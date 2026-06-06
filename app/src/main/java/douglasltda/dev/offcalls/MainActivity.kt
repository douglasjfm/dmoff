package douglasltda.dev.offcalls

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CallLog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import douglasltda.dev.offcalls.ui.theme.OffCallsTheme

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("off_calls_prefs", Context.MODE_PRIVATE) }

    private var currentScreen by mutableStateOf("main")

    // --- Permission Launchers ---

    // Launcher for READ_CONTACTS (needed to filter calls)
    private val requestContactsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Once contacts granted, we usually need the Role as well
            checkAndRequestRole()
        } else {
            Toast.makeText(this, "Permissão necessária para acessar contatos e identificar bloqueios.", Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for READ_CALL_LOG (needed for History screen)
    private val requestCallLogLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentScreen = "history"
        } else {
            Toast.makeText(this, "Permissão negada. Não é possível exibir o histórico.", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for the Default Call Screening Role
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            prefs.edit().putBoolean("blocker_enabled", true).apply()
            Toast.makeText(this, "Bloqueador ativo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OffCallsTheme {
                MainAppContainer()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainAppContainer() {
        var showMenu by remember { mutableStateOf(false) }

        BackHandler(enabled = currentScreen == "history") {
            currentScreen = "main"
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Deixe-me OFF") },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Histórico de bloqueios") },
                                onClick = {
                                    showMenu = false
                                    handleHistoryNavigation()
                                }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (currentScreen == "main") {
                    MainScreen()
                } else {
                    HistoryScreen()
                }
            }
        }
    }

    @Composable
    fun MainScreen() {
        // Use derivedState to keep UI synced with SharedPreferences
        var isBlockerEnabled by remember {
            mutableStateOf(prefs.getBoolean("blocker_enabled", false))
        }
        val iCounter = prefs.getInt("blocker_counter", 0)

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Deixe-me OFF", style = MaterialTheme.typography.headlineMedium)
            // Display Version
            Text(
                text = "Versão: ${getAppVersionName()}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = if (isBlockerEnabled) "HABILITADO" else "DESABILITADO",
                color = if (isBlockerEnabled) Color(0xFF4CAF50) else Color.Red
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sEnabled = if (isBlockerEnabled) "Desabilitar" else "Habilitar"
                Text("$sEnabled bloqueio")
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = isBlockerEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            handleEnableBlocker {
                                isBlockerEnabled = true
                            }
                        } else {
                            isBlockerEnabled = false
                            prefs.edit().putBoolean("blocker_enabled", false).apply()
                        }
                    }
                )
            }
            Text("Total de chamadas bloqueadas: $iCounter", modifier = Modifier.padding(top = 16.dp))
        }
    }

    // --- Logic Helpers ---

    private fun handleEnableBlocker(onSuccess: () -> Unit) {
        // 1. Check Contacts Permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            requestContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
        // 2. Check Role
        else if (checkAndRequestRole()) {
            // If role is already held
            prefs.edit().putBoolean("blocker_enabled", true).apply()
            onSuccess()
        }
    }

    private fun handleHistoryNavigation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)
            == PackageManager.PERMISSION_GRANTED) {
            currentScreen = "history"
        } else {
            requestCallLogLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    private fun checkAndRequestRole(): Boolean {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
        return if (!isRoleHeld) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            requestRoleLauncher.launch(intent)
            false
        } else {
            true
        }
    }

    // --- History Logic ---

    @Composable
    fun HistoryScreen() {
        val blockedCalls = remember { getBlockedCallsFromLog() }
        val limit = prefs.getInt("blocker_counter", 0)

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Registro de chamadas bloqueadas", style = MaterialTheme.typography.titleLarge)
            Text("Exibindo $limit items dos registros", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (blockedCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma chamada bloqueada nos registros.")
                }
            } else {
                LazyColumn {
                    items(blockedCalls.take(if (limit > 0) limit else 50)) { number ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(text = number, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    private fun getBlockedCallsFromLog(): List<String> {
        val list = mutableListOf<String>()
        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.BLOCKED_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                while (it.moveToNext()) {
                    list.add(it.getString(numberIndex))
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
        return list
    }

    private fun getAppVersionName(): String {
        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "N/A"
        }
    }
}