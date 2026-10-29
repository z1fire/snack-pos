package io.github.z1fire.snackpos

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object C {
    val Page = Color(0xFFEEF1F6)
    val Ink = Color(0xFF0F172A)
    val Slate800 = Color(0xFF1E293B)
    val Slate700 = Color(0xFF334155)
    val Slate600 = Color(0xFF475569)
    val Slate500 = Color(0xFF64748B)
    val Slate300 = Color(0xFFCBD5E1)
    val Line = Color(0xFFDDE2E8)
    val Row = Color(0xFFEEF0F3)
    val Soft = Color(0xFFF8FAFC)
    val Yellow = Color(0xFFFACC15)
    val Red = Color(0xFFB91C1C)
    val Green = Color(0xFF166534)
    val Blue = Color(0xFF2563EB)
    val Orange = Color(0xFFEA580C)
    val BarRed = Color(0xFFDC2626)
}

val Inter = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(R.font.inter, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)
val Grotesk = FontFamily(
    listOf(600, 700).map { w ->
        Font(R.font.space_grotesk, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

fun hex(s: String): Color = try {
    Color(android.graphics.Color.parseColor(s))
} catch (_: Exception) {
    C.Blue
}

/** Draws its content through a saturation filter (0 = grayscale). */
fun Modifier.saturation(s: Float): Modifier = if (s >= 1f) this else drawWithCache {
    val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(s) }) }
    onDrawWithContent {
        drawIntoCanvas {
            it.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            it.restore()
        }
    }
}

@Composable
fun rememberPhoto(path: String?): ImageBitmap? {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = if (path == null) null else withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(path)?.asImageBitmap()
        }
    }
    return bmp
}

@Composable
fun PhotoOrInitial(snack: Snack, fontSize: TextUnit, modifier: Modifier = Modifier, decor: @Composable () -> Unit = {}) {
    val photo = rememberPhoto(snack.photo)
    Box(modifier.background(hex(snack.color)), contentAlignment = Alignment.Center) {
        if (photo != null) {
            Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            decor()
            Text(snack.name.take(1).ifEmpty { "?" }, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = fontSize, color = Color.White)
        }
    }
}

@Composable
fun Thumb(snack: Snack) = PhotoOrInitial(snack, 18.sp, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))

enum class Tone(val bg: Color, val fg: Color) {
    Green(Color(0xFFE6F4EA), Color(0xFF166534)),
    Amber(Color(0xFFFEF3C7), Color(0xFF92400E)),
    Gray(Color(0xFFE5E7EB), Color(0xFF374151)),
    Red(Color(0xFFFEE2E2), Color(0xFF991B1B)),
    Blue(Color(0xFFDBEAFE), Color(0xFF1E40AF)),
}

@Composable
fun Pill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    Text(
        text, modifier.background(tone.bg, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 5.dp),
        color = tone.fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFamily = Inter, softWrap = false,
    )
}

/** A plain styled button: the design draws every button by hand rather than with Material. */
@Composable
fun Btn(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    bg: Color = C.Soft,
    fg: Color = C.Ink,
    border: BorderStroke? = BorderStroke(1.dp, C.Line),
    radius: Dp = 10.dp,
    minHeight: Dp = 44.dp,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp),
    label: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Row(
        modifier
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(bg, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(padding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides fg,
        ) { content() }
    }
}

@Composable
fun BtnText(text: String, size: TextUnit = 16.sp, weight: FontWeight = FontWeight.SemiBold, color: Color = androidx.compose.material3.LocalContentColor.current) =
    Text(text, fontFamily = Inter, fontSize = size, fontWeight = weight, color = color, textAlign = TextAlign.Center)

@Composable
fun DarkBtn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, minHeight: Dp = 48.dp) =
    Btn(onClick, modifier, enabled, bg = C.Ink, fg = Color.White, border = null, radius = 12.dp, minHeight = minHeight, padding = PaddingValues(horizontal = 18.dp)) {
        BtnText(text, weight = FontWeight.Bold)
    }

