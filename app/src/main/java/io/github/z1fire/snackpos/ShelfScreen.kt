package io.github.z1fire.snackpos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ease = CubicBezierEasing(.2f, .8f, .2f, 1f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(vm: ShelfViewModel) {
    BackHandler(vm.pinOpen || vm.basket.isNotEmpty() || vm.admin) {
        when {
            vm.pinOpen -> vm.closePin()
            vm.basket.isNotEmpty() -> vm.cancel()
            else -> vm.lock()
        }
    }
    Box(Modifier.fillMaxSize().background(C.Page)) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (vm.pinOpen) Modifier.blur(6.dp) else Modifier)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 1180.dp).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Header(vm)
                if (vm.admin) AdminScreen(vm) else Shelf(vm)
                if (!vm.admin && vm.basket.isNotEmpty()) Spacer(Modifier.height(150.dp))
            }
        }
        if (!vm.admin) BasketBar(vm, Modifier.align(Alignment.BottomCenter))
        if (vm.pinOpen) PinPad(vm)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(vm: ShelfViewModel) {
    FlowRow(
        Modifier.fillMaxWidth().heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.padding(end = 12.dp)) {
            Text(
                if (vm.admin) "Snack admin" else "Grab a snack",
                Modifier.semantics { heading() },
                fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.72).sp, color = C.Ink,
            )
            Text(
                if (vm.admin) "Stock, prices and what is being taken." else "Tap snacks to add them, then take them all at once.",
                Modifier.padding(top = 4.dp), fontFamily = Inter, fontSize = 17.sp, color = C.Slate600,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            val toast = vm.toast
            AnimatedVisibility(toast != null, enter = fadeIn(tween(280)) + scaleIn(tween(280, easing = Ease), 0.96f) + slideInVertically(tween(280, easing = Ease)) { -it / 6 }, exit = fadeOut()) {
                val last = remember { mutableListOf("") }
                if (toast != null) last[0] = toast.text
                Row(
                    Modifier
                        .shadow(12.dp, RoundedCornerShape(999.dp), spotColor = C.Ink.copy(alpha = .25f))
                        .background(C.Ink, RoundedCornerShape(999.dp))
                        .padding(start = 20.dp, top = 6.dp, end = 6.dp, bottom = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(last[0], fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.White)
                    Btn(vm::undo, bg = C.Yellow, fg = C.Ink, border = null, radius = 999.dp, padding = PaddingValues(horizontal = 18.dp)) {
                        BtnText("Undo", weight = FontWeight.Bold)
                    }
                }
            }
            if (vm.admin) {
                Btn(vm::lock, bg = C.Ink, fg = Color.White, border = null, radius = 999.dp, minHeight = 48.dp, padding = PaddingValues(horizontal = 20.dp)) {
                    StrokeIcon(Icon.Lock, Color.White)
                    BtnText("Lock & exit", weight = FontWeight.Bold)
                }
            } else {
                Btn(vm::openPin, bg = Color.White, fg = C.Slate700, border = BorderStroke(1.dp, C.Slate300), radius = 999.dp, padding = PaddingValues(horizontal = 16.dp)) {
                    StrokeIcon(Icon.Lock, C.Slate700)
                    BtnText("Admin")
                }
            }
        }
    }
}

@Composable
private fun Shelf(vm: ShelfViewModel) {
    val items = vm.data.items
    val low = vm.data.lowAt
    val shape = RoundedCornerShape(32.dp)

    Column(
        Modifier.fillMaxWidth()
            .shadow(30.dp, shape, spotColor = C.Ink.copy(alpha = .25f))
            .clip(shape)
            .background(C.Ink)
            .padding(20.dp)
            .semantics { contentDescription = "Snacks" },
    ) {
        if (items.isEmpty()) {
            Text(
                "No snacks yet. Add some in Admin.", Modifier.fillMaxWidth().padding(vertical = 120.dp),
                color = C.Slate300, fontFamily = Inter, fontSize = 18.sp, textAlign = TextAlign.Center,
            )
        } else {
            // Two columns still fit on a phone; tablets get wider cards.
            val narrow = LocalConfiguration.current.screenWidthDp < 600
            Grid(items, min = if (narrow) 130.dp else 190.dp, gap = if (narrow) 12.dp else 16.dp) { s, m ->
                SnackCard(s, low, vm.basket[s.id] ?: 0, m, onAdd = { vm.pick(s.id) }, onRemove = { vm.unpick(s.id) })
            }
        }
    }
}

