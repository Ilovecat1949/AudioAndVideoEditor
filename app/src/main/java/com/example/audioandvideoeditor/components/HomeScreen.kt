package com.example.audioandvideoeditor.components

import PermissionsScreen
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audioandvideoeditor.MainActivity
import com.example.audioandvideoeditor.R
import com.example.audioandvideoeditor.utils.ConfigsUtils
import com.example.audioandvideoeditor.utils.FilesUtils
import com.example.audioandvideoeditor.utils.LogUtils
import com.example.audioandvideoeditor.viewmodel.HomeViewModel

//private fun NavHostController.navigateSingleTopTo(route: String) =
//    this.navigate(route)
//    {
//        popUpTo(
//            this@navigateSingleTopTo.graph.findStartDestination().id
//        ) {
//            saveState = true
//        }
//        launchSingleTop = true
//        restoreState = true
//    }

private fun NavHostController.navigateSingleTopTo(route: String) =
    this.navigate(route)
    {
        launchSingleTop = true
        restoreState = true

        val previousEntry = backQueue.reversed().find { it.destination.route == route }

        if (previousEntry != null) {
           //popBackStack(previousEntry.destination.route!!,false,true)
            previousEntry.destination.route?.let {
                popUpTo(it) {
                    inclusive = false // Don't remove the destination we're popping to.
                    saveState =false
                }
            }
            //Log.d(TAG,"previousEntry: ${previousEntry.destination.route}")
        }
      // Log.d(TAG,"Back Stack: ${this@navigateSingleTopTo.backQueue.map { it.destination.route }.joinToString()}")
    }

private val TAG="HomeScreen"
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
                    {file, route,flag ->
                        homeViewModel.file=file
                        homeViewModel.route_flag=flag
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
                //Text(text="TasksCenterScreen")
            }
            composable(
                route=UserCenter.route
            ){
              //  UserCenterScreen()
                UserCenterScreen {
                    homeNavController.navigateSingleTopTo(it)
                }
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
                AVInfoScreen2(activity, homeViewModel.file!!.path)
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
                ConfigScreen(
                    activity = activity,
                    nextDestination =  {
                        homeNavController.navigateSingleTopTo(it)
                    }
                    )
                //ConfigScreen2()
            }
            composable(
                route=VideoPlay.route
            ){
                VideosPlayScreen(modifier = Modifier.fillMaxSize(), path=homeViewModel.file!!.path)
            }
            composable(
                route=RePackaging.route
            ){
                RePackagingScreen(activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=FFmpegCommands.route
            ){
                FFmpegCommandsScreen(
                    activity,
                    {
                        homeNavController.navigateSingleTopTo(TasksCenter.route)
                        homeViewModel.show_interstistial_ad=true
                    },
                    {route:String->
                        homeNavController.navigateSingleTopTo(route)
                    },
                    {route:String->
                        homeViewModel.nextDestination={homeNavController.navigateSingleTopTo(route)}
                    },
                    homeViewModel.file
                )
            }
            composable(
                route=FilesList.route
            )
                {
                 FilesListScreen()
                }
            composable(
                route=FilesList2.route
            )
            {
                FilesListScreen2(
                    {file, route ->
                        homeViewModel.file=file
                        homeNavController.navigateSingleTopTo(route)
                    }
                )
            }

            composable(
                route=VideoSegmenter.route
            ){
               VideoSegmenterScreen(
                    activity
                   ,homeViewModel.file!!
                   ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
               )
            }

            composable(
                route=PrivacyPolicy.route
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.privacy_policy), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.privacy_policy_context))
                }
            }
            composable(
                route=APPInfo.route
            ){
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                    ,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(stringResource(id = R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(id = R.string.app_info_context))
                }
            }
            composable(
                route=ContactDeveloper.route
            ){
                val context= LocalContext.current
                val scrollState = rememberScrollState()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement= Arrangement.Center,
                    modifier = Modifier
                        .verticalScroll(scrollState) // 关键的滚动修饰符
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(stringResource(id = R.string.welcome), fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text= stringResource(id = R.string.welcome_text))
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.link_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.link))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text= stringResource(id = R.string.feedback_text))
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.mail_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(text = stringResource(id = R.string.mail_addr))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.group_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(text = stringResource(id = R.string.group_number))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.form1_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.form1_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ){
                        Text(text= stringResource(id = R.string.form2_name)+":")
                        Spacer(modifier = Modifier.width(10.dp))
                        SelectionContainer {
                            Text(
                                text= stringResource(id = R.string.form2_link),
                                textDecoration = TextDecoration.Underline,
                                color= Color.Blue,
                                modifier = Modifier.clickable {
                                    FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                                }
                            )
                        }
                    }
                }
            }
            composable(
                route=VideoFormatConversion.route
            ){
               VideoFormatConversionScreen(
                   activity
                   ,homeViewModel.file!!
                   ,{
                       homeNavController.navigateSingleTopTo(TasksCenter.route)
                       homeViewModel.show_interstistial_ad=true
                   }
               )
            }
            composable(
                route=SpeedChange.route
            ){
                SpeedChangeScreen(
                    activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=ExtractAudio.route
            ){
                ExtractAudioScreen(
                    activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }

            composable(
                route=VideoMute.route
            ){
                VideoMuteScreen(
                    activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            composable(
                route=VideoAspectRatio.route
            ){
                VideoAspectRatioScreen(
                    activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
            }
            VideoCrop
            composable(
                route=VideoCrop.route
            ){
                VideoCropScreen2(
                    activity
                    ,homeViewModel.file!!
                    ,{homeNavController.navigateSingleTopTo(TasksCenter.route)}
                )
 //               NineGridPage()
            }
            composable(
                route=FileRead.route
            ){
                FileReadingScreen(homeViewModel.file!!,homeViewModel.route_flag)
            }
            composable(
                route=LogDisplay.route
            ){
                LogDisplayScreen2()
            }
            composable(
                route=APPTest.route
            ){
                APPTestScreen()
            }
            composable(
                route=Permissions.route
            ){
                PermissionsScreen()
            }
            composable(
                route=VideoCompress.route
            ){
                VideoCompressScreen(
                    activity
                    ,homeViewModel.file!!
                    ,{
                        homeNavController.navigateSingleTopTo(TasksCenter.route)
                        homeViewModel.show_interstistial_ad=true
                    }
                )
            }
        }
        homeViewModel.show_crash_message_flag=ConfigsUtils.show_crash_message_flag
        if(homeViewModel.show_crash_message_flag){
            homeViewModel.show_on_screen_ad=false
            showCrashMessage(homeViewModel)
        }
        if(homeViewModel.show_on_screen_ad&&!homeViewModel.show_crash_message_flag && ConfigsUtils.show_on_screen_ad_again_flag){
            OnScreenAd(homeViewModel)
        }
        if(homeViewModel.show_interstistial_ad){
            ShowInterstitialAd(homeViewModel)
        }
    }
}

@Composable
private fun OnScreenAd(
    viewModel: HomeViewModel
){
    val context= LocalContext.current
    AlertDialog(
        onDismissRequest = {
            viewModel.show_on_screen_ad=false
        },
        title ={Text(stringResource(id = R.string.welcome))},
        text ={
            val scrollState = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement= Arrangement.Center,
                modifier = Modifier
                    .verticalScroll(scrollState) // 关键的滚动修饰符
                    .fillMaxWidth()
            ) {
                Text(text= stringResource(id = R.string.welcome_text))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.link_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(text= stringResource(id = R.string.feedback_text))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.mail_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.mail_addr))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.group_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.group_number))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form1_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form1_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form2_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form2_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                            }
                        )
                    }
                }
            }
        },
        confirmButton ={
           TextButton(onClick = {
               viewModel.show_on_screen_ad=false
           }) {
               Text(stringResource(id = R.string.ok))
           }
        },
        dismissButton ={
            TextButton(onClick = {
                viewModel.show_on_screen_ad=false
                ConfigsUtils.setShowOnScreenAdAgainFlag(context, false)
            }) {
                Text(stringResource(id = R.string.dont_remind_again))
            }
        }
    )
}

