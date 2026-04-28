package dev.aaa1115910.bv.tv.component.pgc

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.pgc.PgcType
import dev.aaa1115910.biliapi.entity.pgc.index.PGC_INDEX_ORDER_FIELD
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexOption
import dev.aaa1115910.biliapi.entity.pgc.index.PgcIndexSection
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.tv.component.TvAlertDialog
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.util.getDisplayName

@Composable
fun IndexFilter(
    modifier: Modifier = Modifier,
    type: PgcType,
    show: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<PgcIndexSection>,
    selectedFilters: Map<String, PgcIndexOption>,
    onFilterChange: (PgcIndexOption) -> Unit,
    onResetFilters: () -> Unit
) {
    val context = LocalContext.current

    IndexFilterContent(
        modifier = modifier,
        title = stringResource(R.string.pgc_index_filter_title_prefix) + type.getDisplayName(context),
        show = show,
        onDismissRequest = onDismissRequest,
        sections = sections,
        selectedFilters = selectedFilters,
        onFilterChange = onFilterChange,
        onResetFilters = onResetFilters
    )
}

@Composable
private fun IndexFilterContent(
    modifier: Modifier = Modifier,
    title: String,
    show: Boolean,
    onDismissRequest: () -> Unit,
    sections: List<PgcIndexSection>,
    selectedFilters: Map<String, PgcIndexOption>,
    onFilterChange: (PgcIndexOption) -> Unit,
    onResetFilters: () -> Unit
) {
    if (show) {
        TvAlertDialog(
            modifier = modifier
                .fillMaxWidth(0.8f),
            onDismissRequest = onDismissRequest,
            confirmButton = {
                if (sections.isNotEmpty()) {
                    OutlinedButton(onClick = onResetFilters) {
                        Text(text = stringResource(R.string.filter_dialog_reset))
                    }
                }
            },
            title = {
                Text(text = title)
            },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = sections.filter { it.options.isNotEmpty() },
                        key = { section -> section.field }
                    ) { section ->
                        IndexFilterChipRow(
                            title = section.title,
                            options = section.options,
                            selectedFilter = selectedFilters[section.field],
                            onFilterChange = onFilterChange
                        )
                    }
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun IndexFilterChip(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    FilterChip(
        modifier = modifier,
        selected = selected,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = selected) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
            }
            Text(text = label)
        }
    }
}

@Composable
private fun IndexFilterChipRow(
    modifier: Modifier = Modifier,
    title: String,
    options: List<PgcIndexOption>,
    selectedFilter: PgcIndexOption?,
    onFilterChange: (PgcIndexOption) -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge
        )
        LazyRow(
            modifier = modifier
                .focusRestorer(focusRequester),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        ) {
            items(
                items = options,
                key = { option -> "${option.field}:${option.keyword}:${option.sort.orEmpty()}" }
            ) { option ->
                IndexFilterChip(
                    modifier = if (selectedFilter == option) Modifier.focusRequester(focusRequester) else Modifier,
                    selected = selectedFilter == option,
                    onClick = { onFilterChange(option) },
                    label = option.name
                )
            }
        }
    }
}

private class PgcTypeProvider : PreviewParameterProvider<PgcType> {
    override val values = PgcType.entries.asSequence()
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IndexFilterPreview(
    @PreviewParameter(PgcTypeProvider::class) pgcType: PgcType
) {
    val sections = remember {
        listOf(
            PgcIndexSection(
                field = PGC_INDEX_ORDER_FIELD,
                title = "排序",
                options = listOf(
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "8", "综合排序", sort = "0"),
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "3", "最多追番", sort = "0"),
                    PgcIndexOption(PGC_INDEX_ORDER_FIELD, "0", "最近更新", sort = "0")
                )
            ),
            PgcIndexSection(
                field = "area",
                title = "地区",
                options = listOf(
                    PgcIndexOption("area", "-1", "全部地区"),
                    PgcIndexOption("area", "1,6,7", "国产"),
                    PgcIndexOption("area", "2", "日本"),
                    PgcIndexOption("area", "3", "美国")
                )
            ),
            PgcIndexSection(
                field = "season_status",
                title = "付费类型",
                options = listOf(
                    PgcIndexOption("season_status", "-1", "全部付费"),
                    PgcIndexOption("season_status", "1", "免费"),
                    PgcIndexOption("season_status", "2,6", "付费"),
                    PgcIndexOption("season_status", "4,6", "大会员")
                )
            )
        )
    }
    val selectedFilters = remember {
        mutableStateMapOf<String, PgcIndexOption>().apply {
            sections.forEach { section ->
                section.options.firstOrNull()?.let { option ->
                    put(section.field, option)
                }
            }
        }
    }

    BVTheme {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            IndexFilter(
                type = pgcType,
                show = true,
                onDismissRequest = { },
                sections = sections,
                selectedFilters = selectedFilters,
                onFilterChange = { option -> selectedFilters[option.field] = option },
                onResetFilters = {
                    selectedFilters.clear()
                    sections.forEach { section ->
                        section.options.firstOrNull()?.let { option ->
                            selectedFilters[section.field] = option
                        }
                    }
                }
            )
        }
    }
}