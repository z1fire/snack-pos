package io.github.z1fire.snackpos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(vm: ShelfViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val shape = RoundedCornerShape(14.dp)
        FlowRow(
            Modifier.background(Color.White, shape).border(1.dp, C.Line, shape).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AdminTab.entries.forEach { t ->
                val active = vm.tab == t
                Btn(
                    { vm.goTab(t) }, bg = if (active) C.Ink else Color.Transparent, fg = if (active) Color.White else C.Slate700,
                    border = null, padding = PaddingValues(horizontal = 18.dp),
                ) { BtnText(t.label) }
            }
        }
        AnimatedContent(
            vm.tab,
            transitionSpec = { (fadeIn(tween(300)) + slideInVertically(tween(300)) { 30 }) togetherWith fadeOut(tween(90)) },
            label = "tab",
        ) { tab ->
            when (tab) {
                AdminTab.Stock -> StockTab(vm)
                AdminTab.Count -> CountTab(vm)
                AdminTab.Snacks -> SnacksTab(vm)
                AdminTab.Insights -> InsightsTab(vm)
                AdminTab.Settings -> SettingsTab(vm)
            }
        }
    }
}

/** The design's two-panel rows: side by side when there's room, else stacked. */
@Composable
private fun Split(minMain: Dp, minSide: Dp, sideGrows: Boolean, main: @Composable (Modifier) -> Unit, side: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= minMain + minSide + 16.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                main(Modifier.weight(1f))
                side(if (sideGrows) Modifier.weight(minSide / minMain) else Modifier.width(minSide))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                main(Modifier.fillMaxWidth())
                side(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Fills rows with as many columns of at least [min] as fit. With [stretch], items share each
 * row's full width (CSS auto-fit / flex-wrap); without, short rows keep empty slots (auto-fill).
 */
@Composable
private fun <T> Grid(items: List<T>, min: Dp, gap: Dp, stretch: Boolean = false, extra: (@Composable (Modifier) -> Unit)? = null, cell: @Composable (T, Modifier) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val all: List<Any?> = items + if (extra != null) listOf(Unit) else emptyList()
        val fit = maxOf(1, ((maxWidth + gap) / (min + gap)).toInt())
        val cols = if (stretch) minOf(fit, maxOf(1, all.size)) else fit
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            all.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach {
                        @Suppress("UNCHECKED_CAST")
                        if (it === Unit && extra != null) extra(Modifier.weight(1f)) else cell(it as T, Modifier.weight(1f))
                    }
                    if (!stretch) repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A table that scrolls sideways inside its panel once narrower than [minWidth]. */
@Composable
private fun Table(minWidth: Dp, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = if (maxWidth > minWidth) maxWidth else minWidth
        Box(Modifier.horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(w), content = content)
        }
    }
}

/** A table column: shares leftover width by [weight], or is fixed at [width] when weight is 0. */
private data class Col(val label: String, val weight: Float = 0f, val width: Dp = 0.dp)

private fun RowScope.cell(c: Col) = if (c.weight > 0f) Modifier.weight(c.weight) else Modifier.width(c.width)

@Composable
private fun HeadRow(cols: List<Col>, centered: Set<Int> = emptySet(), end: Set<Int> = emptySet()) {
    Row(Modifier.fillMaxWidth().drawBehind { drawLine(C.Line, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }) {
        cols.forEachIndexed { i, c ->
            Text(
                c.label.uppercase(), cell(c).padding(horizontal = 8.dp, vertical = 10.dp),
                fontFamily = Inter, fontSize = 12.sp, letterSpacing = 0.72.sp, fontWeight = FontWeight.SemiBold, color = C.Slate600,
                textAlign = if (i in centered) TextAlign.Center else if (i in end) TextAlign.End else TextAlign.Start,
            )
        }
    }
}

@Composable
private fun BodyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().drawBehind { drawLine(C.Row, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, content = content,
    )
}

@Composable
private fun RowScope.SnackCell(s: Snack, c: Col) {
    Row(cell(c).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumb(s)
        Text(s.name, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = C.Ink)
    }
}

@Composable
private fun BigNum(text: String, modifier: Modifier) =
    Text(text, modifier, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.Ink, textAlign = TextAlign.Center)

@Composable
private fun SmallBtn(text: String, desc: String, onClick: () -> Unit, enabled: Boolean = true, minW: Dp = 44.dp, weight: FontWeight = FontWeight.Normal, border: Color = C.Line, bg: Color = C.Soft) =
    Btn(onClick, Modifier.widthIn(min = minW).semantics { contentDescription = desc }, enabled, bg = bg, border = BorderStroke(1.dp, border)) {
        BtnText(text, 15.sp, weight)
    }

private fun need(s: Snack) = maxOf(0, s.par - s.stock)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockTab(vm: ShelfViewModel) {
    val items = vm.data.items
    val shopping = items.filter { need(it) > 0 }
    Split(640.dp, 280.dp, false, main = { m ->
        Panel(m) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(12.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.padding(end = 12.dp)) {
                    H2("Stock & par levels")
                    Muted("Par is how many you want on the shelf after restocking.", modifier = Modifier.padding(top = 4.dp))
                }
                DarkBtn("Restock all to par", vm::fillAll, enabled = shopping.isNotEmpty())
            }
            val cols = listOf(Col("Snack", 1f), Col("On shelf", width = 84.dp), Col("Par", width = 140.dp), Col("Status", width = 184.dp), Col("Adjust stock", 1.6f))
            Table(760.dp) {
                HeadRow(cols, centered = setOf(1, 2))
                items.forEach { s ->
                    val out = s.stock <= 0
                    val n = need(s)
                    BodyRow {
                        SnackCell(s, cols[0])
                        BigNum("${s.stock}", cell(cols[1]))
                        Row(cell(cols[2]), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                            Btn({ vm.parBy(s.id, -1) }, Modifier.width(40.dp).semantics { contentDescription = "Lower par for ${s.name}" }, padding = PaddingValues(0.dp)) { BtnText("−", 18.sp, FontWeight.Normal) }
                            Text("${s.par}", Modifier.widthIn(min = 28.dp), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, color = C.Ink)
                            Btn({ vm.parBy(s.id, 1) }, Modifier.width(40.dp).semantics { contentDescription = "Raise par for ${s.name}" }, padding = PaddingValues(0.dp)) { BtnText("+", 18.sp, FontWeight.Normal) }
                        }
                        Box(cell(cols[3]).padding(horizontal = 8.dp)) {
                            Pill(
                                if (out) "Out · buy $n" else if (n > 0) "Below par · buy $n" else "At par",
                                if (out) Tone.Red else if (n > 0) Tone.Amber else Tone.Green,
                            )
                        }
                        FlowRow(cell(cols[4]).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SmallBtn("−1", "Remove one ${s.name} from stock", { vm.setStock(s.id, -1) }, enabled = !out)
                            SmallBtn("+1", "Add one ${s.name} to stock", { vm.setStock(s.id, 1) })
                            SmallBtn("+12", "Add 12 ${s.name}", { vm.setStock(s.id, 12) }, minW = 52.dp, weight = FontWeight.SemiBold)
                            SmallBtn("Fill to par", "Fill ${s.name} to par", { vm.fill(s.id) }, enabled = n > 0, weight = FontWeight.SemiBold, border = C.Ink, bg = Color.White)
                        }
                    }
                }
            }
        }
    }, side = { m ->
        Panel(m, gap = 12.dp) {
            H2("Shopping list")
            if (shopping.isEmpty()) Muted("Everything is at par. Nothing to buy.", 16.sp)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                shopping.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().drawBehind { drawLine(C.Row, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(s.name, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Ink)
                        Text("buy ${need(s)}", fontFamily = Inter, fontSize = 16.sp, color = C.Slate700)
                    }
                }
            }
            if (shopping.isNotEmpty()) Muted("${shopping.sumOf { need(it) }} units to reach par.")
        }
    })
}

