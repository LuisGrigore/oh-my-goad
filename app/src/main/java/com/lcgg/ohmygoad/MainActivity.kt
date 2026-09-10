package com.lcgg.ohmygoad

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.lcgg.ohmygoad.ui.theme.OhMyGOADTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UpdateCheckHandler()
            OhMyGOADTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppVersion(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppVersion(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0),
        ).versionName
    }

    Text(
        text = "Version $versionName",
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
fun AppVersionPreview() {
    OhMyGOADTheme {
        Text(text = "Version 1.0.0")
    }
}