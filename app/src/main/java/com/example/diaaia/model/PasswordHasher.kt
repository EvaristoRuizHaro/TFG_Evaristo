package com.example.diaaia.model

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utilidad para hashear contraseñas con SHA-256 + sal aleatoria.
 *
 * Formato de almacenamiento: "<saltBase16>:<hashBase16>"
 * El hash se calcula como SHA-256(salt || password).
 *
 * No usamos BCrypt para evitar dependencias externas; SHA-256 + sal es suficiente
 * para el alcance de un TFG académico y ya es infinitamente mejor que guardar
 * la contraseña en plano.
 */
object PasswordHasher {

    private const val SALT_BYTES = 16

    /** Genera el almacenado 'salt:hash' a partir de una contraseña en plano. */
    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val hashBytes = sha256(salt + password.toByteArray(Charsets.UTF_8))
        return salt.toHex() + ":" + hashBytes.toHex()
    }

    /** Verifica que una contraseña en plano corresponde al hash almacenado. */
    fun verify(password: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = parts[0].fromHex() ?: return false
        val expected = parts[1].fromHex() ?: return false
        val actual = sha256(salt + password.toByteArray(Charsets.UTF_8))
        return actual.contentEquals(expected)
    }

    private fun sha256(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data)
    }

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { b -> "%02x".format(b) }

    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return ByteArray(length / 2) { i ->
            substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
