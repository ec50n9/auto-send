package com.shello.autonotice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import com.shello.autonotice.ui.theme.AutoNoticeTheme
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

class MainActivity : ComponentActivity() {
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoNoticeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    // 权限请求
                    Column(Modifier.fillMaxSize()){
                        val multiplePermissionsState = rememberMultiplePermissionsState(
                            listOf(
                                android.Manifest.permission.SEND_SMS,
                                android.Manifest.permission.READ_PHONE_STATE,
                            )
                        )
                        Sample(
                            this@MainActivity,
                            multiplePermissionsState,
                            navigateToSettingsScreen = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", packageName, null)
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalPermissionsApi
@Composable
private fun FeatureThatRequiresSendSMSPermission(
    navigateToSettingsScreen: () -> Unit
) {
    var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

    val sendSMSPermissionState =
        rememberPermissionState(permission = Manifest.permission.SEND_SMS)
    PermissionRequired(
        permissionState = sendSMSPermissionState,
        permissionNotGrantedContent = {
            if (doNotShowRationale) {
                Text(text = "请求失败")
            } else {
                Column {
                    Text("发送短信的权限至关重要，请给予")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = { sendSMSPermissionState.launchPermissionRequest() }) {
                            Text("前往授权!")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { doNotShowRationale = true }) {
                            Text("不授权（将无法继续使用）")
                        }
                    }
                }
            }
        },
        permissionNotAvailableContent = {
            Column {
                Text(
                    "请给予发送短信权限"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = navigateToSettingsScreen) {
                    Text("打开设置")
                }
            }
        }
    ) {
        Text("已获取发送短信权限")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun Sample(
    activity: Activity,
    multiplePermissionsState: MultiplePermissionsState,
    navigateToSettingsScreen: () -> Unit
) {
    var doNotShowRationale by rememberSaveable { mutableStateOf(false) }

    when {
        multiplePermissionsState.allPermissionsGranted -> {
            Text("权限ok!")
            Log.d("shello", "Sample: hahahahhaha")
            activity.startActivity(Intent(activity, TextActivity::class.java))
            activity.finish()
        }
        multiplePermissionsState.shouldShowRationale ||
                !multiplePermissionsState.permissionRequested ->
        {
            if (doNotShowRationale) {
                Text("功能无法使用")
            } else {
                Column {
                    val revokedPermissionsText = getPermissionsText(
                        multiplePermissionsState.revokedPermissions
                    )
                    Text(
                        "$revokedPermissionsText 十分重要. " +
                                "请全部给予，否则无法自动发送信息"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(
                            onClick = {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                            }
                        ) {
                            Text("给予权限")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { doNotShowRationale = true }) {
                            Text("不授权（无法使用功能）")
                        }
                    }
                }
            }
        }
        else -> {
            Column {
                val revokedPermissionsText = getPermissionsText(
                    multiplePermissionsState.revokedPermissions
                )
                Text(
                    "$revokedPermissionsText denied. See this FAQ with " +
                            "information about why we need this permission. Please, grant us " +
                            "access on the Settings screen."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = navigateToSettingsScreen) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getPermissionsText(permissions: List<PermissionState>): String {
    val revokedPermissionsSize = permissions.size
    if (revokedPermissionsSize == 0) return ""

    val textToShow = StringBuilder().apply {
        append("The ")
    }

    for (i in permissions.indices) {
        textToShow.append(permissions[i].permission)
        when {
            revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> {
                textToShow.append(", and ")
            }
            i == revokedPermissionsSize - 1 -> {
                textToShow.append(" ")
            }
            else -> {
                textToShow.append(", ")
            }
        }
    }
    textToShow.append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
    return textToShow.toString()
}

