package io.github.z1fire.snackpos

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private val Ease = CubicBezierEasing(.2f, .8f, .2f, 1f)

/** Card tilt in degrees for cards beside the centre one. */
const val Tilt = 16f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun App(vm: ShelfViewModel) {
    BackHandler(vm.pinOpen || vm.selected != null || vm.admin) {
        when {
            vm.pinOpen -> vm.closePin()
            vm.selected != null -> vm.cancel()
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
            }
        }
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
                if (vm.admin) "Stock, prices and what is being taken." else "Swipe to browse, tap to pick.",
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

/** Pages either side of the start so the carousel loops in both directions. */
private const val Loop = 100_000

@Composable
private fun Shelf(vm: ShelfViewModel) {
    val items = vm.data.items
    val n = items.size
    val low = vm.data.lowAt
    val selItem = items.firstOrNull { it.id == vm.selected && it.stock > 0 }
    val pager = rememberPagerState(initialPage = if (n > 0) (Loop / 2) - (Loop / 2) % n else 0) { if (n > 0) Loop else 0 }
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(32.dp)

    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .shadow(30.dp, shape, spotColor = C.Ink.copy(alpha = .25f))
            .clip(shape)
            .background(C.Ink)
            .semantics { contentDescription = "Snacks" },
    ) {
        val cardW = min(maxWidth.value * 0.68f, 290f).dp
        val side = (maxWidth - cardW) / 2
        // Backdrop disc behind the centre card.
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = 40.dp).size(520.dp)
                .alpha(.9f).background(C.Slate800, CircleShape),
        )
        Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 28.dp)) {
            if (n == 0) {
                Text(
                    "No snacks yet. Add some in Admin.", Modifier.fillMaxWidth().padding(vertical = 120.dp),
                    color = C.Slate300, fontFamily = Inter, fontSize = 18.sp, textAlign = TextAlign.Center,
                )
            } else {
                HorizontalPager(
                    pager,
                    Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = side, end = side, top = 36.dp, bottom = 30.dp),
                    pageSize = PageSize.Fixed(cardW),
                    pageSpacing = 4.dp,
                    beyondViewportPageCount = 2,
                    userScrollEnabled = selItem == null,
                ) { page ->
                    val it = items[page % n]
                    val d = (page - pager.currentPage) - pager.currentPageOffsetFraction
                    val a = min(abs(d), 1.6f)
                    SnackCard(
                        it, low,
                        selected = selItem?.id == it.id,
                        dim = selItem != null && selItem.id != it.id,
                        modifier = Modifier.graphicsLayer {
                            val s = 1f - 0.13f * a
                            scaleX = s; scaleY = s
                            // Outer edges swing toward the viewer, as CSS rotateY(-d·tilt) does in the design.
                            rotationY = d.coerceIn(-1f, 1f) * Tilt
                            translationY = (1f - min(a, 1f)) * -10.dp.toPx()
                            alpha = 1f - 0.4f * (a / 1.6f)
                            cameraDistance = 14f * density
                            transformOrigin = TransformOrigin(.5f, .6f)
                        },
                    ) {
                        if (selItem == null && it.stock > 0) {
                            vm.pick(it.id)
                            scope.launch { pager.animateScrollToPage(page) }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    val center = pager.currentPage % n
                    items.forEachIndexed { i, _ ->
                        val on = i == center
                        val w by animateDpAsState(if (on) 28.dp else 8.dp, tween(300, easing = Ease), label = "dot")
                        Box(Modifier.height(8.dp).width(w).background(if (on) C.Yellow else Color.White.copy(alpha = .3f), RoundedCornerShape(999.dp)))
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).heightIn(min = 72.dp), contentAlignment = Alignment.Center) {
                androidx.compose.animation.AnimatedVisibility(selItem != null, enter = fadeIn(tween(300)) + slideInVertically(tween(300, easing = Ease)) { it / 7 }, exit = fadeOut(tween(120))) {
                    val last = remember { arrayOfNulls<Snack>(1) }
                    if (selItem != null) last[0] = selItem
                    val sel = last[0] ?: return@AnimatedVisibility
                    BoxWithConstraints(Modifier.widthIn(max = 580.dp).fillMaxWidth()) {
                        val stack = maxWidth < 440.dp
                        val take: @Composable (Modifier) -> Unit = { m ->
                            Btn(
                                vm::take, m.shadow(16.dp, RoundedCornerShape(20.dp), spotColor = C.Yellow.copy(alpha = .4f)),
                                bg = C.Yellow, fg = C.Ink, border = null, radius = 20.dp, minHeight = 72.dp, padding = PaddingValues(horizontal = 24.dp),
                            ) { BtnText("Take one ${sel.name} · ${money(sel.price)}", size = 21.sp, weight = FontWeight.Bold) }
                        }
                        val cancel: @Composable (Modifier) -> Unit = { m ->
                            Btn(
                                vm::cancel, m, bg = Color.Transparent, fg = Color.White, border = BorderStroke(1.dp, Color.White.copy(alpha = .35f)),
                                radius = 20.dp, minHeight = 72.dp, padding = PaddingValues(horizontal = 24.dp),
                            ) { BtnText("Cancel", size = 18.sp) }
                        }
                        if (stack) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { take(Modifier.fillMaxWidth()); cancel(Modifier.fillMaxWidth()) }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { take(Modifier.weight(1f)); cancel(Modifier) }
                        }
                    }
                }
                if (selItem == null && n > 0) {
                    Text("Tap a snack to pick it.", color = C.Slate300, fontFamily = Inter, fontSize = 18.sp)
                }
            }
        }

        if (selItem == null && n > 0) {
            listOf(Icon.Left to -1, Icon.Right to 1).forEach { (icon, dir) ->
                Box(
                    Modifier
                        .align(if (dir < 0) Alignment.TopStart else Alignment.TopEnd)
                        .padding(start = 16.dp, end = 16.dp, top = 190.dp)
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(C.Ink.copy(alpha = .6f))
                        .border(1.dp, Color.White.copy(alpha = .25f), CircleShape)
                        .clickable(role = Role.Button) { scope.launch { pager.animateScrollToPage(pager.currentPage + dir) } }
                        .semantics { contentDescription = if (dir < 0) "Previous snack" else "Next snack" },
                    contentAlignment = Alignment.Center,
                ) { StrokeIcon(icon, Color.White, 24.dp, 2.4f) }
            }
        }
    }
}

