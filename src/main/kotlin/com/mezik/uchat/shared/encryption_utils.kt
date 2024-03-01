package com.mezik.uchat.shared

import java.security.Key
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

const val DEFAULT_CIPHER_ALGO = "RSA/ECB/PKCS1Padding"

object EncryptionUtils {
    private val symmetricKeyGenerator = KeyGenerator.getInstance("AES").also {
        it.init(256)
    }

    fun generateSymmetricKey(): SecretKey {
        return symmetricKeyGenerator.generateKey()
    }

    fun encrypt(data: ByteArray, key: Key, transformation: String): Result<ByteArray> = runCatching {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.doFinal(data)
    }
}