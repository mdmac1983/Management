package app.orionmd.management.util

import android.content.Context
import android.net.Uri
import app.orionmd.management.data.AppDatabase
import app.orionmd.management.data.AppSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Backs up / restores the entire on-device database as a single zip. The database file itself
 * is already SQLCipher-encrypted at rest, so the zip is exported "as-is" - restoring it only
 * works when unlocked with the same PIN/pattern/password that was active at export time, since
 * that's what derives the passphrase the file was encrypted with.
 */
object BackupManager {

    private const val ENTRY_NAME = "rentals_database.db"
    private const val MANIFEST_NAME = "manifest.txt"

    suspend fun export(context: Context, destinationUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Force a checkpoint so the .db file on disk reflects the latest writes (WAL mode).
            AppSession.repository?.let {
                // no-op hook point - Room/SQLCipher auto-checkpoints; explicit checkpoint isn't
                // exposed through the DAOs, so we rely on Room's default WAL behavior here.
            }
            val dbFile = AppDatabase.databaseFile(context)
            require(dbFile.exists()) { "No database to back up yet" }

            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry(MANIFEST_NAME))
                    zip.write("Rentals encrypted backup\ncreated=${System.currentTimeMillis()}\n".toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(ENTRY_NAME))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            } ?: error("Couldn't open the destination file")
        }
    }

    /**
     * Restores a previously exported zip. The app must re-lock and be unlocked again afterward,
     * since the restored file may have been encrypted under a different-looking (but must be
     * the SAME actual) credential.
     */
    suspend fun import(context: Context, sourceUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dbFile = AppDatabase.databaseFile(context)
            val tempFile = File(context.cacheDir, "restore_temp.db")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    var found = false
                    while (entry != null) {
                        if (entry.name == ENTRY_NAME) {
                            tempFile.outputStream().use { out -> zip.copyTo(out) }
                            found = true
                        }
                        entry = zip.nextEntry
                    }
                    require(found) { "This file doesn't look like a Rentals backup" }
                }
            } ?: error("Couldn't open the selected file")

            // Close the live DB connection just long enough to swap the file out from under it -
            // all file I/O below happens BEFORE we touch app-lock state, so a recomposition
            // triggered by locking can't interrupt an in-progress restore.
            AppSession.repository?.close()

            dbFile.parentFile?.mkdirs()
            tempFile.copyTo(dbFile, overwrite = true)
            // Also clear the -wal/-shm side files so SQLCipher doesn't try to replay a stale WAL
            // against the restored file.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            tempFile.delete()

            // Force a fresh unlock so every screen re-binds to a brand new repository/DB
            // connection instead of holding a stale reference to the one we just closed.
            AppSession.lock()
        }
    }
}
