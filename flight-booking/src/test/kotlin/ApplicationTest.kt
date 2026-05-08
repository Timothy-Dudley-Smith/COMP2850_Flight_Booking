package com.example.com

import com.flightsystem.flightservice.EncryptionService.generateSalt
import com.flightsystem.flightservice.EncryptionService.hashPassword
import com.flightsystem.flightservice.EncryptionService.verifyPassword
import com.flightsystem.flightservice.LoyaltyService
import com.flightsystem.flightservice.PaymentService
import com.flightsystem.model.Payment
import com.flightsystem.model.PaymentStatus
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.time.LocalDateTime
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testRoot() =
        testApplication {
            application {
                module()
            }
            client.get("/").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
}

class EncryptionServiceTest {
    @Test
    fun generateSaltTest() {
        val salt1 = generateSalt()
        val salt2 = generateSalt()

        val decoded1 = Base64.getDecoder().decode(salt1)
        val decoded2 = Base64.getDecoder().decode(salt2)

        assertNotEquals(salt1, salt2)
        assertEquals(16, decoded1.size)
        assertEquals(16, decoded2.size)
    }

    @Test
    fun hashPasswordTestAndVerifyPassword() {
        val password1 = "example"
        val password2 = "password"

        val salt1 = generateSalt()
        val salt2 = generateSalt()

        val hashedPassword1 = hashPassword(password1, salt1)
        val hashedPassword1Copy = hashPassword(password1, salt1)
        val hashedPassword2 = hashPassword(password2, salt2)

        val mixedHashedPassword1 = hashPassword(password1, salt2)

        assertEquals(hashedPassword1, hashedPassword1Copy)

        assertTrue(verifyPassword(password1, hashedPassword1, salt1))
        assertFalse(verifyPassword("Wrong", hashedPassword1, salt1))
        assertTrue(verifyPassword(password2, hashedPassword2, salt2))
        assertNotEquals(password1, hashedPassword1)
        assertNotEquals(password2, hashedPassword2)
        assertNotEquals(hashedPassword1, mixedHashedPassword1)
    }
}

class LoyaltyServiceTest {
    val service = LoyaltyService()

    @Test
    fun applyDiscountTest() {
        val discounted1 = service.applyDiscount(200.00, 1000)
        val discounted2 = service.applyDiscount(200.00, 1000000)

        assertEquals(100.00, discounted1)
        assertEquals(0.0, discounted2)
        assertFailsWith(IllegalArgumentException::class) { service.applyDiscount(200.00, -1) }
        assertFailsWith(IllegalArgumentException::class) { service.applyDiscount(-200.00, 1) }
        assertFailsWith(IllegalArgumentException::class) { service.applyDiscount(-200.00, -1) }
    }
}

class PaymentServiceTest {
    val service = PaymentService()

    @Test
    fun validateCardTest() {
        val correctDetails =
            service.validateCard(
                "4111111111111111",
                "Test Name",
                9,
                2029,
                "301",
            )

        assertTrue(correctDetails.isSuccess)

        val shortCardNumber =
            service.validateCard(
                "411111111111111",
                "Test Name",
                9,
                2029,
                "301",
            )

        assertTrue(shortCardNumber.isFailure)

        val noName =
            service.validateCard(
                "4111111111111111",
                "",
                9,
                2029,
                "301",
            )

        assertTrue(noName.isFailure)

        val mistypedCardNumber =
            service.validateCard(
                "4111111111112111",
                "Test Name",
                9,
                2029,
                "301",
            )

        assertTrue(mistypedCardNumber.isFailure)

        val recentlyExpiredCard =
            service.validateCard(
                "4111111111111111",
                "Test Name",
                4,
                2026,
                "301",
            )

        assertTrue(recentlyExpiredCard.isFailure)

        val veryExpiredCard =
            service.validateCard(
                "4111111111111111",
                "Test Name",
                9,
                2020,
                "301",
            )

        assertTrue(veryExpiredCard.isFailure)

        val badExpiryMonth =
            service.validateCard(
                "4111111111111111",
                "Test Name",
                14,
                2029,
                "301",
            )

        assertTrue(badExpiryMonth.isFailure)

        val shortCSV =
            service.validateCard(
                "4111111111111111",
                "Test Name",
                9,
                2029,
                "30",
            )

        assertTrue(shortCSV.isFailure)
    }
}

class PaymentTest {
    @Test
    fun setSuccessTest() {
        val payment =
            Payment(
                "1",
                "1",
                1,
                100.00,
                "3948",
                "name",
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                null,
            )

        assertNotEquals(payment.status, PaymentStatus.SUCCESS)
        payment.setStatusSuccess()
        assertEquals(PaymentStatus.SUCCESS, payment.status)
    }

    @Test
    fun setFailureTest() {
        val payment =
            Payment(
                "1",
                "1",
                1,
                100.00,
                "3948",
                "name",
                PaymentStatus.PENDING,
                LocalDateTime.now(),
                null,
            )

        assertNotEquals(payment.status, PaymentStatus.FAILED)
        payment.setStatusFailed()
        assertEquals(PaymentStatus.FAILED, payment.status)
    }

    @Test
    fun setRefundedTest() {
        val payment =
            Payment(
                "1",
                "1",
                1,
                100.00,
                "3948",
                "name",
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                null,
            )

        assertNotEquals(payment.status, PaymentStatus.REFUNDED)
        payment.setRefunded()
        assertEquals(PaymentStatus.REFUNDED, payment.status)
        assertNotNull(payment.refundedAt)
    }

    @Test
    fun isRefundableTest() {
        val payment =
            Payment(
                "1",
                "1",
                1,
                100.00,
                "3948",
                "name",
                PaymentStatus.SUCCESS,
                LocalDateTime.now(),
                null,
            )
        assertEquals(payment.status, PaymentStatus.SUCCESS)
        assertTrue(payment.isRefundable())

        val payment2 =
            Payment(
                "1",
                "1",
                1,
                100.00,
                "3948",
                "name",
                PaymentStatus.FAILED,
                LocalDateTime.now(),
                null,
            )

        assertNotEquals(payment2.status, PaymentStatus.SUCCESS)
        assertFalse(payment2.isRefundable())
    }
}