@Composable
private fun CountTab(vm: ShelfViewModel) {
    val items = vm.data.items
    var entered = 0; var missing = 0; var missingVal = 0.0; var extra = 0
    items.forEach { s ->
        val num = vm.counts[s.id]?.toIntOrNull() ?: return@forEach
        val diff = maxOf(0, num) - s.stock
        entered++
        if (diff < 0) { missing -= diff; missingVal -= diff * s.price }
        if (diff > 0) extra += diff
    }
    val summary = if (entered == 0) "Nothing counted yet." else
        "$entered of ${items.size} counted · $missing missing (${money(missingVal)})" + if (extra > 0) " · $extra extra" else ""

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Panel {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth > 640.dp
                val title: @Composable (Modifier) -> Unit = { m ->
                    Column(m) {
                        H2("Count the shelf")
                        Muted("Count what is physically there and type it in. Anything lower than the app expects shows as missing.", modifier = Modifier.padding(top = 4.dp))
                    }
                }
                val status: @Composable () -> Unit = {
                    Text(summary, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Slate700)
                }
                if (wide) Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) { title(Modifier.weight(1f)); status() }
                else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { title(Modifier); status() }
            }
            val cols = listOf(Col("Snack", 1f), Col("App says", width = 100.dp), Col("Actual count", width = 136.dp), Col("Difference", width = 156.dp), Col("Value", .6f))
            Table(640.dp) {
                HeadRow(cols, centered = setOf(1, 2), end = setOf(4))
                items.forEach { s ->
                    val raw = vm.counts[s.id]
                    val num = raw?.toIntOrNull()
                    val diff = if (num != null) maxOf(0, num) - s.stock else 0
                    BodyRow {
                        SnackCell(s, cols[0])
                        BigNum("${s.stock}", cell(cols[1]))
                        Box(cell(cols[2]), contentAlignment = Alignment.Center) {
                            Field(
                                raw ?: "", { vm.setCount(s.id, it) },
                                Modifier.width(88.dp).semantics { contentDescription = "Actual count of ${s.name}" },
                                placeholder = "–", height = 52.dp, borderWidth = 2.dp, radius = 12.dp,
                                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
                                style = TextStyle(fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = C.Ink, textAlign = TextAlign.Center),
                            )
                        }
                        Box(cell(cols[3]).padding(horizontal = 8.dp)) {
                            when {
                                num == null -> Text("Not counted", fontFamily = Inter, fontSize = 14.sp, color = C.Slate500)
                                diff == 0 -> Pill("Matches", Tone.Green)
                                diff < 0 -> Pill("${-diff} missing", Tone.Red)
                                else -> Pill("$diff extra", Tone.Blue)
                            }
                        }
                        Text(
                            if (num != null && diff != 0) (if (diff < 0) "−" else "+") + money(kotlin.math.abs(diff) * s.price) else "",
                            cell(cols[4]).padding(horizontal = 8.dp),
                            fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.End, color = C.Ink,
                        )
                    }
                }
            }
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalArrangement = Arrangement.spacedBy(10.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                if (vm.countMsg.isNotEmpty()) {
                    Text(vm.countMsg, Modifier.weight(1f).padding(end = 10.dp), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Green)
                }
                Btn(vm::clearCounts, enabled = entered > 0, bg = Color.White, border = BorderStroke(1.dp, C.Slate300), radius = 12.dp, minHeight = 48.dp, padding = PaddingValues(horizontal = 18.dp)) { BtnText("Clear entries") }
                DarkBtn("Save count & fix stock", vm::saveCount, enabled = entered > 0)
            }
        }

        val shrink = linkedMapOf<String, Pair<Int, Double>>()
        var allMissing = 0; var allMissingVal = 0.0
        val fmt = remember { SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault()) }
        val audits = vm.data.audits.map { a ->
            var miss = 0; var v = 0.0
            val parts = mutableListOf<String>()
            a.lines.forEach { l ->
                if (l.diff < 0) {
                    miss -= l.diff; v -= l.diff * l.price
                    val (u, x) = shrink[l.name] ?: (0 to 0.0)
                    shrink[l.name] = (u - l.diff) to (x - l.diff * l.price)
                }
                if (l.diff != 0) parts += l.name + " " + (if (l.diff > 0) "+" else "−") + kotlin.math.abs(l.diff)
            }
            allMissing += miss; allMissingVal += v
            Triple(a, miss to v, parts)
        }
        Split(460.dp, 340.dp, false, main = { m ->
            Panel(m, gap = 4.dp) {
                H2("Past counts", 20.sp)
                Spacer(Modifier.height(4.dp))
                if (audits.isEmpty()) Muted("No counts saved yet.", 16.sp)
                audits.forEach { (a, mv, parts) ->
                    Row(
                        Modifier.fillMaxWidth().drawBehind { drawLine(C.Row, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(fmt.format(Date(a.t)), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = C.Ink)
                            Muted("${a.lines.size} snacks counted · " + (if (parts.isEmpty()) "everything matched" else parts.joinToString(", ")))
                        }
                        if (mv.first > 0) Pill("${mv.first} missing · ${money(mv.second)}", Tone.Red) else Pill("Nothing missing", Tone.Green)
                    }
                }
            }
        }, side = { m ->
            Panel(m, gap = 12.dp) {
                H2("Missing by snack", 20.sp)
                val n = vm.data.audits.size
                Muted(if (n == 0) "Totals appear after your first saved count." else "$allMissing missing across $n count${if (n == 1) "" else "s"} · ${money(allMissingVal)}")
                val list = shrink.entries.sortedByDescending { it.value.first }.take(8)
                val max = list.firstOrNull()?.value?.first ?: 1
                list.forEach { (name, uv) -> BarRow(name, uv.first.toFloat() / max, C.BarRed, "${uv.first} · ${money(uv.second)}", 120.dp, 76.dp) }
            }
        })
    }
}

