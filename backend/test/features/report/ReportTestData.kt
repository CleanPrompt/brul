package nl.brul.backend.features.report

import nl.brul.backend.TEST_CREATED_AT
import nl.brul.backend.features.report.model.Location
import nl.brul.backend.features.report.model.Report
import nl.brul.backend.features.report.model.ReportCategory
import nl.brul.backend.features.report.model.ReportDetails
import nl.brul.backend.features.report.model.ReportStatus
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Test data for reports and photos: a report in Breda, real JPEG and PNG files, and a JPEG with GPS in its EXIF data
 * (written the way a phone camera does it).
 */
object ReportTestData {
    /** The centre of Breda. */
    val BREDA = Location(51.5866, 4.7760)

    fun nuisance(
        ownerId: UUID = UUID.randomUUID(),
        title: String = "Graffiti on the bridge",
        location: Location = BREDA,
        createdAt: Instant = TEST_CREATED_AT,
        viewCount: Int = 0
    ) = Report(
        id = UUID.randomUUID(),
        ownerId = ownerId,
        title = title,
        description = "Large graffiti on the pedestrian bridge",
        category = ReportCategory.VANDALISM,
        location = location,
        details = ReportDetails.Nuisance,
        status = ReportStatus.OPEN,
        createdAt = createdAt,
        viewCount = viewCount
    )

    /** A small real image, so ImageIO and the format check accept it. */
    private fun image(format: String): ByteArray {
        val image = BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB)
        val output = ByteArrayOutputStream()
        ImageIO.write(image, format, output)
        return output.toByteArray()
    }

    fun jpeg(): ByteArray = image("jpg")

    fun png(): ByteArray = image("png")

    /**
     * A JPEG with an EXIF APP1 block holding GPS latitude and longitude, inserted right after the
     * start marker (FF D8). Negative values become S and W.
     */
    fun jpegWithGps(
        latitude: Double,
        longitude: Double,
        bigEndian: Boolean = false
    ): ByteArray {
        val plain = jpeg()
        val tiff = gpsTiff(latitude, longitude, bigEndian)
        val exif = "Exif".toByteArray() + byteArrayOf(0, 0) + tiff
        val length = exif.size + 2

        val app1 = byteArrayOf(
            0xFF.toByte(), 0xE1.toByte(),
            (length shr 8).toByte(), (length and 0xFF).toByte()
        ) + exif

        return plain.copyOfRange(0, 2) + app1 + plain.copyOfRange(2, plain.size)
    }

    /**
     * TIFF layout (offsets from the start of the TIFF data):
     *   0  header ("II" or "MM", 42, offset of IFD0 = 8)
     *   8  IFD0 with one entry: GPS IFD pointer (0x8825) -> 26
     *  26  GPS IFD with four entries: lat ref, lat, lon ref, lon
     *  80  latitude as three rationals (degrees, minutes, seconds)
     * 104  longitude as three rationals
     */
    private fun gpsTiff(latitude: Double, longitude: Double, bigEndian: Boolean): ByteArray {
        val buffer = ByteBuffer.allocate(128)
            .order(if (bigEndian) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)

        buffer.put(if (bigEndian) "MM".toByteArray() else "II".toByteArray())
        buffer.putShort(42)
        buffer.putInt(8)

        // IFD0
        buffer.putShort(1)
        entry(buffer, tag = 0x8825, type = 4, count = 1, value = 26)
        buffer.putInt(0)

        // GPS IFD
        buffer.putShort(4)
        asciiEntry(buffer, tag = 1, letter = if (latitude < 0) 'S' else 'N')
        entry(buffer, tag = 2, type = 5, count = 3, value = 80)
        asciiEntry(buffer, tag = 3, letter = if (longitude < 0) 'W' else 'E')
        entry(buffer, tag = 4, type = 5, count = 3, value = 104)
        buffer.putInt(0)

        dms(buffer, abs(latitude))
        dms(buffer, abs(longitude))

        return buffer.array()
    }

    private fun entry(buffer: ByteBuffer, tag: Int, type: Int, count: Int, value: Int) {
        buffer.putShort(tag.toShort())
        buffer.putShort(type.toShort())
        buffer.putInt(count)
        buffer.putInt(value)
    }

    /** Two ASCII bytes ("N\0") fit inside the entry itself. */
    private fun asciiEntry(buffer: ByteBuffer, tag: Int, letter: Char) {
        buffer.putShort(tag.toShort())
        buffer.putShort(2)
        buffer.putInt(2)
        buffer.put(letter.code.toByte())
        buffer.put(0)
        buffer.putShort(0)
    }

    /** Degrees and minutes as whole numbers, seconds with four decimals. */
    private fun dms(buffer: ByteBuffer, value: Double) {
        val degrees = value.toLong()
        val minutesFull = (value - degrees) * 60
        val minutes = minutesFull.toLong()
        val seconds = ((minutesFull - minutes) * 60 * 10_000).roundToLong()

        buffer.putInt(degrees.toInt()).putInt(1)
        buffer.putInt(minutes.toInt()).putInt(1)
        buffer.putInt(seconds.toInt()).putInt(10_000)
    }
}
