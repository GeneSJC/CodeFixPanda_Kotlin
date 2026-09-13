package com.example.githubissuefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.githubissuefinder.ui.navigation.AppNavHost
import com.example.githubissuefinder.ui.theme.GitHubIssueFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubIssueFinderTheme {
                AppNavHost()
            }
        }
    }
}
