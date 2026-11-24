package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfile
import com.example.data.WithdrawMethod
import com.example.data.WithdrawRequest
import com.example.data.WithdrawStatus
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.EasypaisaGreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.GlassCardHighlight
import com.example.ui.theme.GlassCardSurface
import com.example.ui.theme.JazzCashCrimson
import com.example.ui.theme.MicLiveEmerald
import com.example.ui.theme.MicMutedRed
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoyalGold
import com.example.ui.theme.StageNightBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VelvetPlumSurface

@Composable
fun WalletScreen(
    userProfile: UserProfile?,
    requestsState: UiState<List<WithdrawRequest>>,
    isBusy: Boolean,
    isAdmin: Boolean,
    onConvertDiamonds: (Long) -> Unit,
    onSubmitWithdraw: (WithdrawMethod, String, String, () -> Unit) -> Unit,
    onOpenAdmin: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var showWithdrawForm by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf(WithdrawMethod.EASYPAISA) }
    var accountNumber by remember { mutableStateOf("") }
    var coinsAmountText by remember { mutableStateOf("1000") }

    val parsedAmount = coinsAmountText.trim().toLongOrNull() ?: 0L
    val currentCoins = userProfile?.coins ?: 0L
    val currentDiamonds = userProfile?.diamonds ?: 0L
    val isAmountValid = parsedAmount >= 1000L && parsedAmount <= currentCoins
    val isNumberValid = accountNumber.trim().matches(Regex("^03[0-9]{9}$"))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(StageNightBg, VelvetPlumSurface, StageNightBg)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = GlassCardSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clickable { onBack() }
                        .testTag("wallet_back_button")
                ) {
                    Text(
                        text = "⬅️ Wapis",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Text(
                    text = "👛 Mera Wallet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )

                if (isAdmin) {
                    Surface(
                        color = RoyalGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, RoyalGold, RoundedCornerShape(50))
                            .clickable { onOpenAdmin() }
                            .testTag("wallet_admin_button")
                    ) {
                        Text(
                            text = "🛡️ Admin",
                            style = MaterialTheme.typography.labelMedium,
                            color = RoyalGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(64.dp))
                }
            }

            // Balance Hero Card
            Surface(
                color = GlassCardHighlight,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, RoyalGold.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "💰 Kul Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BalanceStatBlock(
                            emoji = "🪙",
                            label = "Coins Balance",
                            value = "$currentCoins",
                            subValue = "= $currentCoins Rs",
                            accent = RoyalGold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        BalanceStatBlock(
                            emoji = "💎",
                            label = "Diamonds (Gifts)",
                            value = "$currentDiamonds",
                            subValue = "1 💎 = 1 Coin",
                            accent = CyanGlow,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💵 Ab tak kamayi: Rs. ${userProfile?.totalEarnedRs ?: 0}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MicLiveEmerald
                            )
                            Text(
                                text = "1000 Coins = 1000 Rs",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalGold
                            )
                        }
                    }

                    if (currentDiamonds > 0L) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onConvertDiamonds(currentDiamonds) },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("convert_diamonds_button")
                        ) {
                            Text(
                                text = "💎 $currentDiamonds Diamonds ko Coins me Badlein 🪙",
                                color = CyanGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { showWithdrawForm = !showWithdrawForm },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("open_withdraw_button")
                    ) {
                        Text(
                            text = if (showWithdrawForm) "🔽 Withdraw Form Band Karein" else "💸 Withdraw Karein (Easypaisa / JazzCash)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Withdraw Form (2 options: Easypaisa, JazzCash)
            if (showWithdrawForm) {
                Surface(
                    color = GlassCardSurface,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonMagenta.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🏦 Payment Method Chunein",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PaymentMethodCard(
                                method = WithdrawMethod.EASYPAISA,
                                selected = selectedMethod == WithdrawMethod.EASYPAISA,
                                accentColor = EasypaisaGreen,
                                onClick = { selectedMethod = WithdrawMethod.EASYPAISA },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("method_easypaisa")
                            )
                            PaymentMethodCard(
                                method = WithdrawMethod.JAZZCASH,
                                selected = selectedMethod == WithdrawMethod.JAZZCASH,
                                accentColor = JazzCashCrimson,
                                onClick = { selectedMethod = WithdrawMethod.JAZZCASH },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("method_jazzcash")
                            )
                        }

                        Text(
                            text = "${selectedMethod.emoji} ${selectedMethod.label} Mobile Number",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { if (it.length <= 11) accountNumber = it.filter(Char::isDigit) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("03001234567") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonMagenta,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_number_input")
                        )

                        Text(
                            text = "🪙 Kitne Coins Withdraw Karne Hain? (Kam az kam 1000)",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = coinsAmountText,
                            onValueChange = { coinsAmountText = it.filter(Char::isDigit) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("1000") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RoyalGold,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdraw_amount_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1000L, 2000L, 5000L).forEach { preset ->
                                Surface(
                                    color = if (parsedAmount == preset) ElectricViolet else GlassCardHighlight,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { coinsAmountText = preset.toString() }
                                ) {
                                    Text(
                                        text = "$preset 🪙",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Validation messages in Roman Urdu
                        if (parsedAmount in 1..999 || coinsAmountText.isBlank()) {
                            Surface(
                                color = MicMutedRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Kam az kam 1000 Coins (= 1000 Rs) withdraw ho sakte hain. Is se kam withdraw nahi ho sakta.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFFB4BE),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else if (parsedAmount > currentCoins) {
                            Surface(
                                color = MicMutedRed.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ Aap ke Wallet me sirf $currentCoins Coins mojud hain.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFFB4BE),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            Surface(
                                color = MicLiveEmerald.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💵 Aap ko Rs. $parsedAmount milenge (${selectedMethod.label}). Request Admin approve karega to paisa transfer hoga.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MicLiveEmerald,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onSubmitWithdraw(selectedMethod, accountNumber, coinsAmountText) {
                                    accountNumber = ""
                                    showWithdrawForm = false
                                }
                            },
                            enabled = !isBusy && isAmountValid && isNumberValid,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EasypaisaGreen,
                                disabledContainerColor = Color.White.copy(alpha = 0.12f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("submit_withdraw_button")
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp).width(20.dp),
                                    color = TextPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Withdraw Request Bhejein 🚀",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Withdraw History
            Text(
                text = "📜 Meri Withdraw Requests",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            when (requestsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(color = NeonMagenta)
                }
                is UiState.Error -> {
                    Text("⚠️ ${requestsState.message}", color = MicMutedRed)
                }
                is UiState.Success -> {
                    if (requestsState.data.isEmpty()) {
                        Surface(
                            color = GlassCardSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Abhi koi withdraw request nahi bheji gayi. Kam az kam 1000 Coins hone par Easypaisa ya JazzCash se nikalwayein! 💸",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        requestsState.data.forEach { req ->
                            WithdrawRequestCard(
                                request = req,
                                showActions = false,
                                onApprove = {},
                                onReject = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceStatBlock(
    emoji: String,
    label: String,
    value: String,
    subValue: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = "$emoji $label", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(text = subValue, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
private fun PaymentMethodCard(
    method: WithdrawMethod,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.24f) else GlassCardHighlight,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .heightIn(min = 64.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accentColor else Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = method.emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = method.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AdminApprovalScreen(
    requestsState: UiState<List<WithdrawRequest>>,
    isBusy: Boolean,
    onApprove: (WithdrawRequest) -> Unit,
    onReject: (WithdrawRequest) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(StageNightBg, VelvetPlumSurface, StageNightBg)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .navigationBarsPadding()
                .widthIn(max = 640.dp)
                .align(Alignment.Center)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = GlassCardSurface,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .clickable { onBack() }
                        .testTag("admin_back_button")
                ) {
                    Text(
                        text = "⬅️ Wapis",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Text(
                    text = "🛡️ Admin Panel",
                    style = MaterialTheme.typography.headlineSmall,
                    color = RoyalGold
                )

                Spacer(Modifier.width(64.dp))
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Withdraw requests check karein. Approve karne par user ko paisa transfer hoga aur Reject karne par Coins wapis milenge.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(14.dp))

            when (requestsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = RoyalGold)
                    }
                }
                is UiState.Error -> {
                    Text("⚠️ ${requestsState.message}", color = MicMutedRed)
                }
                is UiState.Success -> {
                    if (requestsState.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Abhi koi withdraw request mojud nahi hai ⏳",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(requestsState.data, key = { it.id }) { req ->
                                WithdrawRequestCard(
                                    request = req,
                                    showActions = req.status == "PENDING" && !isBusy,
                                    onApprove = { onApprove(req) },
                                    onReject = { onReject(req) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WithdrawRequestCard(
    request: WithdrawRequest,
    showActions: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val method = WithdrawMethod.fromWire(request.method)
    val status = WithdrawStatus.fromWire(request.status)
    val statusColor = when (status) {
        WithdrawStatus.PENDING -> RoyalGold
        WithdrawStatus.APPROVED -> MicLiveEmerald
        WithdrawStatus.REJECTED -> MicMutedRed
    }

    Surface(
        color = GlassCardSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${method.emoji} ${method.label} • ${request.accountNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rs. ${request.rupeesAmount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = RoyalGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "👤 ${request.userName}   •   🪙 ${request.coinsAmount} Coins",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Surface(
                color = statusColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "${status.emoji} ${status.romanUrduLabel}",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            if (request.adminNote.isNotBlank()) {
                Text(
                    text = "📝 ${request.adminNote}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            if (showActions) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = EasypaisaGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("admin_approve_${request.id}")
                    ) {
                        Text("✅ Approve", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = MicMutedRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("admin_reject_${request.id}")
                    ) {
                        Text("❌ Reject", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
