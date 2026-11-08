package nl.brul.backend.features.report.service

import nl.brul.backend.features.report.model.Location

/**
 * Reads the GPS coordinates from a JPEG photo. A phone camera writes them into the EXIF data: a block of extra
 * information near the start of the file (marker APP1, "Exif").
 *
 * How such a file is built, in short:
 * 1. A JPEG is a list of segments; each starts with 0xFF, a type byte and a length.
 * 2. The APP1 segment holds a TIFF structure: a list of "tags" (number + value).
 * 3. Tag 0x8825 points to a second list: the GPS tags.
 * 4. Those hold latitude and longitude as three fractions (degrees, minutes, seconds) plus a letter:
 *    N/S for the latitude, E/W for the longitude.
 *
 * No extra library is needed. Every step checks that the bytes are really there; a broken or strange file gives
 * `null` (no GPS) and never a crash. PNG photos almost never contain GPS and are not read.
 * The coordinates 0,0 count as "no GPS": some phones write that when they had no fix.
 */
object PhotoGpsReader {

    private const val MARKER_APP1 = 0xE1
    private const val MARKER_START_OF_SCAN = 0xDA
    private const val MARKER_END_OF_IMAGE = 0xD9

    private const val TAG_GPS_IFD = 0x8825
    private const val TAG_LATITUDE_REF = 0x0001
    private const val TAG_LATITUDE = 0x0002
    private const val TAG_LONGITUDE_REF = 0x0003
    private const val TAG_LONGITUDE = 0x0004

    private const val TYPE_ASCII = 2
    private const val TYPE_RATIONAL = 5

    /** Where the photo was taken, or `null` when the photo has no usable GPS data. */
    fun read(bytes: ByteArray): Location? =
        runCatching { readOrNull(bytes) }.getOrNull()

    private fun readOrNull(bytes: ByteArray): Location? {
        val tiffStart = findExifTiffStart(bytes) ?: return null
        val tiff = Tiff(bytes, tiffStart)

        val gpsIfd = tiff.findEntry(tiff.firstIfdOffset(), TAG_GPS_IFD)?.valueOffset
            ?: return null

        val latitude = tiff.coordinate(gpsIfd, TAG_LATITUDE, TAG_LATITUDE_REF, negative = 'S')
            ?: return null
        val longitude = tiff.coordinate(gpsIfd, TAG_LONGITUDE, TAG_LONGITUDE_REF, negative = 'W')
            ?: return null

        if (latitude == 0.0 && longitude == 0.0) {
            return null
        }

        // Location checks the ranges itself (-90..90 and -180..180).
        return runCatching { Location(latitude, longitude) }.getOrNull()
    }

    /** Finds the APP1 segment with "Exif\0\0" and returns the position where the TIFF data starts. */
    private fun findExifTiffStart(bytes: ByteArray): Int? {
        if (bytes.size < 4 || bytes.u8(0) != 0xFF || bytes.u8(1) != 0xD8) {
            return null
        }

        var position = 2

        while (position + 4 <= bytes.size) {
            if (bytes.u8(position) != 0xFF) {
                return null
            }

            val marker = bytes.u8(position + 1)

            if (marker == MARKER_START_OF_SCAN || marker == MARKER_END_OF_IMAGE) {
                return null
            }

            // The length counts its own two bytes, but not the marker.
            val length = (bytes.u8(position + 2) shl 8) or bytes.u8(position + 3)
            val dataStart = position + 4

            if (length < 2 || dataStart + length - 2 > bytes.size) {
                return null
            }

            if (marker == MARKER_APP1 && isExifHeader(bytes, dataStart)) {
                return dataStart + 6
            }

            position = dataStart + length - 2
        }

        return null
    }

    private fun isExifHeader(bytes: ByteArray, at: Int): Boolean {
        val header = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00) // "Exif\0\0"

        return at + header.size <= bytes.size &&
                header.indices.all { bytes[at + it] == header[it] }
    }

    private fun ByteArray.u8(index: Int): Int = this[index].toInt() and 0xFF

    /** One tag of a TIFF list: number, type, number of values and the value or where it is. */
    private class Entry(
        val tag: Int,
        val type: Int,
        val count: Long,
        val valueOffset: Int,
        val entryPosition: Int
    )

    /**
     * Reads numbers from the TIFF data. TIFF can be little-endian ("II", Intel) or big-endian ("MM", Motorola);
     * all positions count from the start of the TIFF data ([start]).
     */
    private class Tiff(
        private val bytes: ByteArray,
        private val start: Int
    ) {
        private val littleEndian: Boolean = when {
            bytes[start] == 'I'.code.toByte() && bytes[start + 1] == 'I'.code.toByte() -> true
            bytes[start] == 'M'.code.toByte() && bytes[start + 1] == 'M'.code.toByte() -> false
            else -> error("Not a TIFF header.")
        }

        init {
            check(u16(2) == 42) { "Not a TIFF header." }
        }

        fun firstIfdOffset(): Int = u32(4).toInt()

        fun findEntry(ifdOffset: Int, tag: Int): Entry? {
            val count = u16(ifdOffset)

            check(count in 0..500) { "Unreasonable number of tags." }

            return (0 until count)
                .map { index -> entryAt(ifdOffset + 2 + index * 12) }
                .firstOrNull { it.tag == tag }
        }

        /** Degrees + minutes/60 + seconds/3600, negative for S or W. */
        fun coordinate(gpsIfd: Int, valueTag: Int, refTag: Int, negative: Char): Double? {
            val value = findEntry(gpsIfd, valueTag) ?: return null
            val ref = findEntry(gpsIfd, refTag) ?: return null

            if (value.type != TYPE_RATIONAL || value.count < 3 || ref.type != TYPE_ASCII) {
                return null
            }

            val degrees = rational(value.valueOffset)
            val minutes = rational(value.valueOffset + 8)
            val seconds = rational(value.valueOffset + 16)

            val decimal = degrees + minutes / 60.0 + seconds / 3600.0

            if (!decimal.isFinite()) {
                return null
            }

            // Two ASCII bytes ("N\0") fit inside the entry itself.
            val letter = Char(byte(ref.entryPosition + 8))

            return if (letter.uppercaseChar() == negative) -decimal else decimal
        }

        private fun entryAt(position: Int): Entry = Entry(
            tag = u16(position),
            type = u16(position + 2),
            count = u32(position + 4),
            valueOffset = u32(position + 8).toInt(),
            entryPosition = position
        )

        private fun rational(offset: Int): Double {
            val numerator = u32(offset)
            val denominator = u32(offset + 4)

            return if (denominator == 0L) Double.NaN else numerator.toDouble() / denominator
        }

        private fun byte(offset: Int): Int {
            val index = start + offset
            check(offset >= 0 && index < bytes.size) { "Outside the file." }
            return bytes[index].toInt() and 0xFF
        }

        private fun u16(offset: Int): Int {
            val a = byte(offset)
            val b = byte(offset + 1)
            return if (littleEndian) a or (b shl 8) else (a shl 8) or b
        }

        private fun u32(offset: Int): Long {
            val a = u16(offset).toLong()
            val b = u16(offset + 2).toLong()
            return if (littleEndian) a or (b shl 16) else (a shl 16) or b
        }
    }
}
