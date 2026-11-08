package nl.brul.backend.features.report

import nl.brul.backend.features.report.repository.PhotoStorage

/** Photo files in memory instead of on disk, so tests leave nothing behind. */
class InMemoryPhotoStorage : PhotoStorage {
    val files = mutableMapOf<String, ByteArray>()

    override fun save(key: String, bytes: ByteArray) {
        files[key] = bytes.copyOf()
    }

    override fun load(key: String): ByteArray? = files[key]?.copyOf()

    override fun delete(key: String) {
        files.remove(key)
    }
}
