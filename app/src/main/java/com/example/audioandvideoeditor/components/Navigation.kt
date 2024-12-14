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

@Composable
fun SootheBottomNavigation(onTabSelected: (Destination) -> Unit,currentScreen:Destination){
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
    ){
        NavigationBarItem(
            icon={
                Icon(painter = painterResource(id = R.drawable.baseline_business_center_24),
                    contentDescription =null
                )
            },
            label = {
                Text(
                    text = LocalContext.current.resources.getString(R.string.function_center)
                )
            },
            selected= currentScreen==FunctionsCenter,
            onClick = {onTabSelected(FunctionsCenter)}
        )
        NavigationBarItem(
            icon={
                Icon(painter = painterResource(id = R.drawable.baseline_notes_24),
                    contentDescription =null
                )
            },
            label = {
                Text(
                    text = LocalContext.current.resources.getString(R.string.task_center)
                )
            },
            selected= currentScreen==TasksCenter,
            onClick = {onTabSelected(TasksCenter)}
        )
        NavigationBarItem(
            icon={
                Icon(painter = painterResource(id = R.drawable.baseline_person_24),
                    contentDescription =null
                )
            },
            label = {
                Text(
                    text = LocalContext.current.resources.getString(R.string.user_center)
                )
            },
            selected= currentScreen==UserCenter,
            onClick = {onTabSelected(UserCenter)}
        )
    }
}