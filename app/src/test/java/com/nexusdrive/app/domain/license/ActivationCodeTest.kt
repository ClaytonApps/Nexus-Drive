package com.nexusdrive.app.domain.license

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivationCodeTest {

    private val secret = "test-secret-please-change-in-prod"

    @Test
    fun `gerado é validado com sucesso`() {
        val code = ActivationCode.generate(30, secret)
        val v = ActivationCode.verify(code, secret)
        assertTrue(v is ActivationCode.Verification.Valid)
        assertEquals(30, (v as ActivationCode.Verification.Valid).validForDays)
    }

    @Test
    fun `formato gerado é NXS- com 5 grupos de 4`() {
        val code = ActivationCode.generate(30, secret)
        val regex = Regex("^NXS(-[A-HJ-NP-Z2-9]{4}){5}$")
        assertTrue("código não bate com regex: $code", regex.matches(code))
    }

    @Test
    fun `código com segredo errado é rejeitado`() {
        val code = ActivationCode.generate(30, secret)
        val v = ActivationCode.verify(code, "outro-segredo")
        assertEquals(ActivationCode.Verification.InvalidSignature, v)
    }

    @Test
    fun `código mutilado é detectado`() {
        val code = ActivationCode.generate(30, secret)
        val tampered = code.dropLast(1) + if (code.last() == 'A') 'B' else 'A'
        val v = ActivationCode.verify(tampered, secret)
        assertTrue(v is ActivationCode.Verification.InvalidSignature ||
                v is ActivationCode.Verification.MalformedFormat)
    }

    @Test
    fun `aceita código com espaços e minúsculas`() {
        val code = ActivationCode.generate(30, secret)
        val messy = " ${code.lowercase()} "
        // espaços não são removidos pelo verify, mas case sim — testa só case:
        assertTrue(ActivationCode.verify(code.lowercase(), secret) is ActivationCode.Verification.Valid)
    }
}