@Composable
private fun SnacksTab(vm: ShelfViewModel) {
    var target by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val id = target
        if (uri != null && id != null) vm.setPhoto(id, uri)
    }
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val number = KeyboardOptions(keyboardType = KeyboardType.Number)
    Grid(vm.data.items, 260.dp, 14.dp, extra = { m -> AddSnackForm(vm, m, decimal, number) }) { s, m ->
        val shape = RoundedCornerShape(20.dp)
        Column(m.clip(shape).background(Color.White).border(1.dp, C.Line, shape)) {
            PhotoOrInitial(s, 56.sp, Modifier.fillMaxWidth().height(120.dp))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Labeled("Name") { Field(s.name, { vm.setName(s.id, it) }, Modifier.fillMaxWidth()) }
                Labeled("Price ($)") { Field(s.priceInput, { vm.setPrice(s.id, it) }, Modifier.fillMaxWidth(), keyboard = decimal) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val photoLabel = if (s.photo != null) "Change photo" else "Add photo"
                    Btn(
                        { target = s.id; picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        Modifier.weight(1f).semantics { contentDescription = "$photoLabel for ${s.name}" },
                        border = BorderStroke(1.dp, C.Slate300), padding = PaddingValues(horizontal = 14.dp),
                    ) {
                        StrokeIcon(Icon.Camera, C.Ink)
                        BtnText(photoLabel, 14.sp)
                    }
                    val confirming = vm.confirmRemove == s.id
                    Btn(
                        { vm.removeSnack(s.id) },
                        Modifier.semantics { contentDescription = if (confirming) "Confirm removing ${s.name}" else "Remove ${s.name}" },
                        bg = if (confirming) C.Red else Color.White, fg = if (confirming) Color.White else C.Red,
                        border = BorderStroke(1.dp, if (confirming) C.Red else C.Slate300), padding = PaddingValues(horizontal = 14.dp),
                    ) { BtnText(if (confirming) "Tap again to remove" else "Remove", 14.sp) }
                }
            }
        }
    }
}

