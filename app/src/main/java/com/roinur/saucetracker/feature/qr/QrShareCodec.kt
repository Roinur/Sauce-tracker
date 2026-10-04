package com.roinur.saucetracker.feature.qr

import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

data class QrSharedEntry(
    val key: SourceEntryKey,
    val canonicalUrl: String = "",
    val previewTitle: String = "",
    val read: Boolean = false,
    val rating: Int = 0,
    val pinned: Boolean = false,
    val localTags: List<String> = emptyList()
)

data class QrSharePackage(val entries: List<QrSharedEntry>)

sealed interface QrDecodeResult {
    data class Success(val value: QrSharePackage) : QrDecodeResult
    data class Invalid(val reason: String) : QrDecodeResult
}

object QrShareCodec {
    const val FORMAT_PREFIX = "STQR1:"
    const val MAX_ENTRIES = 10
    const val MAX_ENCODED_BYTES = 2_200
    private const val MAX_INFLATED_BYTES = 24_000

    fun encode(value: QrSharePackage): String {
        require(value.entries.size in 1..MAX_ENTRIES) { "QR packages contain 1-$MAX_ENTRIES entries." }
        require(value.entries.map { it.key }.distinct().size == value.entries.size) { "Duplicate entries in QR package." }
        val compact = serialize(value, includeOptional = true)
        val first = wrap(compact)
        if (compact.size + 8 <= MAX_INFLATED_BYTES && first.toByteArray(Charsets.UTF_8).size <= MAX_ENCODED_BYTES) return first
        val essential = serialize(value.copy(entries = value.entries.map { it.copy(canonicalUrl = "", previewTitle = "", localTags = emptyList()) }), includeOptional = false)
        return wrap(essential).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_ENCODED_BYTES) { "QR package is too large; choose fewer entries or text export." }
        }
    }

    fun decode(encoded: String): QrDecodeResult {
        if (!encoded.startsWith(FORMAT_PREFIX)) return QrDecodeResult.Invalid("Unsupported QR format.")
        if (encoded.toByteArray(Charsets.UTF_8).size > MAX_ENCODED_BYTES) return QrDecodeResult.Invalid("QR payload is too large.")
        return runCatching {
            val compressed = Base64.getUrlDecoder().decode(encoded.removePrefix(FORMAT_PREFIX))
            val packet = inflateBounded(compressed)
            require(packet.size > 8) { "Truncated QR payload." }
            val content = packet.copyOfRange(0, packet.size - 8)
            val expected = DataInputStream(ByteArrayInputStream(packet, packet.size - 8, 8)).readLong()
            require(crc(content) == expected) { "QR checksum failed." }
            val value = deserialize(content)
            require(value.entries.size in 1..MAX_ENTRIES) { "Invalid entry count." }
            require(value.entries.map { it.key }.distinct().size == value.entries.size) { "Duplicate entries in QR payload." }
            QrDecodeResult.Success(value)
        }.getOrElse { QrDecodeResult.Invalid(it.message ?: "Invalid QR payload.") }
    }

    fun decode(encoded: String, supportedSources: Set<SourceId>): QrDecodeResult = when (val result = decode(encoded)) {
        is QrDecodeResult.Invalid -> result
        is QrDecodeResult.Success -> if (result.value.entries.all { it.key.sourceId in supportedSources }) {
            result
        } else {
            QrDecodeResult.Invalid("QR contains an unsupported source.")
        }
    }

    private fun serialize(value: QrSharePackage, includeOptional: Boolean): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeByte(1)
            out.writeByte(value.entries.size)
            value.entries.forEach { entry ->
                out.writeUTF(entry.key.sourceId.value)
                out.writeUTF(entry.key.remoteId)
                out.writeUTF(if (includeOptional) entry.canonicalUrl.take(512) else "")
                out.writeUTF(if (includeOptional) entry.previewTitle.take(120) else "")
                out.writeBoolean(entry.read)
                out.writeByte(entry.rating.coerceIn(0, 5))
                out.writeBoolean(entry.pinned)
                val tags = if (includeOptional) entry.localTags.map(String::trim).filter(String::isNotBlank).distinct().take(12) else emptyList()
                out.writeByte(tags.size)
                tags.forEach { out.writeUTF(it.take(60)) }
            }
        }
        bytes.toByteArray()
    }

    private fun deserialize(content: ByteArray): QrSharePackage = DataInputStream(ByteArrayInputStream(content)).use { input ->
        require(input.readUnsignedByte() == 1) { "Unsupported QR version." }
        val count = input.readUnsignedByte()
        require(count in 1..MAX_ENTRIES) { "Invalid entry count." }
        val entries = List(count) {
            val source = SourceId(input.readUTF())
            val remoteId = input.readUTF().also { require(it.length <= 160) }
            val url = input.readUTF().also { require(it.length <= 512) { "QR URL is too long." } }
            val title = input.readUTF().also { require(it.length <= 120) { "QR preview title is too long." } }
            val read = input.readBoolean()
            val rating = input.readUnsignedByte().also { require(it in 0..5) }
            val pinned = input.readBoolean()
            val tagCount = input.readUnsignedByte().also { require(it <= 12) }
            val tags = List(tagCount) { input.readUTF().also { tag -> require(tag.length <= 60) } }
            QrSharedEntry(SourceEntryKey(source, remoteId), url, title, read, rating, pinned, tags)
        }
        require(input.available() == 0) { "Unexpected trailing QR data." }
        QrSharePackage(entries)
    }

    private fun wrap(content: ByteArray): String {
        val packet = ByteArrayOutputStream().use { bytes ->
            bytes.write(content)
            DataOutputStream(bytes).use { it.writeLong(crc(content)) }
            bytes.toByteArray()
        }
        val compressed = ByteArrayOutputStream().use { bytes ->
            DeflaterOutputStream(bytes).use { it.write(packet) }
            bytes.toByteArray()
        }
        return FORMAT_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
    }

    private fun inflateBounded(compressed: ByteArray): ByteArray = InflaterInputStream(ByteArrayInputStream(compressed)).use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            require(output.size() + read <= MAX_INFLATED_BYTES) { "QR payload expands beyond its safe limit." }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    }

    private fun crc(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value
}
