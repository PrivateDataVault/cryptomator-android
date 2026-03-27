package org.cryptomator.presentation.licensing

import android.content.Context
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.R
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
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
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		assertFalse(licenseEnforcer.hasWriteAccess())
	}

	@Test
	fun `hasWriteAccess returns false when no license and no trial and no subscription`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		assertFalse(licenseEnforcer.hasWriteAccess())
	}

	// -- hasPaidLicense --

	@Test
	fun `hasPaidLicense returns true when license token is present`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("some-token")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns true when subscription is active`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(true)

		assertTrue(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns false when only trial is active`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		assertFalse(licenseEnforcer.hasPaidLicense())
	}

	@Test
	fun `hasPaidLicense returns false when no license and no subscription`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)

		assertFalse(licenseEnforcer.hasPaidLicense())
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

	// -- startTrial --

	@Test
	fun `startTrial sets trial expiration date 30 days in the future`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)
		val before = System.currentTimeMillis()
		licenseEnforcer.startTrial()
		val after = System.currentTimeMillis()

		val captor = org.mockito.ArgumentCaptor.forClass(Long::class.java)
		org.mockito.Mockito.verify(sharedPreferencesHandler).setTrialExpirationDate(captor.capture())

		val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
		assertTrue(captor.value >= before + thirtyDaysMs)
		assertTrue(captor.value <= after + thirtyDaysMs)
	}

	@Test
	fun `startTrial does not overwrite existing active trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)

		licenseEnforcer.startTrial()

		org.mockito.Mockito.verify(sharedPreferencesHandler, org.mockito.Mockito.never()).setTrialExpirationDate(org.mockito.ArgumentMatchers.anyLong())
	}

	@Test
	fun `startTrial does not overwrite expired trial`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		licenseEnforcer.startTrial()

		org.mockito.Mockito.verify(sharedPreferencesHandler, org.mockito.Mockito.never()).setTrialExpirationDate(org.mockito.ArgumentMatchers.anyLong())
	}

	// -- evaluateTrialState --

	@Test
	fun `evaluateTrialState returns active with formatted date when trial is active`() {
		val futureDate = System.currentTimeMillis() + 86400000L
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(futureDate)

		val state = licenseEnforcer.evaluateTrialState()

		assertTrue(state.isActive)
		assertFalse(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns expired with formatted date when trial is expired`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertTrue(state.isExpired)
		assertNotNull(state.formattedExpirationDate)
	}

	@Test
	fun `evaluateTrialState returns inactive and not expired when no trial started`() {
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val state = licenseEnforcer.evaluateTrialState()

		assertFalse(state.isActive)
		assertFalse(state.isExpired)
		assertNull(state.formattedExpirationDate)
	}

	// -- evaluateUiState --

	@Test
	fun `evaluateUiState returns active trial with expiration text`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		val context: Context = mock()
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() + 86400000L)
		`when`(context.getString(org.mockito.ArgumentMatchers.eq(R.string.screen_license_check_trial_expiration), org.mockito.ArgumentMatchers.any())).thenReturn("Expiration Date: Mar 28, 2026")

		val uiState = licenseEnforcer.evaluateUiState(context)

		assertTrue(uiState.hasWriteAccess)
		assertFalse(uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isActive)
		assertNotNull(uiState.trialExpirationText)
	}

	@Test
	fun `evaluateUiState returns expired trial with expired info text`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		val context: Context = mock()
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(System.currentTimeMillis() - 1000L)
		`when`(context.getString(R.string.screen_license_check_trial_expired_info)).thenReturn("Your trial has expired.")

		val uiState = licenseEnforcer.evaluateUiState(context)

		assertFalse(uiState.hasWriteAccess)
		assertFalse(uiState.hasPaidLicense)
		assertTrue(uiState.trialState.isExpired)
		assertNotNull(uiState.trialExpirationText)
	}

	@Test
	fun `evaluateUiState returns null expiration text when no trial started`() {
		assumeTrue(BuildConfig.FLAVOR != "playstore" && BuildConfig.FLAVOR != "accrescent", "Licensing logic is bypassed on this flavor")
		val context: Context = mock()
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")
		`when`(sharedPreferencesHandler.hasRunningSubscription()).thenReturn(false)
		`when`(sharedPreferencesHandler.trialExpirationDate()).thenReturn(0L)

		val uiState = licenseEnforcer.evaluateUiState(context)

		assertFalse(uiState.hasWriteAccess)
		assertFalse(uiState.hasPaidLicense)
		assertFalse(uiState.trialState.isActive)
		assertFalse(uiState.trialState.isExpired)
		assertNull(uiState.trialExpirationText)
	}
}
