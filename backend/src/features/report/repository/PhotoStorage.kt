package nl.brul.backend.features.report.repository

/**
 * Where the bytes of photos are kept; the database only stores the key. The service only knows this interface,
 * so tests can keep photos in memory and the files never end up in H2.
 * The real version is `FileSystemPhotoStorage`.
 */
interface PhotoStorage {
    fun save(key: String, bytes: ByteArray)

    /** The bytes stored under [key], or `null` when there is no such file. */
    fun load(key: String): ByteArray?

    fun delete(key: String)
}
