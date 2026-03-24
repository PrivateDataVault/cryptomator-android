package org.cryptomator.presentation.licensing

import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LicenseEnforcerTest {

	private val sharedPreferencesHandler: SharedPreferencesHandler = mock()
	private lateinit var licenseEnforcer: LicenseEnforcer

	@BeforeEach
	fun setUp() {
		licenseEnforcer = LicenseEnforcer(sharedPreferencesHandler)
	}

	// -- hasWriteAccess --

	@Test
	fun `hasWriteAccess returns true when license token is present`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true when subscription is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(true)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns true when trial is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertTrue(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns false when trial is expired`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		assertFalse(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns false when no license and no trial and no subscription`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasWriteAccess())
	}

	// -- hasActiveTrial --

	@Test
	fun `hasActiveTrial returns true when trial expiration is in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertTrue(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when trial expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	@Test
	fun `hasActiveTrial returns false when no trial was started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasActiveTrial())
	}

	// -- hasExpiredTrial --

	@Test
	fun `hasExpiredTrial returns true when trial expiration is in the past`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		assertTrue(licenseEnforcer.hasExpiredTrial())
	}

	@Test
	fun `hasExpiredTrial returns false when trial is still active`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertFalse(licenseEnforcer.hasExpiredTrial())
	}

	@Test
	fun `hasExpiredTrial returns false when no trial was started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasExpiredTrial())
	}

	// -- startTrial --

	@Test
	fun `startTrial sets trial expiration date 30 days in the future`() {
		val before = System.currentTimeMillis()
		licenseEnforcer.startTrial()
		val after = System.currentTimeMillis()

		val captor = org.mockito.ArgumentCaptor.forClass(Long::class.java)
		org.mockito.Mockito.verify(sharedPreferencesHandler).setTrialExpirationDate(captor.capture())

		val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
		assertTrue(captor.value >= before + thirtyDaysMs)
		assertTrue(captor.value <= after + thirtyDaysMs)
	}
}
