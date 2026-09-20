package app.orionmd.management.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Holds the unlocked database/repository for as long as the process lives. There is no
 * persisted "unlocked" flag anywhere - a fresh process always starts locked, and the DB can only
 * be opened by re-deriving its passphrase from a correct PIN/pattern/password via LockManager.
 */
object AppSession {
    var repository: RentalsRepository? = null
        private set

    var isUnlocked by mutableStateOf(false)
        private set

    private var rawKey: ByteArray? = null

    suspend fun unlock(context: Context, derivedKey: ByteArray) {
        repository?.close()
        val db = AppDatabase.open(context, derivedKey)
        AppDatabase.seedDefaultsIfNeeded(db)
        repository = RentalsRepository(db)
        rawKey = derivedKey
        isUnlocked = true
    }

    /**
     * Re-keys the underlying SQLCipher database to [newKey]. IMPORTANT: the live [repository]
     * connection is closed FIRST - `PRAGMA rekey` rewrites the whole database file, and leaving
     * the existing connection open while a second one performs that rewrite risks the file being
     * left unreadable under either key. If the rekey itself fails, the original connection (still
     * valid, since the on-disk file was never actually touched) is restored before the failure is
     * rethrown, so the caller can show an error without also losing access to the current data.
     */
    suspend fun rekey(context: Context, newKey: ByteArray) {
        val currentKey = rawKey ?: return
        repository?.close()
        val db = AppDatabase.open(context, currentKey)
        try {
            val hex = newKey.joinToString("") { "%02x".format(it) }
            db.openHelper.writableDatabase.execSQL("PRAGMA rekey = \"x'$hex'\";")
        } catch (e: Exception) {
            db.close()
            unlock(context, currentKey)
            throw e
        }
        db.close()
        unlock(context, newKey)
    }

    fun currentRawKey(): ByteArray? = rawKey

    fun lock() {
        repository?.close()
        repository = null
        rawKey = null
        isUnlocked = false
    }
}
