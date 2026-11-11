package hrln.interactive.sidekit.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hrln.interactive.sidekit.design.theme.SidekitTheme
import hrln.interactive.sidekit.sdk.SidekitContract

/**
 * CI smoke consumer: builds against the Sidekit artifacts and renders the design system plus a
 * value from the SDK contract.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SidekitTheme {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "Sidekit SDK sample\nContract schema ${SidekitContract.SCHEMA_VERSION}",
                        style = SidekitTheme.typography.body
                    )
                }
            }
        }
    }
}