@Composable
private fun SnackCard(s: Snack, low: Int, selected: Boolean, dim: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val out = s.stock <= 0
    val isLow = !out && s.stock <= low
    val shape = RoundedCornerShape(28.dp)
    val lift by animateDpAsState(if (selected) (-8).dp else 0.dp, tween(350, easing = Ease), label = "lift")
    val grow by animateFloatAsState(if (selected) 1.05f else 1f, tween(350, easing = Ease), label = "grow")
    val fade by animateFloatAsState(if (out) .5f else if (dim) .3f else 1f, tween(300), label = "fade")
    val sat by animateFloatAsState(if (out) 0f else if (dim) .4f else 1f, tween(300), label = "sat")
    val ring by animateFloatAsState(if (selected) 1f else 0f, tween(350, easing = Ease), label = "ring")
    val label = if (out) "${s.name}, ${money(s.price)}, all gone" else "Pick ${s.name}, ${money(s.price)}, ${s.stock} left"

    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = lift.toPx(); scaleX = grow; scaleY = grow; alpha = fade }
                .drawBehind {
                    if (ring > 0f) {
                        val r = 5.dp.toPx() * ring
                        drawRoundRect(C.Yellow, Offset(-r, -r), Size(size.width + 2 * r, size.height + 2 * r), CornerRadius(28.dp.toPx() + r))
                    }
                }
                .shadow(if (selected) 26.dp else 18.dp, shape, spotColor = Color.Black.copy(alpha = if (selected) .5f else .35f))
                .clip(shape)
                .background(Color.White)
                .saturation(sat)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !out, role = Role.Button, onClickLabel = label, onClick = onPick,
                )
                .semantics { contentDescription = label },
        ) {
            PhotoOrInitial(s, 110.sp, Modifier.fillMaxWidth().height(230.dp)) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.align(Alignment.TopEnd).offset(40.dp, (-40).dp).size(160.dp).background(Color.White.copy(alpha = .14f), CircleShape))
                    Box(Modifier.align(Alignment.BottomStart).offset((-30).dp, 50.dp).size(130.dp).background(Color.Black.copy(alpha = .10f), CircleShape))
                }
            }
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(s.name, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 27.sp, color = C.Ink, maxLines = 2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(money(s.price), fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.32).sp, color = C.Ink)
                    Pill(
                        if (out) "All gone" else if (isLow) "Only ${s.stock} left" else "${s.stock} left",
                        if (out) Tone.Gray else if (isLow) Tone.Amber else Tone.Green,
                    )
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
