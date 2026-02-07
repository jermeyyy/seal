package com.jermey.seal

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun OkHttpDemoContent() {
    // Should never be reached on iOS — the tab is hidden.
    Text("OkHttp is not available on this platform.")
}
