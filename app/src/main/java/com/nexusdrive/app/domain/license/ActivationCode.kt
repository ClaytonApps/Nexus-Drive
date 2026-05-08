package com.nexusdrive.app.domain.license

import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Geração e validação de códigos de ativação no formato:
 *
 *     NXS-XXXX-XXXX-XXXX-XXXX-XXXX
 *
 * Cada código é um payload binário de 12 bytes codificado em base32:
 *
 *   [1 byte version][2 bytes days BE][5 bytes nonce][4 bytes hmac]
 *
 * O `hmac` é HMAC-SHA256(version||days||nonce, SECRET) truncado nos
 * primeiros 4 bytes — o suficiente para impedir geração por força
 * bruta (probabilidade 1 em 2^32 de acertar um código falso).
 *
 * ----------------------------------------------------------------------
 * AVISO DE SEGURANÇA
 * ----------------------------------------------------------------------
 * O `SECRET` está embutido no APK. Quem decompilar o app consegue
 * gerar códigos válidos. Para um produto pago em escala, mova a
 * validação para o seu backend (`POST /v1/license/validate`).
 * Esta implementação é adequada para MVP / venda direta para
 * clientes de confiança.
 * ----------------------------------------------------------------------
 */
object ActivationCode {

    private const val VERSION: Byte = 0x01
    private const val PAYLOAD_BYTES = 12 // 1 + 2 + 5 + 4
    private const val PREFIX = "NXS"

    // Alfabeto base32 RFC 4648 SEM padding, sem 0/1/8/O/I (visualmente confusos).
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /**
     * Gera um código de ativação para `validForDays` dias.
     * Uso típico: 30 dias.
     */
    fun generate(validForDays: Int, secret: String, random: SecureRandom = SecureRandom()): String {
        require(validForDays in 1..65535) { "validForDays fora da faixa" }
        val nonce = ByteArray(5).also { random.nextBytes(it) }
        val unsigned = byteArrayOf(
            VERSION,
            (validForDays shr 8 and 0xFF).toByte(),
            (validForDays and 0xFF).toByte(),
            nonce[0], nonce[1], nonce[2], nonce[3], nonce[4]
        )
        val mac = hmacShort(unsigned, secret)
        val full = unsigned + mac
        return format(encodeBase32(full))
    }

    /** Resultado de uma tentativa de validação. */
    sealed class Verification {
        data class Valid(val validForDays: Int) : Verification()
        data object MalformedFormat : Verification()
        data object UnknownVersion : Verification()
        data object InvalidSignature : Verification()
    }

    fun verify(rawCode: String, secret: String): Verification {
        val cleaned = rawCode.uppercase().replace("-", "").removePrefix(PREFIX)
        val bytes = decodeBase32(cleaned) ?: return Verification.MalformedFormat
        if (bytes.size != PAYLOAD_BYTES) return Verification.MalformedFormat
        if (bytes[0] != VERSION) return Verification.UnknownVersion

        val unsigned = bytes.copyOfRange(0, 8)
        val expectedMac = hmacShort(unsigned, secret)
        val actualMac = bytes.copyOfRange(8, 12)
        if (!constantTimeEquals(expectedMac, actualMac)) {
            return Verification.InvalidSignature
        }
        val days = ((bytes[1].toInt() and 0xFF) shl 8) or (bytes[2].toInt() and 0xFF)
        return Verification.Valid(days)
    }

    private fun hmacShort(data: ByteArray, secret: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data).copyOfRange(0, 4)
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun format(body: String): String {
        // Quebra em grupos de 4: NXS-XXXX-XXXX-XXXX-XXXX-XXXX
        val groups = body.chunked(4)
        return (listOf(PREFIX) + groups).joinToString("-")
    }

    private fun encodeBase32(bytes: ByteArray): String {
        val sb = StringBuilder()
        var buffer = 0
        var bits = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                sb.append(ALPHABET[(buffer shr bits) and 0x1F])
            }
        }
        if (bits > 0) sb.append(ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        return sb.toString()
    }

    private fun decodeBase32(s: String): ByteArray? {
        if (s.isEmpty()) return null
        val out = mutableListOf<Byte>()
        var buffer = 0
        var bits = 0
        for (c in s) {
            val idx = ALPHABET.indexOf(c)
            if (idx < 0) return null
            buffer = (buffer shl 5) or idx
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }
}
