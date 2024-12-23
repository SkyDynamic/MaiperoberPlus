package io.github.skydynamic.maiproberplus

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Observer
import io.github.skydynamic.maiproberplus.ui.compose.DownloadDialog
import io.github.skydynamic.maiproberplus.ui.compose.InfoDialog
import io.github.skydynamic.maiproberplus.ui.compose.bests.BestsImageGenerateCompose
import io.github.skydynamic.maiproberplus.ui.compose.checkResourceComplete
import io.github.skydynamic.maiproberplus.ui.compose.scores.ScoreManagerCompose
import io.github.skydynamic.maiproberplus.ui.compose.setting.SettingCompose
import io.github.skydynamic.maiproberplus.ui.compose.sync.SyncCompose
import io.github.skydynamic.maiproberplus.ui.theme.MaiProberplusTheme

val NOTIFICATION_CHANNEL_ID = "io.github.skydynamic.maiproberplus.notification.channel.default"
val PROCESS_NOTIFICATION_CHANNEL_ID = "io.github.skydynamic.maiproberplus.notification.channel.process"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaiProberplusTheme(
                dynamicColor = false
            ) {
                AppContent()
            }
        }

        GlobalViewModel.localMessage.observe(this, Observer { message ->
            GlobalViewModel.showMessageDialog = true
        })
    }
}

@Composable
@SuppressLint("NewApi")
fun AppContent() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var openInitDownloadDialog by remember { mutableStateOf(false) }

    val items = listOf("成绩同步", "成绩管理", "图片生成", "设置")
    val selectedIcons = listOf(
        Icons.Filled.Refresh,
        Icons.Filled.Build,
        Icons.Filled.Star,
        Icons.Filled.Settings
    )
    val unselectedIcons = listOf(
        Icons.Outlined.Refresh,
        Icons.Outlined.Build,
        Icons.Filled.Star,
        Icons.Outlined.Settings
    )

    val composeList: List<@Composable () -> Unit> = listOf(
        @Composable { SyncCompose() },
        @Composable { ScoreManagerCompose() },
        @Composable { BestsImageGenerateCompose() },
        @Composable { SettingCompose() }
    )

    val checkResourceResult = checkResourceComplete()
    if (checkResourceResult.isNotEmpty()) {
        openInitDownloadDialog = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (selectedItem == index) selectedIcons[index] else unselectedIcons[index],
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Crossfade(
                targetState = selectedItem,
                animationSpec = tween(durationMillis = 500),
                label = "pageCross"
            ) { targetState ->
                composeList[targetState]()
            }
        }
    }

    when {
        GlobalViewModel.showMessageDialog -> {
            InfoDialog(GlobalViewModel.localMessage.value!!) {
                GlobalViewModel.showMessageDialog = false
            }
        }
        openInitDownloadDialog -> {
            DownloadDialog(checkResourceResult) {
                openInitDownloadDialog = false
            }
        }
    }
}