@Composable
fun ShowInterstitialAd(
    viewModel: HomeViewModel
){
    AlertDialog(
        onDismissRequest = {
            viewModel.show_interstistial_ad =false
        },
        title ={
            Text(stringResource(id = R.string.task_tip))
        },
        text ={},
        confirmButton ={
            TextButton(onClick = {
                viewModel.show_interstistial_ad =false
            }) {
                Text("OK")
            }
        },
        dismissButton ={}
    )
}


@Composable
fun showCrashMessage(viewModel: HomeViewModel) {
    val context= LocalContext.current
    val message= LogUtils.getLogContext(context)
    AlertDialog(
        onDismissRequest = {
            viewModel.show_crash_message_flag=false
            ConfigsUtils.setCrashMessageFlag(context,false)
        },
        title = {
            Text(stringResource(id = R.string.crash_message_title))
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement= Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.feedback_text2))
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.mail_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.mail_addr))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.group_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(text = stringResource(id = R.string.group_number))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form1_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form1_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form1_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text(text= stringResource(id = R.string.form2_name)+":")
                    Spacer(modifier = Modifier.width(10.dp))
                    SelectionContainer {
                        Text(
                            text= stringResource(id = R.string.form2_link),
                            textDecoration = TextDecoration.Underline,
                            color= Color.Blue,
                            modifier = Modifier.clickable {
                                FilesUtils.openWebLink(context,context.getString(R.string.form2_link))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("logs", message)
                            clipboard.setPrimaryClip(clip)
                            val toast = Toast.makeText(context, context.getString(R.string.copy_to_clipboard), Toast.LENGTH_SHORT)
                            toast.setGravity(Gravity.CENTER, 0, 0)
                            toast.show()
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_content_copy_24)
                                , contentDescription = null)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                    ){
                        item {
                            Text(message)
                        }
                    }
                }

            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.show_crash_message_flag=false
                ConfigsUtils.setCrashMessageFlag(context,false)
            }) {
                Text("确定")
            }
        },
        dismissButton = {
        }
    )
}