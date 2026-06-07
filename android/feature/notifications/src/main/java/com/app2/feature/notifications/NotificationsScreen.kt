package com.app2.feature.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app2.core.ui.ViewState
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockErrorView
import com.app2.core.ui.components.StockSkeletonList
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Error
import com.app2.core.ui.theme.Info
import com.app2.core.data.remote.dto.NotificationDTO
import com.app2.core.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Notifications") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
            },
            actions = {
                if (unreadCount > 0) {
                    TextButton(onClick = { viewModel.markAllRead() }) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Tout lu")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        when (val s = state) {
            is ViewState.Loading -> {
                StockSkeletonList(modifier = Modifier.padding(12.dp))
            }
            is ViewState.Loaded -> {
                PullToRefreshBox(
                    isRefreshing = false,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = s.data,
                            key = { it.id }
                        ) { notification ->
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if ((notification.isRead ?: 0) == 0) {
                                        viewModel.markRead(notification.id)
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
            is ViewState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NotificationImportant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = "Aucune notification",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is ViewState.Error -> {
                StockErrorView(
                    message = s.message,
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationDTO,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val iconColor: Color
    when (notification.type) {
        "low_stock", "out_of_stock" -> {
            icon = Icons.Default.Warning
            iconColor = Error
        }
        "reorder" -> {
            icon = Icons.Default.Inventory2
            iconColor = Info
        }
        else -> {
            icon = Icons.Default.NotificationImportant
            iconColor = Accent
        }
    }

    StockCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if ((notification.isRead ?: 0) == 0) {
                        Modifier.background(
                            color = Accent.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else Modifier
                )
                .padding(4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title ?: notification.type,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if ((notification.isRead ?: 0) != 0) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if ((notification.isRead ?: 0) == 0) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Error, RoundedCornerShape(4.dp))
                        )
                    }
                }
                if (notification.message != null) {
                    Text(
                        text = notification.message ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (notification.createdAt != null) {
                    Text(
                        text = (notification.createdAt ?: "").take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