@Composable
private fun SnackCard(s: Snack, low: Int, qty: Int, modifier: Modifier, onAdd: () -> Unit, onRemove: () -> Unit) {
    val out = s.stock <= 0
    val isLow = !out && s.stock <= low
    val full = qty >= s.stock
    val selected = qty > 0
    val shape = RoundedCornerShape(24.dp)
    val grow by animateFloatAsState(if (selected) 1.02f else 1f, tween(250, easing = Ease), label = "grow")
    val fade by animateFloatAsState(if (out) .5f else 1f, tween(300), label = "fade")
    val sat by animateFloatAsState(if (out) 0f else 1f, tween(300), label = "sat")
    val ring by animateFloatAsState(if (selected) 1f else 0f, tween(250, easing = Ease), label = "ring")
    val label = when {
        out -> "${s.name}, ${money(s.price)}, all gone"
        selected -> "Add another ${s.name}, ${money(s.price)}, $qty picked, ${s.stock} left"
        else -> "Add ${s.name}, ${money(s.price)}, ${s.stock} left"
    }

    Box(modifier.graphicsLayer { scaleX = grow; scaleY = grow; alpha = fade }) {
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (ring > 0f) {
                        val r = 4.dp.toPx() * ring
                        drawRoundRect(C.Yellow, Offset(-r, -r), Size(size.width + 2 * r, size.height + 2 * r), CornerRadius(24.dp.toPx() + r))
                    }
                }
                .shadow(if (selected) 20.dp else 12.dp, shape, spotColor = Color.Black.copy(alpha = .35f))
                .clip(shape)
                .background(Color.White)
                .saturation(sat)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !out && !full, role = Role.Button, onClickLabel = label, onClick = onAdd,
                )
                .semantics { contentDescription = label },
        ) {
            PhotoOrInitial(s, 64.sp, Modifier.fillMaxWidth().height(130.dp)) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.TopEnd).offset(30.dp, (-30).dp).size(110.dp).background(Color.White.copy(alpha = .14f), CircleShape))
                    Box(Modifier.align(Alignment.BottomStart).offset((-24).dp, 36.dp).size(90.dp).background(Color.Black.copy(alpha = .10f), CircleShape))
                }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(s.name, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 22.sp, color = C.Ink, maxLines = 2, minLines = 2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(money(s.price), fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = C.Ink)
                    Pill(
                        if (out) "All gone" else if (isLow) "Only ${s.stock}" else "${s.stock} left",
                        if (out) Tone.Gray else if (isLow) Tone.Amber else Tone.Green,
                    )
                }
            }
        }
        if (selected) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(10.dp).size(44.dp)
                    .shadow(8.dp, CircleShape).background(C.Yellow, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("$qty", fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.Ink) }
            Box(
                Modifier.align(Alignment.TopStart).padding(10.dp).size(44.dp)
                    .shadow(8.dp, CircleShape).clip(CircleShape).background(C.Ink.copy(alpha = .85f))
                    .clickable(role = Role.Button, onClick = onRemove)
                    .semantics { contentDescription = "Remove one ${s.name}" },
                contentAlignment = Alignment.Center,
            ) { Text("−", fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Color.White) }
        }
    }
}

