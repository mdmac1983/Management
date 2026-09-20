package app.orionmd.management.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles all cryptography for Rentals.
 *
 * Two layers, matching the pattern used across the other OrionMD apps:
 *  1. The user's PIN/pattern/password is stretched via PBKDF2 into a raw key. That raw key
 *     becomes the SQLCipher passphrase for the entire on-device database (bookings, customers,
 *     everything) - so the whole DB is unreadable without the correct credential.
 *  2. A separate Android Keystore AES key wraps a random "verifier" blob so we can check a
 *     PIN/pattern/password attempt is correct WITHOUT ever storing the credential itself.
 *
 * Changing the lock credential re-derives the passphrase and re-keys the database (see
 * LockManager.changeCredential / RentalsRepository.rekeyDatabase).
 */
object CryptoManager {

    private const val KEYSTORE_ALIAS = "rentals_verifier_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val GCM_TAG_LENGTH_BITS = 128

    /** Derives a stable 256-bit key from a credential string + salt (PBKDF2-HMAC-SHA256). */
    fun deriveKey(credential: String, salt: ByteArray): ByteArray {
        val spec: KeySpec = PBEKeySpec(credential.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    /** Renders a derived key as the hex string SQLCipher expects for a raw passphrase (x'...'). */
    fun toSqlCipherKey(rawKey: ByteArray): String {
        val hex = rawKey.joinToString("") { "%02x".format(it) }
        return "x'$hex'"
    }

    fun randomSalt(size: Int = 16): ByteArray {
        val salt = ByteArray(size)
        SecureRandom().nextBytes(salt)
        return salt
    }

    fun sha256(input: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(input)

    // ---- Keystore-backed verifier, used for a fast "is this PIN/pattern/password right?"
    // ---- check before we touch the (much slower) SQLCipher-keyed database open. ----

    private fun getOrCreateKeystoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_LENGTH_BITS)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Encrypts [plainText] with the Keystore key. Returns iv+ciphertext, both Base64-able by caller. */
    fun keystoreEncrypt(plainText: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKeystoreKey())
        val cipherText = cipher.doFinal(plainText)
        return cipher.iv to cipherText
    }

    fun keystoreDecrypt(iv: ByteArray, cipherText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKeystoreKey(), spec)
        return cipher.doFinal(cipherText)
    }

    fun deleteKeystoreKey() {
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            keyStore.deleteEntry(KEYSTORE_ALIAS)
        }
    }

    // ---- Generic field-level AES-GCM, used for anything encrypted independently of the
    // ---- SQLCipher DB passphrase itself (e.g. the exported backup file). ----

    fun encryptWithKey(rawKey: ByteArray, plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(rawKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText)
        return iv + cipherText
    }

    fun decryptWithKey(rawKey: ByteArray, ivAndCipherText: ByteArray): ByteArray {
        val iv = ivAndCipherText.copyOfRange(0, 12)
        val cipherText = ivAndCipherText.copyOfRange(12, ivAndCipherText.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(rawKey, "AES")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        return cipher.doFinal(cipherText)
    }
}
