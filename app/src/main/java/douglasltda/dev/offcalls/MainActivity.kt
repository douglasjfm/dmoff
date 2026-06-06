package douglasltda.dev.offcalls

import android.os.Bundle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import douglasltda.dev.offcalls.ui.theme.OffCallsTheme
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.error
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    // SharedPreferences to save the toggle state
    private val prefs by lazy { getSharedPreferences("off_calls_prefs", Context.MODE_PRIVATE) }

    // Contact Permission Request Launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkAndRequestRole()
        } else {
            Toast.makeText(this, "Contacts permission is required to filter calls.", Toast.LENGTH_LONG).show()
        }
    }

    // Role Manager Launcher (Android 10+)
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "App successfully set as Call Screening app!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "App needs to be the screening app to block calls.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.layout) // Create a interface for requesting user to turn on or turn off

        // Use setContent for Jetpack Compose UI
        setContent {
            OffCallsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }

        // 1. Check Contacts Permission
        /*if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            checkAndRequestRole()
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }*/
    }

    // 2. Request the Call Screening Role
    private fun checkAndRequestRole(): Boolean {
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
        val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

        if (!isRoleHeld) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            requestRoleLauncher.launch(intent)
            return false
        } else {
            Toast.makeText(this, "Off Calls is active.", Toast.LENGTH_SHORT).show()
            return true
        }
        /*} else {
            Toast.makeText(this, "This API requires Android 10 (API 29) or higher.", Toast.LENGTH_LONG).show()
        }*/
    }

    @Composable
    fun MainScreen() {
        // State for the switch, persisted in SharedPreferences
        var isBlockerEnabled by remember {
            mutableStateOf(prefs.getBoolean("blocker_enabled", false))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Off Calls Blocker",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isBlockerEnabled) "Blocking is ACTIVE" else "Blocking is INACTIVE",
                color = if (isBlockerEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Enable Call Blocker")
                Spacer(modifier = Modifier.width(16.dp))
                Text("Counter: ${prefs.getInt("blocker_counter", 0)}")
                Switch(
                    checked = isBlockerEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            // When turning ON, check permissions
                            handleEnableBlocker {
                                isBlockerEnabled = true
                                prefs.edit().putBoolean("blocker_enabled", true).apply()
                                prefs.edit().putInt("blocker_counter", 0).apply()
                            }
                        } else {
                            // Turn OFF
                            isBlockerEnabled = false
                            prefs.edit().putBoolean("blocker_enabled", false).apply()
                        }
                    }
                )
            }
        }
    }

    private fun handleEnableBlocker(onSuccess: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED) {
            if (checkAndRequestRole()) {
                onSuccess()
            }
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OffCallsTheme {
        Greeting("Android")
    }
}