package io.github.z1fire.snackpos

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Snack(
    val id: Int,
    val name: String,
    val stock: Int,
    val color: String,
    val price: Double,
    val priceInput: String,
    val par: Int,
    /** Absolute path of a JPEG in the app's files directory, or null. */
    val photo: String? = null,
)

@Serializable
data class LogEntry(val id: Int, val name: String, val price: Double, val t: Long)

@Serializable
data class AuditLine(
    val id: Int,
    val name: String,
    val expected: Int,
    val counted: Int,
    val diff: Int,
    val price: Double,
)

@Serializable
data class Audit(val t: Long, val lines: List<AuditLine>)

/** Everything kept between launches. */
@Serializable
data class ShelfData(
    val items: List<Snack>,
    val nextId: Int,
    val log: List<LogEntry> = emptyList(),
    val audits: List<Audit> = emptyList(),
    val pin: String = "1234",
    val lowAt: Int = 3,
)

val Palette = listOf("#2563EB", "#DB2777", "#0D9488", "#7C3AED", "#EA580C", "#65A30D", "#B45309", "#0891B2")

fun seedData(): ShelfData {
    val seed = listOf(
        Triple("Potato Chips", 14, "#F59E0B") to (1.00 to 18),
        Triple("Pretzels", 9, "#B45309") to (1.00 to 12),
        Triple("Chocolate Bar", 12, "#7C2D12") to (1.50 to 15),
        Triple("Gummy Bears", 2, "#DB2777") to (1.25 to 12),
        Triple("Granola Bar", 10, "#65A30D") to (1.00 to 12),
        Triple("Trail Mix", 6, "#0D9488") to (1.50 to 10),
        Triple("Fruit Cup", 0, "#EA580C") to (1.25 to 10),
        Triple("Crackers", 8, "#CA8A04") to (1.00 to 12),
        Triple("Popcorn", 11, "#2563EB") to (1.25 to 12),
    )
    val items = seed.mapIndexed { i, (a, b) ->
        Snack(i, a.first, a.second, a.third, b.first, "%.2f".format(b.first), b.second)
    }
    return ShelfData(items = items, nextId = items.size)
}

/** Saves the shelf as JSON in the app's private files directory. */
class ShelfStore(context: Context) {
    private val file = File(context.filesDir, "snack-shelf-v1.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): ShelfData? = try {
        if (file.exists()) json.decodeFromString(ShelfData.serializer(), file.readText()) else null
    } catch (_: Exception) {
        null
    }

    /** Writes through a temp file so a crash mid-write can't corrupt the save. */
    fun save(data: ShelfData): Boolean = try {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(ShelfData.serializer(), data))
        tmp.renameTo(file) || run { file.delete(); tmp.renameTo(file) }
    } catch (_: Exception) {
        false
    }
}

fun money(n: Double): String = "$" + "%.2f".format(Math.round(n * 100) / 100.0)
