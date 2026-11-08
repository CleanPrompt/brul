package nl.brul.backend.features.report.repository

import java.nio.file.Files
import java.nio.file.Path

/**
 * Keeps every photo as a file in the folder [root] (`data/uploads/` in `Application.kt`, ignored by Git).
 *
 * The service creates the keys itself (a UUID), so a strange key is a bug in our own code: [resolve] then
 * fails with a 500 instead of writing outside the folder, for example through `../`.
 */
class FileSystemPhotoStorage(
    private val root: Path
) : PhotoStorage {

    override fun save(key: String, bytes: ByteArray) {
        Files.createDirectories(root)
        Files.write(resolve(key), bytes)
    }

    override fun load(key: String): ByteArray? {
        val file = resolve(key)
        return if (Files.exists(file)) Files.readAllBytes(file) else null
    }

    override fun delete(key: String) {
        Files.deleteIfExists(resolve(key))
    }

    private fun resolve(key: String): Path {
        check(key.matches(SAFE_KEY) && !key.contains("..")) {
            "Invalid storage key"
        }

        return root.resolve(key)
    }

    private companion object {
        val SAFE_KEY = Regex("[A-Za-z0-9._-]+")
    }
}
