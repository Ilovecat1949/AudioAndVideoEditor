package com.example.audioandvideoeditor.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.navigation.Destination

/**
 * 优化后的底部导航组件
 * @param onTabSelected 标签选中回调
 * @param currentScreen 当前选中的页面
 */
@Composable
fun SootheBottomNavigation(
    onTabSelected: (Destination) -> Unit,
    currentScreen: Destination
) {
    val context = LocalContext.current

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
    ) {
        // 功能中心
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_business_center_24),
                    contentDescription = null
                )
            },
            label = { Text(text = context.resources.getString(R.string.function_center)) },
            selected = currentScreen == Destination.FunctionsCenter,
            onClick = { onTabSelected(Destination.FunctionsCenter) }
        )

        // 任务中心
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_notes_24),
                    contentDescription = null
                )
            },
            label = { Text(text = context.resources.getString(R.string.task_center)) },
            selected = currentScreen == Destination.TasksCenter,
            onClick = { onTabSelected(Destination.TasksCenter) }
        )

        // 文件中心
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_folder_24),
                    contentDescription = null
                )
            },
            label = { Text(text = context.resources.getString(R.string.file)) },
            selected = currentScreen == Destination.FilesList2,
            onClick = { onTabSelected(Destination.FilesList2) }
        )

        // 设置中心
        NavigationBarItem(
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.settings_24px),
                    contentDescription = null
                )
            },
            label = { Text(text = context.resources.getString(R.string.settings)) },
            selected = currentScreen == Destination.Config,
            onClick = { onTabSelected(Destination.Config) }
        )
    }
}