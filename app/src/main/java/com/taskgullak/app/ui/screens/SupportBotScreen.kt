package com.taskgullak.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskgullak.app.data.model.SupportBotMessage
import com.taskgullak.app.ui.theme.InterFontFamily
import com.taskgullak.app.ui.theme.PoppinsFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportBotBottomSheet(
    chatMessages: List<SupportBotMessage>,
    userEmail: String,
    referredByCode: String,
    notificationsEnabled: Boolean,
    onSendMessage: (String) -> Unit,
    onConfirmAction: (actionType: String, actionData: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFCFBFF),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        SupportBotContent(
            chatMessages = chatMessages,
            userEmail = userEmail,
            referredByCode = referredByCode,
            notificationsEnabled = notificationsEnabled,
            onSendMessage = onSendMessage,
            onConfirmAction = onConfirmAction,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun SupportBotContent(
    chatMessages: List<SupportBotMessage>,
    userEmail: String,
    referredByCode: String,
    notificationsEnabled: Boolean,
    onSendMessage: (String) -> Unit,
    onConfirmAction: (actionType: String, actionData: String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(580.dp)
            .padding(horizontal = 18.dp)
            .padding(bottom = 20.dp)
            .testTag("support_bot_content"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7B3FE4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Gullak Assistant AI",
                        color = Color(0xFF17142E),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Online • Gullak Admin Synced",
                            color = Color(0xFF6F6E8A),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close Bot",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Quick Action Suggestion Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                BotActionChip(
                    label = "🎟️ Raise Ticket",
                    onClick = { onSendMessage("I want to raise a support ticket") }
                )
            }
            item {
                BotActionChip(
                    label = if (notificationsEnabled) "🔔 Mute Notifications" else "🔔 Turn ON Notifications",
                    onClick = { onSendMessage("Toggle notifications") }
                )
            }
            item {
                BotActionChip(
                    label = "💰 Check Wallet & Tasks",
                    onClick = { onSendMessage("Check my wallet balance and tasks") }
                )
            }
        }

        // Chat Bubble Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SupportAgent,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Hello! I am your Gullak Support Assistant.",
                        color = Color(0xFF334155),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = "Ask anything or tap a quick chip above to manage your account.",
                        color = Color(0xFF64748B),
                        fontFamily = InterFontFamily,
                        fontSize = 11.5.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chatMessages, key = { it.id.ifBlank { "${it.sender}_${it.text.hashCode()}_${it.createdAt}" } }) { msg ->
                        SupportBotMessageItem(
                            message = msg,
                            onConfirmAction = onConfirmAction
                        )
                    }
                }
            }
        }

        // Bottom Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Type your question or issue...",
                        color = Color(0xFF94A3B8),
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    textStyle = TextStyle(
                        color = Color(0xFF17142E),
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF7B3FE4)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("support_bot_text_input")
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF7B3FE4))
                    .clickable {
                        val clean = inputText.trim()
                        if (clean.isNotBlank()) {
                            onSendMessage(clean)
                            inputText = ""
                        }
                    }
                    .testTag("support_bot_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BotActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF334155),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SupportBotMessageItem(
    message: SupportBotMessage,
    onConfirmAction: (actionType: String, actionData: String) -> Unit
) {
    val isUser = message.sender.equals("user", ignoreCase = true)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7B3FE4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) Color(0xFF7B3FE4) else Color.White)
                    .border(
                        1.dp,
                        if (isUser) Color(0xFF6D28D9) else Color(0xFFE2E8F0),
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else Color(0xFF1E293B),
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // In-line Confirmation Card (Mandatory Confirm Button before executing action)
            if (!isUser && message.actionType.isNotBlank() && message.actionType != "none") {
                Spacer(modifier = Modifier.height(6.dp))
                ActionConfirmationCard(
                    actionType = message.actionType,
                    actionData = message.actionData,
                    actionExecuted = message.actionExecuted,
                    onConfirm = {
                        onConfirmAction(message.actionType, message.actionData)
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionConfirmationCard(
    actionType: String,
    actionData: String,
    actionExecuted: Boolean,
    onConfirm: () -> Unit
) {
    val titleText = when (actionType) {
        "confirm_remove_referral" -> "Action Required: Remove Referral Code"
        "confirm_toggle_notifications" -> "Action Required: Toggle Notifications"
        "confirm_raise_ticket" -> "Action Required: Raise Support Ticket"
        else -> "Action Required: Confirm Changes"
    }

    val bodyText = when (actionType) {
        "confirm_remove_referral" -> "Are you sure you want to unlink referral code ${actionData.ifBlank { "from your account" }}?"
        "confirm_toggle_notifications" -> "Are you sure you want to update your system notification settings?"
        "confirm_raise_ticket" -> "Confirm creating support ticket: \"${actionData.take(60)}\"?"
        else -> "Please confirm if you want to proceed with this account modification."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFEF3C7))
            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = titleText,
                color = Color(0xFF92400E),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp
            )
            Text(
                text = bodyText,
                color = Color(0xFF78350F),
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            if (actionExecuted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Confirmed & Executed",
                        color = Color(0xFF15803D),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Confirm",
                            color = Color.White,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