/** White rounded panel used for every admin section. */
@Composable
fun Panel(modifier: Modifier = Modifier, gap: Dp = 14.dp, radius: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier.background(Color.White, shape).border(1.dp, C.Line, shape).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

@Composable
fun H2(text: String, size: TextUnit = 22.sp) =
    Text(text, fontFamily = Grotesk, fontWeight = FontWeight.Bold, fontSize = size, color = C.Ink)

@Composable
fun Muted(text: String, size: TextUnit = 14.sp, modifier: Modifier = Modifier) =
    Text(text, modifier, fontFamily = Inter, fontSize = size, color = C.Slate600)

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    style: TextStyle = TextStyle(fontFamily = Inter, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = C.Ink),
    height: Dp = 44.dp,
    borderWidth: Dp = 1.dp,
    radius: Dp = 10.dp,
    password: Boolean = false,
) {
    val shape = RoundedCornerShape(radius)
    BasicTextField(
        value, onChange,
        modifier.height(height).border(borderWidth, C.Slate300, shape).background(Color.White, shape),
        textStyle = style,
        singleLine = true,
        keyboardOptions = keyboard,
        cursorBrush = SolidColor(C.Ink),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = if (style.textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart) {
                if (value.isEmpty() && placeholder.isNotEmpty()) Text(placeholder, style = style.copy(color = C.Slate500))
                inner()
            }
        },
    )
}

@Composable
fun Labeled(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = Inter, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Slate600)
        content()
    }
}

/** name | bar | value, the row shape shared by every chart in the design. */
@Composable
fun BarRow(name: String, fraction: Float, color: Color, value: String, nameWidth: Dp, valueWidth: Dp, nameMuted: Boolean = false, valueBold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            name, Modifier.width(nameWidth), fontFamily = Inter, fontSize = 14.sp, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            fontWeight = if (nameMuted) FontWeight.Normal else FontWeight.SemiBold, color = if (nameMuted) C.Slate700 else C.Ink,
        )
        Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(6.dp)).background(C.Page)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(fraction.coerceIn(0.04f, 1f)).clip(RoundedCornerShape(6.dp)).background(color))
        }
        Text(
            value, Modifier.widthIn(min = valueWidth), fontFamily = Inter, fontSize = 14.sp, textAlign = TextAlign.End,
            fontWeight = if (valueBold) FontWeight.SemiBold else FontWeight.Normal, color = if (valueBold) C.Ink else C.Slate700,
        )
    }
}

/** Stroke icons from the design, drawn in a 24-unit box. */
enum class Icon { Lock, Left, Right, Camera }

@Composable
fun StrokeIcon(icon: Icon, color: Color, size: Dp = 18.dp, width: Float = 2f) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24f
        val st = Stroke(width * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = st)
        when (icon) {
            Icon.Lock -> {
                drawRoundRect(color, Offset(5 * u, 11 * u), androidx.compose.ui.geometry.Size(14 * u, 10 * u), androidx.compose.ui.geometry.CornerRadius(2 * u), style = st)
                p { moveTo(8 * u, 11 * u); lineTo(8 * u, 7 * u); arcTo(Rect(8 * u, 3 * u, 16 * u, 11 * u), 180f, 180f, false); lineTo(16 * u, 11 * u) }
            }
            Icon.Left -> p { moveTo(15 * u, 6 * u); lineTo(9 * u, 12 * u); lineTo(15 * u, 18 * u) }
            Icon.Right -> p { moveTo(9 * u, 6 * u); lineTo(15 * u, 12 * u); lineTo(9 * u, 18 * u) }
            Icon.Camera -> {
                p { moveTo(4 * u, 8 * u); lineTo(7 * u, 8 * u); lineTo(9 * u, 5 * u); lineTo(15 * u, 5 * u); lineTo(17 * u, 8 * u); lineTo(20 * u, 8 * u); lineTo(20 * u, 19 * u); lineTo(4 * u, 19 * u); close() }
                drawCircle(color, 3.5f * u, Offset(12 * u, 13 * u), style = st)
            }
        }
    }
}