@Composable
private fun AddSnackForm(vm: ShelfViewModel, m: Modifier, decimal: KeyboardOptions, number: KeyboardOptions) {
    val f = vm.form
    val shape = RoundedCornerShape(20.dp)
    Column(
        m.background(Color.White, shape)
            .drawBehind {
                drawRoundRect(
                    C.Slate300, cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))),
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        H2("Add a snack", 20.sp)
        Labeled("Name") { Field(f.name, { vm.editForm(f.copy(name = it)) }, Modifier.fillMaxWidth(), placeholder = "e.g. Cheese Crackers") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Labeled("Price", Modifier.weight(1f)) { Field(f.price, { vm.editForm(f.copy(price = it)) }, Modifier.fillMaxWidth(), placeholder = "1.00", keyboard = decimal) }
            Labeled("Stock", Modifier.weight(1f)) { Field(f.stock, { vm.editForm(f.copy(stock = it.filter(Char::isDigit))) }, Modifier.fillMaxWidth(), placeholder = "12", keyboard = number) }
            Labeled("Par", Modifier.weight(1f)) { Field(f.par, { vm.editForm(f.copy(par = it.filter(Char::isDigit))) }, Modifier.fillMaxWidth(), placeholder = "12", keyboard = number) }
        }
        DarkBtn("Add snack", vm::addSnack, Modifier.fillMaxWidth(), enabled = !f.invalid)
    }
}