/** Sticks to the bottom of the screen while anything is picked. */
@Composable
private fun BasketBar(vm: ShelfViewModel, modifier: Modifier) {
    val lines = vm.lines
    androidx.compose.animation.AnimatedVisibility(
        lines.isNotEmpty(), modifier,
        enter = fadeIn(tween(250)) + slideInVertically(tween(300, easing = Ease)) { it / 2 },
        exit = fadeOut(tween(150)) + slideOutVertically(tween(200)) { it / 2 },
    ) {
        val last = remember { mutableListOf<Pair<Snack, Int>>() }
        if (lines.isNotEmpty()) { last.clear(); last.addAll(lines) }
        val count = last.sumOf { it.second }
        val total = last.sumOf { (s, q) -> s.price * q }
        BoxWithConstraints(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val stack = maxWidth < 560.dp
            val summary: @Composable (Modifier) -> Unit = { m ->
                Column(m) {
                    Text(
                        "$count ${if (count == 1) "snack" else "snacks"} · ${money(total)}",
                        fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text(
                        last.joinToString(", ") { (s, q) -> if (q > 1) "$q× ${s.name}" else s.name },
                        fontFamily = Inter, fontSize = 15.sp, color = C.Slate300, maxLines = 2,
                    )
                }
            }
            val take: @Composable (Modifier) -> Unit = { m ->
                Btn(
                    vm::take, m, bg = C.Yellow, fg = C.Ink, border = null, radius = 18.dp, minHeight = 64.dp, padding = PaddingValues(horizontal = 24.dp),
                ) { BtnText("Take ${if (count == 1) "it" else "all"} · ${money(total)}", size = 20.sp, weight = FontWeight.Bold) }
            }
            val clear: @Composable (Modifier) -> Unit = { m ->
                Btn(
                    vm::cancel, m, bg = Color.Transparent, fg = Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = .35f)),
                    radius = 18.dp, minHeight = 64.dp, padding = PaddingValues(horizontal = 20.dp),
                ) { BtnText("Clear", size = 18.sp) }
            }
            Box(
                Modifier.widthIn(max = 1140.dp).fillMaxWidth()
                    .shadow(30.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = .5f))
                    .background(C.Ink, RoundedCornerShape(26.dp))
                    .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(26.dp))
                    .padding(16.dp),
            ) {
                if (stack) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        summary(Modifier.padding(horizontal = 4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { take(Modifier.weight(1f)); clear(Modifier) }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        summary(Modifier.weight(1f).padding(start = 8.dp))
                        clear(Modifier)
                        take(Modifier)
                    }
                }
            }
        }
    }
}


@Composable
private fun PinPad(vm: ShelfViewModel) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(vm.pinErrorTick) {
        if (vm.pinErrorTick > 0) shake.animateTo(0f, keyframes {
            durationMillis = 400
            -10f at 80; 10f at 160; -6f at 240; 6f at 320
        })
    }
    Box(
        Modifier.fillMaxSize()
            .background(C.Ink.copy(alpha = .7f))
            .clickable(remember { MutableInteractionSource() }, null) {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 360.dp).fillMaxWidth()
                .shadow(40.dp, RoundedCornerShape(28.dp), spotColor = Color.Black.copy(alpha = .4f))
                .background(Color.White, RoundedCornerShape(28.dp))
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 24.dp)
                .semantics { contentDescription = "Enter admin PIN" },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            H2("Admin PIN", 24.sp)
            Row(
                Modifier.heightIn(min = 22.dp).graphicsLayer { translationX = shake.value.dp.toPx() },
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(vm.data.pin.length) { i ->
                    val filled = i < vm.pinEntry.length
                    Box(Modifier.size(18.dp).clip(CircleShape).background(if (filled) C.Ink else Color.Transparent).border(2.dp, C.Ink, CircleShape))
                }
            }
            Text(
                if (vm.pinError) "Wrong PIN. Try again." else "", Modifier.heightIn(min = 20.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = C.Red, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, fontFamily = Inter,
            )
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "close", "0", "back")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { k ->
                            val plain = k == "close" || k == "back"
                            Btn(
                                { when (k) { "close" -> vm.closePin(); "back" -> vm.backspacePin(); else -> vm.pressDigit(k) } },
                                Modifier.weight(1f).height(68.dp)
                                    .then(if (k == "back") Modifier.semantics { contentDescription = "Delete last digit" } else Modifier),
                                bg = if (plain) Color.White else C.Soft, fg = C.Ink, radius = 18.dp, minHeight = 68.dp,
                            ) {
                                when (k) {
                                    "close" -> BtnText("Cancel", 16.sp)
                                    "back" -> Text("⌫", fontSize = 26.sp, color = C.Ink)
                                    else -> Text(k, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = C.Ink)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
