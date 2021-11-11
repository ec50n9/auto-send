package com.shello.autonotice

import android.content.Context
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import com.shello.autonotice.ui.theme.AutoNoticeTheme
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

class TextActivity : ComponentActivity() {
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoNoticeTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Greeting2()
                }
            }
        }
    }

    @ExperimentalPermissionsApi
    @Composable
    fun Greeting2() {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(ScrollState(0))
        ) {
            var originalData by remember { mutableStateOf("") }
            var objectDivision by remember { mutableStateOf("") }
            var propertyDivision by remember { mutableStateOf("") }
            var phoneIndex by remember { mutableStateOf("") }
            var targetText by remember { mutableStateOf("") }
            Surface(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxWidth()
                    .shadow(2.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "短信群发小工具", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = originalData,
                onValueChange = { originalData = it },
                label = { Text(text = "原始数据") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = objectDivision,
                onValueChange = { objectDivision = it },
                label = { Text(text = "对象分割符") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = propertyDivision,
                onValueChange = { propertyDivision = it },
                label = { Text(text = "属性分割符") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = phoneIndex,
                onValueChange = { phoneIndex = it },
                label = { Text(text = "号码变量下标（从0开始）") })
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text(text = "短信内容") })

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                sendAllSMS(
                    segmentationData(originalData, objectDivision, propertyDivision),
                    phoneIndex,
                    targetText
                )
                Toast.makeText(this@TextActivity, "发送完成，请打开短信app查看", Toast.LENGTH_SHORT).show()
            }) {
                Text(text = "开始发送")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

fun segmentationData(
    originalData: String,
    objectDivision: String,
    propertyDivision: String
): List<Map<String, String>> {
    val objectTextList = originalData.split(objectDivision)
    val dataList = mutableListOf<MutableMap<String, String>>()
    objectTextList.forEach { objectText ->
        val properties = mutableMapOf<String, String>()
        objectText
            .split(propertyDivision)
            .forEachIndexed { index, property ->
                properties[index.toString()] = property
            }
        dataList.add(properties)
    }
    return dataList
}

fun textualSubstitution(targetText: String, attrMap: Map<String, String>): String {
    var text = targetText
    attrMap.forEach { (key, value) ->
        text = text.replace("{$key}", value)
    }
    return text
}

private fun sendAllSMS(
    varList: List<Map<String, String>>,
    phoneIndex: String,
    targetText: String
) {
    varList.forEach { attrMap ->
        val phone = attrMap[phoneIndex] ?: ""
        Log.d("shello", "开始发送: $phone")
        // fakeSendSMS(phone, textualSubstitution(targetText, attrMap))
        sendSMS(phone, textualSubstitution(targetText, attrMap))
        Log.d("shello", "已发送: $phone")
    }
}

private fun getInputStream(path: String) = FileInputStream(File(path))

private fun readExcel(
    context: Context,
    stream: FileInputStream,
    todo: (phone: String, name: String) -> Unit
) {
    //val stream = context.resources.openRawResource(R.raw.phone)
    try {
        val workbook = XSSFWorkbook(stream)
        val sheet = workbook.getSheetAt(0)
        val rowCount = sheet.physicalNumberOfRows
        Log.d("shello", "共有: ${rowCount - 1}条电话记录")
        //val formulaEvaluator = workbook.creationHelper.createFormulaEvaluator()
        for (i in 1 until rowCount) {
            val row = sheet.getRow(i)
            val cellsCount = row.physicalNumberOfCells
            val phone = row.getCell(0).stringCellValue
            val name = row.getCell(1).stringCellValue
            todo(phone, name)
            Log.d("shello", "已发送: $phone")
        }
    } catch (e: Exception) {
        Log.e("shello", "错误: ${e.message}")
    }
}

private fun fakeSendSMS(phone: String, content: String) {
    Log.d("shello", "fakeSendSMS: $phone ==> $content")
}

private fun sendSMS(phone: String, content: String) {
    if (phone.isNotEmpty() && content.isNotEmpty()) {
        val manager = SmsManager.getDefault()
        if (content.length > 70) {
            val msgs = manager.divideMessage(content)
            manager.sendMultipartTextMessage(phone, null, msgs, null, null)
        } else
            manager.sendTextMessage(phone, null, content, null, null)
    }
}