private fun hourLabel(h: Int): String {
    val x = if (h % 12 == 0) 12 else h % 12
    return "$x ${if (h < 12) "AM" else "PM"}"
}

@Composable
private fun InsightsTab(vm: ShelfViewModel) {
    val d = vm.data
    val byId = linkedMapOf<Int, Triple<String, Int, Double>>()
    val hours = sortedMapOf<Int, Int>()
    val cal = Calendar.getInstance()
    d.log.forEach { e ->
        val (_, c, v) = byId[e.id] ?: Triple(e.name, 0, 0.0)
        byId[e.id] = Triple(e.name, c + 1, v + e.price)
        cal.timeInMillis = e.t
        val h = cal.get(Calendar.HOUR_OF_DAY)
        hours[h] = (hours[h] ?: 0) + 1
    }
    val value = d.log.sumOf { it.price }
    val missing = d.audits.sumOf { a -> a.lines.sumOf { if (it.diff < 0) -it.diff else 0 } }
    val missingVal = d.audits.sumOf { a -> a.lines.sumOf { if (it.diff < 0) -it.diff * it.price else 0.0 } }
    val n = d.audits.size
    val tiles = listOf(
        Triple("Snacks taken", "${d.log.size}", "since this history started"),
        Triple("Value taken", money(value), "at current shelf prices"),
        Triple("On the shelf", "${d.items.sumOf { it.stock }}", money(d.items.sumOf { it.stock * it.price }) + " worth"),
        Triple("Below par", "${d.items.count { need(it) > 0 }}", "${d.items.count { it.stock <= 0 }} out of stock"),
        Triple("Missing in counts", "$missing", "${money(missingVal)} across $n count${if (n == 1) "" else "s"}"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Grid(tiles, 180.dp, 12.dp, stretch = true) { (label, v, note), m ->
            val shape = RoundedCornerShape(18.dp)
            Column(m.background(Color.White, shape).border(1.dp, C.Line, shape).padding(18.dp)) {
                Text(label, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Slate600)
                Text(v, Modifier.padding(top = 4.dp), fontFamily = Grotesk, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = C.Ink)
                Text(note, Modifier.padding(top = 2.dp), fontFamily = Inter, fontSize = 13.sp, color = C.Slate600)
            }
        }
        if (d.log.isEmpty()) {
            Panel(radius = 18.dp) { Muted("No snacks taken yet. Charts fill in as snacks are taken from the shelf.", 16.sp) }
        } else {
            Split(420.dp, 320.dp, true, main = { m ->
                Panel(m, gap = 12.dp) {
                    H2("Most taken", 20.sp)
                    val top = byId.values.sortedByDescending { it.second }.take(6)
                    val max = top.first().second
                    top.forEach { (name, c, v) -> BarRow(name, c.toFloat() / max, C.Blue, "$c · ${money(v)}", 130.dp, 72.dp) }
                }
            }, side = { m ->
                Panel(m, gap = 12.dp) {
                    H2("Busiest times", 20.sp)
                    val max = hours.values.maxOrNull() ?: 1
                    hours.forEach { (h, c) -> BarRow(hourLabel(h), c.toFloat() / max, C.Orange, "$c", 64.dp, 32.dp, nameMuted = true, valueBold = true) }
                }
            })
        }
    }
}

@Composable
private fun SettingsTab(vm: ShelfViewModel) {
    val cards: List<@Composable (Modifier) -> Unit> = listOf(
        { m ->
            Panel(m, gap = 12.dp) {
                H2("Admin PIN", 20.sp)
                Muted("Keeps the admin screen away from casual taps. Use 4–8 digits.")
                Labeled("New PIN") {
                    Field(
                        vm.newPin, vm::typeNewPin, Modifier.fillMaxWidth(), height = 48.dp, password = true,
                        keyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        style = TextStyle(fontFamily = Inter, fontSize = 20.sp, letterSpacing = 6.sp, color = C.Ink),
                    )
                }
                DarkBtn("Save PIN", vm::savePin, Modifier.fillMaxWidth())
                if (vm.pinMsg.isNotEmpty()) Text(vm.pinMsg, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = C.Slate700)
            }
        },
        { m ->
            Panel(m, gap = 12.dp) {
                H2("“Only a few left” warning", 20.sp)
                Muted("Snack cards show “Only X left” at or below this count.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Btn({ vm.lowBy(-1) }, Modifier.size(52.dp).semantics { contentDescription = "Lower warning level" }, radius = 12.dp, minHeight = 52.dp, padding = PaddingValues(0.dp)) { BtnText("−", 22.sp, FontWeight.Normal) }
                    Text("${vm.data.lowAt}", Modifier.widthIn(min = 40.dp), fontFamily = Grotesk, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = C.Ink)
                    Btn({ vm.lowBy(1) }, Modifier.size(52.dp).semantics { contentDescription = "Raise warning level" }, radius = 12.dp, minHeight = 52.dp, padding = PaddingValues(0.dp)) { BtnText("+", 22.sp, FontWeight.Normal) }
                }
            }
        },
        { m ->
            Panel(m, gap = 12.dp) {
                H2("History", 20.sp)
                Muted("${vm.data.log.size} snacks logged. Clearing resets Insights; stock is not changed.")
                val c = vm.confirmClear
                Btn(
                    vm::clearLog, Modifier.fillMaxWidth(), bg = if (c) C.Red else Color.White, fg = if (c) Color.White else C.Red,
                    border = BorderStroke(1.dp, if (c) C.Red else C.Slate300), radius = 12.dp, minHeight = 48.dp,
                ) { BtnText(if (c) "Tap again to clear history" else "Clear history", weight = FontWeight.Bold) }
            }
        },
        { m ->
            Panel(m, gap = 8.dp) {
                H2("Saving", 20.sp)
                Text(
                    if (vm.saveFailed) "Could not save: storage is full. Try fewer or smaller photos." else "Saving automatically on this tablet.",
                    fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = if (vm.saveFailed) C.Red else C.Green,
                )
                Muted("Data is kept on this tablet only. Other devices keep their own copy.")
            }
        },
    )
    Grid(cards, 320.dp, 16.dp, stretch = true) { card, m -> card(m) }
}
