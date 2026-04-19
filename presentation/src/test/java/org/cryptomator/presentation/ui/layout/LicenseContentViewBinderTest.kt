package org.cryptomator.presentation.ui.layout

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ViewLicenseCheckContentBinding
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicenseContentViewBinderTest {

	private lateinit var binding: ViewLicenseCheckContentBinding
	private lateinit var context: Context

	@Before
	fun setUp() {
		context = ApplicationProvider.getApplicationContext()
		context.setTheme(R.style.AppTheme)
		binding = ViewLicenseCheckContentBinding.inflate(LayoutInflater.from(context))
	}

	@Test
	fun `bindTrialState with no trial shows trial button group and hides badge and expiration`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = false)

		binder.bindTrialState(active = false, expired = false, expirationText = null)

		assertThat(binding.trialButtonGroup.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvTrialStatusBadge.visibility, `is`(View.GONE))
		assertThat(binding.tvTrialExpiration.visibility, `is`(View.GONE))
		assertThat(binding.btnTrial.isEnabled, `is`(true))
	}

	@Test
	fun `bindTrialState with active trial hides button and shows active badge and expiration`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = false)
		val expirationText = "Expiration Date: 2026-05-19"

		binder.bindTrialState(active = true, expired = false, expirationText = expirationText)

		assertThat(binding.trialButtonGroup.visibility, `is`(View.GONE))
		assertThat(binding.tvTrialStatusBadge.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvTrialStatusBadge.text.toString(), `is`(context.getString(R.string.screen_license_check_trial_status_active)))
		assertThat(binding.tvTrialExpiration.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvTrialExpiration.text.toString(), `is`(expirationText))
		assertThat(binding.tvInfoText.visibility, `is`(View.GONE))
	}

	@Test
	fun `bindTrialState with expired trial shows expired badge and info text`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = false)
		val expirationText = "Expiration Date: 2026-03-19"

		binder.bindTrialState(active = false, expired = true, expirationText = expirationText)

		assertThat(binding.trialButtonGroup.visibility, `is`(View.GONE))
		assertThat(binding.tvTrialStatusBadge.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvTrialStatusBadge.text.toString(), `is`(context.getString(R.string.screen_license_check_trial_status_expired)))
		assertThat(binding.tvTrialExpiration.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvTrialExpiration.text.toString(), `is`(expirationText))
		assertThat(binding.tvInfoText.visibility, `is`(View.VISIBLE))
		assertThat(binding.tvInfoText.text.toString(), `is`(context.getString(R.string.screen_license_check_trial_expired_info)))
	}

	@Test
	fun `bindPurchaseState on non-freemium with paid license hides trial row and info text and disables purchase button`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = false)

		binder.bindPurchaseState(unlocked = true, hasPaidLicense = true)

		assertThat(binding.rowTrial.visibility, `is`(View.GONE))
		assertThat(binding.tvInfoText.visibility, `is`(View.GONE))
		assertThat(binding.btnPurchase.isEnabled, `is`(false))
	}

	@Test
	fun `bindPurchaseState on freemium with paid license hides purchase options and trial surfaces`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = true)

		binder.bindPurchaseState(unlocked = true, hasPaidLicense = true)

		assertThat(binding.purchaseOptionsGroup.visibility, `is`(View.GONE))
		assertThat(binding.tvRestorePurchase.visibility, `is`(View.GONE))
		assertThat(binding.tvInfoText.visibility, `is`(View.GONE))
		assertThat(binding.tvTrialStatusBadge.visibility, `is`(View.GONE))
		assertThat(binding.tvTrialExpiration.visibility, `is`(View.GONE))
	}

	@Test
	fun `transitioning from expired trial to paid license clears info text`() {
		val binder = LicenseContentViewBinder(binding, isFreemiumFlavor = false)

		binder.bindTrialState(active = false, expired = true, expirationText = "Expiration Date: 2026-03-19")
		binder.bindPurchaseState(unlocked = true, hasPaidLicense = true)

		assertThat(binding.rowTrial.visibility, `is`(View.GONE))
		assertThat(binding.tvInfoText.visibility, `is`(View.GONE))
	}
}
