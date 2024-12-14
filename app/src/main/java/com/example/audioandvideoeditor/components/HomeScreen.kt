package com.example.audioandvideoeditor.components

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.viewmodel.HomeViewModel

private fun NavHostController.navigateSingleTopTo(route: String) =
    this.navigate(route) {
        popUpTo(
            this@navigateSingleTopTo.graph.findStartDestination().id
        ) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }

@Composable
fun HomeScreen(
    activity: MainActivity,
    homeViewModel: HomeViewModel= viewModel()
)
{
    val homeNavController= rememberNavController()
    val currentBackStack by homeNavController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val currentScreen =HomeNavigationRow.find { it.route == currentDestination?.route }?:FunctionsCenter
    Scaffold(
        bottomBar = {
            SootheBottomNavigation({ screen ->
                run {
                    homeNavController.navigateSingleTopTo(screen.route)
                }
            }, currentScreen)
        }
    )  { innerPadding ->
        NavHost(
            navController = homeNavController,
            startDestination = FunctionsCenter.route,
            modifier = Modifier.padding(innerPadding)
        ){
            composable(
                route=FunctionsCenter.route
            ){
              FunctionsCenterScreen(
                  {
                      homeNavController.navigateSingleTopTo(it)
                  },
                  {
                   homeViewModel.nextDestination={homeNavController.navigateSingleTopTo(it) }
                  })
              //Text(text="FunctionsCenterScreen")
            }
            composable(
                route=TasksCenter.route
            ){
              //  TasksCenterScreen3(task_binder,taskDao)
                TasksCenterScreen(
                    activity,
                    {file, route ->
                        homeViewModel.file=file
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
                //Text(text="TasksCenterScreen")
            }
            composable(
                route=UserCenter.route
            ){
              //  UserCenterScreen()
                UserCenterScreen({
                    homeNavController.navigateSingleTopTo(it)
                })
//                Text(text="UserCenterScreen")
            }
            composable(
                route=FileSelection.route
            ){
                FileSelectionScreen(
                    backDestination = { homeNavController .popBackStack() },
                    setFile = {
                      homeViewModel.file=it
                    },
                    nextDestination = homeViewModel.nextDestination)
            }
            composable(
                route=ReEncoding.route
            ){
                ReEncodingScreen(activity
                                ,homeViewModel.file!!
                                ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=AudioAndVideoInfo.route
            ){
                AVInfoScreen(activity, homeViewModel.file!!.path)
            }
            composable(
                route=VideoFilesList.route
            ){
                VideoFilesListScreen(
                    {file, route ->
                        homeViewModel.file=file
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }
            composable(
                route=FFmpegInfo.route
            ){
                FFmpegInfoScreen(activity)
            }
            composable(
                route=Config.route
            ){
                ConfigScreen(activity = activity)
            }
            composable(
                route=VideoPlay.route
            ){
                VideosPlayScreen(modifier = Modifier.fillMaxSize(), path=homeViewModel.file!!.path)
            }
        }
    }
}
