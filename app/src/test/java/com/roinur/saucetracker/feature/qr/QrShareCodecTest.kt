package com.roinur.saucetracker.feature.qr

import com.roinur.saucetracker.data.source.SourceEntryKey
import com.roinur.saucetracker.data.source.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.DeflaterOutputStream

class QrShareCodecTest {
    private val supported = setOf(SourceId("nhentai"), SourceId("mangadex"))

    @Test fun mixedPackageRoundTrips() {
        val packageValue = QrSharePackage(listOf(
            QrSharedEntry(SourceEntryKey(SourceId("nhentai"), "123"), previewTitle = "One", read = true, rating = 4),
            QrSharedEntry(SourceEntryKey(SourceId("mangadex"), "123e4567-e89b-12d3-a456-426614174000"), previewTitle = "Two", pinned = true)
        ))
        val decoded = QrShareCodec.decode(QrShareCodec.encode(packageValue), supported)
        assertTrue(decoded is QrDecodeResult.Success)
        assertEquals(packageValue, (decoded as QrDecodeResult.Success).value)
    }

    @Test fun changedPayloadIsRejected() {
        val encoded = QrShareCodec.encode(QrSharePackage(listOf(QrSharedEntry(SourceEntryKey(SourceId("nhentai"), "123")))))
        val changed = encoded.dropLast(1) + if (encoded.last() == 'A') "B" else "A"
        assertTrue(QrShareCodec.decode(changed, supported) is QrDecodeResult.Invalid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun moreThanTenEntriesAreRejected() {
        QrShareCodec.encode(QrSharePackage((1..11).map { QrSharedEntry(SourceEntryKey(SourceId("nhentai"), it.toString())) }))
    }

    private fun packet(content: ByteArray): String {
        val bytes = ByteArrayOutputStream()
        bytes.write(content)
        DataOutputStream(bytes).writeLong(CRC32().apply { update(content) }.value)
        val compressed = ByteArrayOutputStream()
        DeflaterOutputStream(compressed).use { it.write(bytes.toByteArray()) }
        return QrShareCodec.FORMAT_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed.toByteArray())
    }

    @Test fun tenEntriesRoundTrip() {
        val value = QrSharePackage((1..10).map { QrSharedEntry(SourceEntryKey(SourceId("nhentai"), it.toString())) })
        assertEquals(value, (QrShareCodec.decode(QrShareCodec.encode(value)) as QrDecodeResult.Success).value)
    }
    @Test(expected = IllegalArgumentException::class)
    fun duplicateEntriesCannotGenerateAnUnreadableQr() {
        val entry = QrSharedEntry(SourceEntryKey(SourceId("nhentai"), "123"))
        QrShareCodec.encode(QrSharePackage(listOf(entry, entry)))
    }
    @Test fun highlyCompressibleOptionalMetadataStillRespectsInflatedLimit() {
        val tags = (1..12).map { "列".repeat(58) + it }
        val value = QrSharePackage((1..10).map { QrSharedEntry(SourceEntryKey(SourceId("nhentai"), it.toString()), canonicalUrl = "例".repeat(512), previewTitle = "題".repeat(120), localTags = tags) })
        val result = QrShareCodec.decode(QrShareCodec.encode(value))
        assertTrue(result is QrDecodeResult.Success)
        val entries = (result as QrDecodeResult.Success).value.entries
        assertEquals(10, entries.size)
        assertTrue(entries.all { it.previewTitle.isEmpty() && it.canonicalUrl.isEmpty() && it.localTags.isEmpty() })
    }
    @Test fun decompressionBombAndUnknownVersionAreRejected() {
        assertTrue(QrShareCodec.decode(packet(ByteArray(30_000))) is QrDecodeResult.Invalid)
        assertTrue(QrShareCodec.decode(packet(byteArrayOf(2, 1, 0))) is QrDecodeResult.Invalid)
        assertTrue(QrShareCodec.decode("STQR99:abc") is QrDecodeResult.Invalid)
    }
    @Test fun unknownSourceNeverPassesSupportedSourceCheck() {
        val value = QrSharePackage(listOf(QrSharedEntry(SourceEntryKey(SourceId("unknown"), "123"))))
        assertTrue(QrShareCodec.decode(QrShareCodec.encode(value), supported) is QrDecodeResult.Invalid)
    }
    @Test fun oversizedUntrustedDisplayMetadataIsRejected() {
        val content = ByteArrayOutputStream()
        DataOutputStream(content).use { out ->
            out.writeByte(1); out.writeByte(1); out.writeUTF("nhentai"); out.writeUTF("123")
            out.writeUTF(""); out.writeUTF("x".repeat(121)); out.writeBoolean(false)
            out.writeByte(0); out.writeBoolean(false); out.writeByte(0)
        }
        assertTrue(QrShareCodec.decode(packet(content.toByteArray())) is QrDecodeResult.Invalid)
    }
}
