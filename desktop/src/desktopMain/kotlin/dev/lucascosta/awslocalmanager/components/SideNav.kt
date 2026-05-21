package dev.lucascosta.awslocalmanager.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.lucascosta.awslocalmanager.constants.AppConstants.SIDE_NAV_WIDTH
import dev.lucascosta.awslocalmanager.data.model.navigation.NavGroup
import dev.lucascosta.awslocalmanager.data.model.navigation.NavItem
import dev.lucascosta.awslocalmanager.i18n.LocalStrings
import dev.lucascosta.awslocalmanager.navigation.Screen

@Composable
fun SideNav(
    currentScreen: Screen,
    isExpanded: Boolean,
    onScreenSelected: (Screen) -> Unit,
    onToggleExpand: () -> Unit,
) {
    val strings = LocalStrings.current

    val groups =
        listOf(
            NavGroup(
                titleFn = { strings.navGroupMonitoring },
                items =
                    listOf(
                        NavItem(Screen.Dashboard, Icons.Outlined.Dashboard) { strings.navDashboard },
                        NavItem(Screen.Running, Icons.Outlined.PlayCircle) { strings.navRunning },
                        NavItem(Screen.Inspector, Icons.Outlined.Search) { strings.navInspector },
                    ),
            ),
            NavGroup(
                titleFn = { strings.navGroupInfrastructure },
                items =
                    listOf(
                        NavItem(Screen.Project, Icons.Outlined.Folder) { strings.navProject },
                        NavItem(Screen.Quick, Icons.Outlined.Add) { strings.navQuick },
                    ),
            ),
            NavGroup(
                titleFn = { strings.navGroupTools },
                items =
                    listOf(
                        NavItem(Screen.Setup, Icons.Outlined.MonitorHeart) { strings.navSetup },
                        NavItem(Screen.Settings, Icons.Outlined.Settings) { strings.navSettings },
                    ),
            ),
        )

    val width by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else 64.dp,
        animationSpec = tween(200),
        label = SIDE_NAV_WIDTH,
    )

    Surface(
        modifier = Modifier.width(width).fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 8.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true),
                            onClick = onToggleExpand,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.AutoMirrored.Outlined.MenuOpen else Icons.Outlined.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(4.dp))

            groups.forEach { group ->
                if (isExpanded) {
                    Text(
                        group.titleFn(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }

                group.items.forEach { item ->
                    NavItemRow(
                        item = item,
                        isSelected = isNavItemSelected(item, currentScreen),
                        isExpanded = isExpanded,
                        onClick = { onScreenSelected(item.screen) },
                    )
                }
            }
        }
    }
}

private fun isNavItemSelected(
    item: NavItem,
    currentScreen: Screen,
): Boolean =
    when {
        item.screen == Screen.Project && currentScreen is Screen.Infrastructure -> true
        else -> currentScreen == item.screen
    }

@Composable
private fun NavItemRow(
    item: NavItem,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    val label = item.labelFn()
    val bgColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onClick,
                )
                .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = label,
            tint =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
        )

        if (isExpanded) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}
