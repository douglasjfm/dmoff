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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            Toast.makeText(this, getString(R.string.str_msg_permissao_req), Toast.LENGTH_LONG).show()
        }
    }

    // Launcher for READ_CALL_LOG (needed for History screen)
    private val requestCallLogLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            currentScreen = "history"
        } else {
            Toast.makeText(this, getString(R.string.str_msg_perm_negada), Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher for the Default Call Screening Role
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, getString(R.string.str_msg_permissao), Toast.LENGTH_SHORT).show()
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
                    title = { Text(getString(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(getString(R.string.str_menu_opt)) },
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
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        // Use derivedState to keep UI synced with SharedPreferences
        var isBlockerEnabled by remember {
            mutableStateOf(prefs.getBoolean("blocker_enabled", false) && roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING))
        }
        val iCounter = prefs.getInt("blocker_counter", 0)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = getString(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            // Display Version
            Text(
                text = getString(R.string.str_versao, getAppVersionName()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 1.dp)
            )
            Text(
                text = if (isBlockerEnabled) getString(R.string.str_hab) else getString(R.string.str_desab),
                color = if (isBlockerEnabled) Color(0xFF4CAF50) else Color.Red
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sEnabled = if (isBlockerEnabled) getString(R.string.str_desabilitar) else getString(R.string.str_habilitar)
                Text(getString(R.string.str_swit_desc, sEnabled))
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
                            prefs.edit().putInt("blocker_counter", 0).apply()
                        }
                    }
                )
            }
            if (isBlockerEnabled) Text(getString(R.string.str_bloqueios_cont, iCounter), modifier = Modifier.padding(top = 16.dp))
            else {
                Text(getString(R.string.str_bloqueios_na), modifier = Modifier.padding(top = 16.dp))
                prefs.edit().putInt("blocker_counter", 0).apply()
            }
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
        val limit = blockedCalls.size

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text(getString(R.string.str_regis_tit), style = MaterialTheme.typography.titleLarge)
            Text(getString(R.string.str_regis_desc, limit), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (blockedCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(getString(R.string.str_regis_empty))
                }
            } else {
                LazyColumn {
                    items(blockedCalls.take(if (limit > 0) limit else 50)) { record ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)) {
                            Text(text = record.first + " - " + record.second, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }

    private fun getBlockedCallsFromLog(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE), // Added DATE here
                "${CallLog.Calls.TYPE} = ?",
                arrayOf(CallLog.Calls.BLOCKED_TYPE.toString()),
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)

                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    val dateLong = it.getLong(dateIndex)
                    val formattedDate = dateFormat.format(Date(dateLong))

                    list.add(number to formattedDate)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted handled by handleHistoryNavigation
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