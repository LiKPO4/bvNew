package dev.aaa1115910.bv.tv.component.buttons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonBorder
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.ui.theme.BVTheme

@Composable
fun ToViewButton(
    modifier: Modifier = Modifier,
    onAddToView: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    colors: ButtonColors = ButtonDefaults.colors(),
    border: ButtonBorder = ButtonDefaults.border()
) {
    Button(
        modifier = modifier,
        onClick = { onAddToView() },
        contentPadding = contentPadding,
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = colors,
        border = border,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null
            )
            Text(
                text = stringResource(R.string.toview_button_text)
            )
        }
    }
}

@Preview
@Composable
fun ToViewButtonPreview() {
    BVTheme {
        ToViewButton()
    }
}