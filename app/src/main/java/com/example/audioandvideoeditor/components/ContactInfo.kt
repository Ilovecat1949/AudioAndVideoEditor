package com.example.audioandvideoeditor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.FilesUtils

/**
 * 通用联系方式展示组件（提取重复逻辑）
 */
@Composable
fun ContactInfoSection() {
    val context = LocalContext.current

    ContactItem(
        labelRes = R.string.link_name,
        valueRes = R.string.link,
        isClickable = true,
        onClick = { FilesUtils.openWebLink(context, context.getString(R.string.link)) }
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(text = stringResource(id = R.string.feedback_text))

    Spacer(modifier = Modifier.height(20.dp))
    ContactItem(labelRes = R.string.mail_name, valueRes = R.string.mail_addr)

    Spacer(modifier = Modifier.height(20.dp))
    ContactItem(labelRes = R.string.group_name, valueRes = R.string.group_number)

//    Spacer(modifier = Modifier.height(20.dp))
//    ContactItem(
//        labelRes = R.string.form1_name,
//        valueRes = R.string.form1_link,
//        isClickable = true,
//        onClick = { FilesUtils.openWebLink(context, context.getString(R.string.form1_link)) }
//    )

    Spacer(modifier = Modifier.height(20.dp))
    ContactItem(
        labelRes = R.string.form2_name,
        valueRes = R.string.form2_link,
        isClickable = true,
        onClick = { FilesUtils.openWebLink(context, context.getString(R.string.form2_link)) }
    )
}

/**
 * 单个联系项组件
 * @param labelRes 标签字符串资源ID
 * @param valueRes 内容字符串资源ID
 * @param isClickable 是否可点击（仅链接类需要）
 * @param onClick 点击事件（可点击时生效）
 */
@Composable
fun ContactItem(
    labelRes: Int,
    valueRes: Int,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(text = stringResource(id = labelRes) + ":")
        Spacer(modifier = Modifier.width(10.dp))

        SelectionContainer {
            Text(
                text = stringResource(id = valueRes),
                color = if (isClickable) Color.Blue else Color.Unspecified,
                textDecoration = if (isClickable) TextDecoration.Underline else TextDecoration.None,
                modifier = if (isClickable) Modifier.clickable { onClick?.invoke() } else Modifier
            )
        }
    }
}