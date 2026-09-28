package app.termosh.core.security

import app.termosh.core.security.model.SshKeyType
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyPairGenerator @Inject constructor() {

    fun generate(type: SshKeyType): KeyPair {
        val generator = KeyPairGenerator.getInstance(type.jcaName)
        when (type) {
            SshKeyType.ED25519 -> Unit
            SshKeyType.RSA -> generator.initialize(
                RSAKeyGenParameterSpec(4096, RSAKeyGenParameterSpec.F4)
            )
            SshKeyType.ECDSA_P256 -> generator.initialize(ECGenParameterSpec("secp256r1"))
        }
        return generator.generateKeyPair()
    }

    fun toOpenSshPublicKey(pair: KeyPair, comment: String? = null): String {
        val pub: PublicKey = pair.public
        val encoded: ByteArray = when (pub) {
            is RSAPublicKey -> encodeRsa(pub)
            is ECPublicKey -> encodeEc(pub)
            else -> encodeEd25519(pub.encoded)
        }
        val b64 = Base64.getEncoder().encodeToString(encoded)
        val type = sshTypeName(pub)
        val suffix = comment?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "$type $b64$suffix"
    }

    private fun sshTypeName(pub: PublicKey): String = when (pub) {
        is RSAPublicKey -> "ssh-rsa"
        is ECPublicKey -> "ecdsa-sha2-nistp256"
        else -> "ssh-ed25519"
    }

    private fun writeString(out: ByteArrayOutputStream, bytes: ByteArray) {
        out.write((bytes.size ushr 24) and 0xFF)
        out.write((bytes.size ushr 16) and 0xFF)
        out.write((bytes.size ushr 8) and 0xFF)
        out.write(bytes.size and 0xFF)
        out.write(bytes)
    }

    private fun encodeRsa(pub: RSAPublicKey): ByteArray {
        val out = ByteArrayOutputStream()
        writeString(out, "ssh-rsa".toByteArray())
        writeString(out, pub.publicExponent.toByteArray())
        writeString(out, pub.modulus.toByteArray())
        return out.toByteArray()
    }

    private fun encodeEc(pub: ECPublicKey): ByteArray {
        val out = ByteArrayOutputStream()
        writeString(out, "ecdsa-sha2-nistp256".toByteArray())
        writeString(out, "nistp256".toByteArray())
        writeString(out, encodeUncompressedPoint(pub))
        return out.toByteArray()
    }

    /**
     * Собирает uncompressed-точку по RFC 5480: 0x04 || X || Y,
     * каждая координата дополнена нулями до размера поля.
     */
    private fun encodeUncompressedPoint(pub: ECPublicKey): ByteArray {
        val coordSize = (pub.params.curve.field.fieldSize + 7) / 8
        val x = toFixedLength(pub.w.affineX, coordSize)
        val y = toFixedLength(pub.w.affineY, coordSize)
        val result = ByteArray(1 + coordSize * 2)
        result[0] = 0x04
        System.arraycopy(x, 0, result, 1, coordSize)
        System.arraycopy(y, 0, result, 1 + coordSize, coordSize)
        return result
    }

    /**
     * BigInteger.toByteArray() может вернуть знаковый 0x00 в начале
     * или короче нужного размера. Приводим к ровно [size] байт.
     */
    private fun toFixedLength(value: BigInteger, size: Int): ByteArray {
        val bytes = value.toByteArray()
        return when {
            bytes.size == size -> bytes
            bytes.size == size + 1 && bytes[0] == 0.toByte() ->
                bytes.copyOfRange(1, bytes.size)
            bytes.size < size -> {
                val out = ByteArray(size)
                System.arraycopy(bytes, 0, out, size - bytes.size, bytes.size)
                out
            }
            else -> bytes.copyOfRange(bytes.size - size, bytes.size)
        }
    }

    private fun encodeEd25519(x509: ByteArray): ByteArray {
        require(x509.size >= 32) { "Unexpected Ed25519 SPKI size: ${x509.size}" }
        val raw = x509.copyOfRange(x509.size - 32, x509.size)
        val out = ByteArrayOutputStream()
        writeString(out, "ssh-ed25519".toByteArray())
        writeString(out, raw)
        return out.toByteArray()
    }
}
