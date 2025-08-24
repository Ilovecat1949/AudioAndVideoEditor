import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.lifecycle.rememberLifecycle
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.PermissionsUtils

@Composable
fun BasicSwitch() {
    var isChecked by remember { mutableStateOf(false) }
    Switch(
        checked = isChecked,
        onCheckedChange = { isChecked = it }
    )
}
@Composable
fun PermissionsScreen(
){
    val context= LocalContext.current
//    var externalStoragePermissionRemind by remember {
//        mutableStateOf(ConfigsUtils.externalStoragePermissionRemind)
//    }
//    var notificationsRemind by remember {
//        mutableStateOf(ConfigsUtils.notificationsRemind)
//    }
    val life= rememberLifecycle()
    life.onLifeCreate {
        //println("PermissionsScreen onLifeCreate")
        PermissionsUtils.checkNotificationsPermission(context)
        PermissionsUtils.checkSelfExternalStoragePermission(context)
        PermissionsUtils.isIgnoringBatteryOptimizations(context)
    }
//    var ignoringBatteryOptimizationsEnabled by  remember {
//        mutableStateOf(PermissionsUtils.ignoringBatteryOptimizationsEnabled)
//    }
//    val launcher = rememberLauncherForActivityResult(
        // 使用 StartActivityForResult 协定
//        contract = ActivityResultContracts.StartActivityForResult()
//    ) { result ->
//        PermissionsUtils.checkNotificationsPermission(context)
//        PermissionsUtils.checkSelfExternalStoragePermission(context)
//        PermissionsUtils.isIgnoringBatteryOptimizations(context)
//        println("PermissionsScreen onLifeCreate")
//    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement= Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text=stringResource(R.string.file_read_and_write)
                    , fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {

//                    Text(stringResource(R.string.remind))
//                   Spacer(modifier = Modifier.width(5.dp))
//                   Switch(
//                        checked = externalStoragePermissionRemind,
//                        onCheckedChange = {
//                            ConfigsUtils.setPermissionRemind(context,!ConfigsUtils.externalStoragePermissionRemind,0)
//                            externalStoragePermissionRemind=ConfigsUtils.externalStoragePermissionRemind
//                        }
//                    )
//                    Spacer(modifier = Modifier.width(15.dp))
                    Text(stringResource(R.string.grant))
                    Spacer(modifier = Modifier.width(5.dp))
                    Switch(
                        checked = PermissionsUtils.areSelfExternalStoragePermissionEnabled,
                        onCheckedChange = {
                            val packageName = context.packageName
                            val intent = Intent()
                            if(
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.R
                            ){
                                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                intent.data = Uri.fromParts("package", packageName, null)
                                ContextCompat.startActivity(context, intent, null)
                                //launcher.launch(intent)
                            }
                            else{
                                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                intent.data = Uri.fromParts("package", packageName, null)
                                ContextCompat.startActivity(context, intent, null)
                                //launcher.launch(intent)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.notification), fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    {
//                    Text(stringResource(R.string.remind))
//                    Spacer(modifier = Modifier.width(5.dp))
//                    Switch(
//                        checked = notificationsRemind,
//                        onCheckedChange = {
//                            ConfigsUtils.setPermissionRemind(context,!ConfigsUtils.notificationsRemind,1)
//                            notificationsRemind=ConfigsUtils.notificationsRemind
//                        }
//                    )
//                    Spacer(modifier = Modifier.width(15.dp))
                        Text(stringResource(R.string.grant))
                        Spacer(modifier = Modifier.width(5.dp))
                        Switch(
                            checked = PermissionsUtils.areNotificationsEnabled,
                            onCheckedChange = {
//                            if( Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU){
//                                val intent = Intent()
//                                val packageName = context.packageName
//                                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
//                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//                                ContextCompat.startActivity(context, intent, null)
//                            }
                                val intent = Intent().apply {
                                    when {
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            putExtra(
                                                Settings.EXTRA_APP_PACKAGE,
                                                context.packageName
                                            )
                                        }
                                        // For older versions, you might need to guide them to the general app info screen
                                        // from where they can access notification settings.
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                            data =
                                                Uri.fromParts("package", context.packageName, null)
                                        }

                                        else -> {
                                            action =
                                                Settings.ACTION_SETTINGS // Fallback to general settings
                                        }
                                    }
                                }
                                ContextCompat.startActivity(context, intent, null)
                                //launcher.launch(intent)
                            }
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
                Divider(color = Color.LightGray, thickness = 1.dp)
            }
        }
        Column (
            modifier = Modifier
                .fillMaxWidth()
        ){
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text=stringResource(R.string.cancel_power_saving)
                    , fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {

                    Text(stringResource(R.string.grant))
                    Spacer(modifier = Modifier.width(5.dp))
                    Switch(
                        checked =PermissionsUtils.ignoringBatteryOptimizationsEnabled,
                        onCheckedChange = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent().apply {
                                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                    data = Uri.parse("package:" + context.packageName)
                                }
                                context.startActivity(intent)
                                //launcher.launch(intent)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}


