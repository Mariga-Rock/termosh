package app.termosh.domain.repository

import app.termosh.domain.model.SecretRef

/**
 * Абстракция над CryptoManager из :core:security.
 * Домен оперирует только SecretRef, инфраструктура — байтами.
 */
interface SecretCodec {

    fun encrypt(plaintext: ByteArray): SecretRef

    fun decrypt(ref: SecretRef): ByteArray

    fun encryptString(plaintext: String): SecretRef =
        encrypt(plaintext.toByteArray(Charsets.UTF_8))

    fun decryptString(ref: SecretRef): String =
        decrypt(ref).toString(Charsets.UTF_8)
}
