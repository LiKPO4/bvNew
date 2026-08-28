package dev.aaa1115910.bv.mobile.component.settings

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.aaa1115910.bv.component.settings.UpdateDialog

@Composable
fun UpdateDialog(
    modifier: Modifier = Modifier,
    show: Boolean,
    onHideDialog: () -> Unit
) {
    UpdateDialog(
        modifier = modifier,
        show = show,
        onHideDialog = onHideDialog,
        text = { text ->
            Text(text = text)
        },
        button = { buttonModifier, enabled, onClick, content ->
            Button(
                modifier = buttonModifier,
                enabled = enabled,
                onClick = onClick,
                content = content
            )
        },
        outlinedButton = { buttonModifier, enabled, onClick, content ->
            OutlinedButton(
                modifier = buttonModifier,
                enabled = enabled,
                onClick = onClick,
                content = content
            )
        }
    )
}
