package io.github.z1fire.snackpos

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class AdminTab(val label: String) {
    Stock("Stock"), Count("Stock count"), Snacks("Snacks & prices"), Insights("Insights"), Settings("Settings")
}

/** What a take removed, so Undo can put it all back. */
data class Toast(val taken: Map<Int, Int>, val at: Long, val text: String)

data class AddForm(val name: String = "", val price: String = "", val stock: String = "", val par: String = "") {
    val invalid: Boolean get() = name.isBlank() || (price.toDoubleOrNull()?.let { it < 0 } ?: true)
}

class ShelfViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ShelfStore(app)

    var data by mutableStateOf(store.load() ?: seedData())
        private set
    var saveFailed by mutableStateOf(false)
        private set

    var toast by mutableStateOf<Toast?>(null)
        private set
    /** Snack id to how many are picked. */
    val basket = mutableStateMapOf<Int, Int>()
    var admin by mutableStateOf(false)
        private set
    var tab by mutableStateOf(AdminTab.Stock)
        private set

    var pinOpen by mutableStateOf(false)
        private set
    var pinEntry by mutableStateOf("")
        private set
    var pinError by mutableStateOf(false)
        private set
    /** Bumped on every wrong PIN so the dots shake again. */
    var pinErrorTick by mutableStateOf(0)
        private set
    var newPin by mutableStateOf("")
        private set
    var pinMsg by mutableStateOf("")
        private set

    var confirmRemove by mutableStateOf<Int?>(null)
        private set
    var confirmClear by mutableStateOf(false)
        private set
    val counts = mutableStateMapOf<Int, String>()
    var countMsg by mutableStateOf("")
        private set
    var form by mutableStateOf(AddForm())
        private set

    private var toastJob: Job? = null
    private val saves = Channel<ShelfData>(Channel.CONFLATED)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (d in saves) {
                val ok = store.save(d)
                withContext(Dispatchers.Main) { saveFailed = !ok }
            }
        }
    }

    private fun update(fn: (ShelfData) -> ShelfData) {
        data = fn(data)
        saves.trySend(data)
    }

    private fun patch(id: Int, fn: (Snack) -> Snack) =
        update { d -> d.copy(items = d.items.map { if (it.id == id) fn(it) else it }) }

    fun setStock(id: Int, delta: Int) = patch(id) { it.copy(stock = maxOf(0, it.stock + delta)) }
    fun fill(id: Int) = patch(id) { it.copy(stock = maxOf(it.stock, it.par)) }
    fun fillAll() = update { d -> d.copy(items = d.items.map { it.copy(stock = maxOf(it.stock, it.par)) }) }
    fun parBy(id: Int, delta: Int) = patch(id) { it.copy(par = maxOf(0, it.par + delta)) }
    fun setName(id: Int, v: String) = patch(id) { it.copy(name = v) }
    fun setPrice(id: Int, v: String) = patch(id) { x ->
        val p = v.toDoubleOrNull()
        x.copy(priceInput = v, price = if (p != null && p >= 0) p else x.price)
    }

    /** Adds one to the basket, up to what is on the shelf. */
    fun pick(id: Int) {
        val it = data.items.firstOrNull { s -> s.id == id } ?: return
        val have = basket[id] ?: 0
        if (have < it.stock) basket[id] = have + 1
    }

    fun unpick(id: Int) {
        val have = basket[id] ?: return
        if (have <= 1) basket.remove(id) else basket[id] = have - 1
    }

    fun cancel() { basket.clear() }

    /** Basket lines still in stock, in shelf order. */
    val lines: List<Pair<Snack, Int>>
        get() = data.items.mapNotNull { s -> basket[s.id]?.let { q -> s to minOf(q, s.stock) }?.takeIf { it.second > 0 } }

    fun take() {
        val picked = lines
        if (picked.isEmpty()) return
        val now = System.currentTimeMillis()
        val qty = picked.associate { (s, q) -> s.id to q }
        update { d ->
            d.copy(
                items = d.items.map { s -> qty[s.id]?.let { s.copy(stock = s.stock - it) } ?: s },
                log = d.log + picked.flatMap { (s, q) -> List(q) { LogEntry(s.id, s.name, s.price, now) } },
            )
        }
        basket.clear()
        val count = picked.sumOf { it.second }
        val total = picked.sumOf { (s, q) -> s.price * q }
        val text = if (picked.size == 1 && count == 1) "Enjoy your ${picked[0].first.name}!" else "Enjoy! $count snacks · ${money(total)}"
        toast = Toast(qty, now, text)
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(5000); toast = null }
    }

    fun undo() {
        val t = toast ?: return
        update { d ->
            d.copy(
                items = d.items.map { s -> t.taken[s.id]?.let { s.copy(stock = s.stock + it) } ?: s },
                log = d.log.filterNot { it.t == t.at && it.id in t.taken },
            )
        }
        toastJob?.cancel()
        toast = null
    }

    fun openPin() { pinOpen = true; pinEntry = ""; pinError = false; basket.clear() }
    fun closePin() { pinOpen = false; pinEntry = ""; pinError = false }
    fun backspacePin() { pinEntry = pinEntry.dropLast(1); pinError = false }

    fun pressDigit(k: String) {
        val entry = (pinEntry + k).take(8)
        if (entry.length >= data.pin.length) {
            if (entry == data.pin) {
                pinOpen = false; pinEntry = ""; pinError = false
                admin = true; basket.clear(); tab = AdminTab.Stock
            } else {
                pinEntry = ""; pinError = true; pinErrorTick++
            }
            return
        }
        pinEntry = entry; pinError = false
    }

    fun lock() { admin = false; confirmRemove = null; confirmClear = false; pinMsg = "" }

    fun goTab(t: AdminTab) { tab = t; confirmRemove = null; confirmClear = false; pinMsg = ""; countMsg = "" }

    fun removeSnack(id: Int) {
        if (confirmRemove != id) { confirmRemove = id; return }
        data.items.firstOrNull { it.id == id }?.photo?.let { File(it).delete() }
        update { d -> d.copy(items = d.items.filter { it.id != id }) }
        counts.remove(id)
        basket.remove(id)
        confirmRemove = null
    }

    fun editForm(f: AddForm) { form = f }

    fun addSnack() {
        val f = form
        val price = f.price.toDoubleOrNull() ?: return
        if (f.name.isBlank() || price < 0) return
        val stock = maxOf(0, f.stock.toIntOrNull() ?: 0)
        val par = f.par.toIntOrNull()?.let { maxOf(0, it) } ?: maxOf(stock, 12)
        update { d ->
            val id = d.nextId
            val item = Snack(id, f.name.trim(), stock, Palette[id % Palette.size], price, "%.2f".format(price), par)
            d.copy(items = d.items + item, nextId = id + 1)
        }
        form = AddForm()
    }

    fun typeNewPin(v: String) { newPin = v.filter(Char::isDigit).take(8); pinMsg = "" }

    fun savePin() {
        if (!Regex("^\\d{4,8}$").matches(newPin)) { pinMsg = "Use 4 to 8 digits."; return }
        val p = newPin
        update { it.copy(pin = p) }
        newPin = ""; pinMsg = "PIN updated."
    }

    fun lowBy(delta: Int) = update { it.copy(lowAt = (it.lowAt + delta).coerceIn(0, 50)) }

    fun clearLog() {
        if (!confirmClear) { confirmClear = true; return }
        update { it.copy(log = emptyList()) }
        confirmClear = false
    }

    fun setCount(id: Int, v: String) { counts[id] = v.filter(Char::isDigit).take(4); countMsg = "" }
    fun clearCounts() { counts.clear(); countMsg = "" }

    fun saveCount() {
        val lines = data.items.mapNotNull { it ->
            val num = counts[it.id]?.toIntOrNull() ?: return@mapNotNull null
            val counted = maxOf(0, num)
            AuditLine(it.id, it.name, it.stock, counted, counted - it.stock, it.price)
        }
        if (lines.isEmpty()) return
        val byId = lines.associate { it.id to it.counted }
        update { d ->
            d.copy(
                items = d.items.map { s -> byId[s.id]?.let { s.copy(stock = it) } ?: s },
                audits = listOf(Audit(System.currentTimeMillis(), lines)) + d.audits,
            )
        }
        counts.clear()
        countMsg = "Count saved. The app now matches the shelf."
    }

    /** Scales the picked image to at most 520px and keeps it as a JPEG in app storage. */
    fun setPhoto(id: Int, uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                try {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 520) sample *= 2
                    val raw = ctx.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                    } ?: return@withContext null
                    val sc = minOf(1f, 520f / maxOf(raw.width, raw.height))
                    val bmp = Bitmap.createScaledBitmap(raw, (raw.width * sc).toInt().coerceAtLeast(1), (raw.height * sc).toInt().coerceAtLeast(1), true)
                    val dir = File(ctx.filesDir, "photos").apply { mkdirs() }
                    val out = File(dir, "snack-$id-${System.currentTimeMillis()}.jpg")
                    FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.JPEG, 80, it) }
                    out.absolutePath
                } catch (_: Exception) {
                    null
                }
            } ?: return@launch
            val old = data.items.firstOrNull { it.id == id }?.photo
            patch(id) { it.copy(photo = path) }
            if (old != null) withContext(Dispatchers.IO) { File(old).delete() }
        }
    }
}
