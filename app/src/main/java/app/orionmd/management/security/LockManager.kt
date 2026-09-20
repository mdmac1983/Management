package app.orionmd.management.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

enum class CredentialType { PIN, PATTERN, PASSWORD }

/**
 * Owns the app-lock credential (PIN / pattern / password) and turns a correct attempt into the
 * raw key used to open the SQLCipher database. Nothing here ever stores the credential itself -
 * only a PBKDF2 salt and a Keystore-wrapped verifier hash used to check attempts quickly.
 */
class LockManager(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "rentals_lock_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isCredentialSet(): Boolean = prefs.contains(KEY_SALT)

    fun credentialType(): CredentialType? =
        prefs.getString(KEY_TYPE, null)?.let { CredentialType.valueOf(it) }

    /** Normalizes a pattern (list of dot indices 0-8) into a stable string for key derivation. */
    fun patternToString(dots: List<Int>): String = dots.joinToString(",")

    /** A derived key that hasn't been committed as the active credential yet - see [prepareCredential]. */
    class PendingCredential internal constructor(
        internal val type: CredentialType,
        internal val salt: ByteArray,
        val derivedKey: ByteArray
    )

    /**
     * Derives the key for a new credential WITHOUT persisting anything yet. Used by Settings >
     * Change Lock so the database can be re-keyed FIRST, with the old credential only overwritten
     * (via [commitCredential]) once that re-key has actually succeeded - otherwise a failed re-key
     * (e.g. a locked database file) would leave the stored credential pointing at a passphrase the
     * on-disk database was never actually encrypted with, permanently locking the owner out.
     */
    fun prepareCredential(type: CredentialType, rawCredential: String): PendingCredential {
        val salt = CryptoManager.randomSalt()
        val derivedKey = CryptoManager.deriveKey(rawCredential, salt)
        return PendingCredential(type, salt, derivedKey)
    }

    /** Persists a [PendingCredential] as the active one. Only call this after the database itself
     *  has been successfully re-keyed to [PendingCredential.derivedKey] (or, for first-time setup,
     *  immediately - there's no existing database to protect yet). */
    fun commitCredential(pending: PendingCredential) {
        val verifierHash = CryptoManager.sha256(pending.derivedKey)
        val (iv, encryptedHash) = CryptoManager.keystoreEncrypt(verifierHash)

        prefs.edit()
            .putString(KEY_TYPE, pending.type.name)
            .putString(KEY_SALT, Base64.encodeToString(pending.salt, Base64.NO_WRAP))
            .putString(KEY_VERIFIER_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_VERIFIER_HASH, Base64.encodeToString(encryptedHash, Base64.NO_WRAP))
            .apply()
    }

    /** Convenience for first-time onboarding, where there's no existing encrypted database to
     *  protect against a half-committed state - derive and commit in one step, as before. */
    fun setCredential(type: CredentialType, rawCredential: String): ByteArray {
        val pending = prepareCredential(type, rawCredential)
        commitCredential(pending)
        return pending.derivedKey
    }

    /** Returns the derived DB key if [attempt] is correct, or null if it doesn't match. */
    fun verify(attempt: String): ByteArray? {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return null
        val ivB64 = prefs.getString(KEY_VERIFIER_IV, null) ?: return null
        val hashB64 = prefs.getString(KEY_VERIFIER_HASH, null) ?: return null

        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        val storedEncryptedHash = Base64.decode(hashB64, Base64.NO_WRAP)

        val derivedKey = CryptoManager.deriveKey(attempt, salt)
        val attemptHash = CryptoManager.sha256(derivedKey)

        val storedHash = try {
            CryptoManager.keystoreDecrypt(iv, storedEncryptedHash)
        } catch (e: Exception) {
            // Keystore key lost (e.g. device restore) - credential can't be verified anymore.
            return null
        }

        return if (storedHash.contentEquals(attemptHash)) derivedKey else null
    }

    /** Wipes the lock credential entirely. Used only alongside a full data reset. */
    fun clearCredential() {
        prefs.edit().clear().apply()
        CryptoManager.deleteKeystoreKey()
    }

    companion object {
        private const val KEY_TYPE = "credential_type"
        private const val KEY_SALT = "credential_salt"
        private const val KEY_VERIFIER_IV = "verifier_iv"
        private const val KEY_VERIFIER_HASH = "verifier_hash"
    }
}
