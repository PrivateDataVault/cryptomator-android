package org.cryptomator.presentation.ui.layout

import android.content.Intent
import android.net.Uri
import android.view.View
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ViewLicenseCheckContentBinding

/** Shared visibility-toggling logic for the license check content included layout. */
class LicenseContentViewBinder(
	private val binding: ViewLicenseCheckContentBinding,
	private val isIapFlavor: Boolean
) {

	private val context get() = binding.root.context

	/** Sets the initial visibility state and button defaults for IAP mode. */
	fun bindInitialIapLayout() {
		binding.licenseEntryGroup.visibility = View.GONE
		binding.btnPurchase.visibility = View.GONE
		binding.tvLicenseLink.visibility = View.GONE
		binding.purchaseOptionsGroup.visibility = View.VISIBLE
		binding.tvRestorePurchase.visibility = View.VISIBLE
		binding.legalLinksGroup.visibility = View.VISIBLE
		binding.btnSubscription.isEnabled = false
		binding.btnLifetime.isEnabled = false
	}

	/** Sets the initial visibility state for license-entry (non-IAP) mode. */
	fun bindInitialLicenseEntryLayout() {
		binding.licenseEntryGroup.visibility = View.VISIBLE
		binding.purchaseOptionsGroup.visibility = View.GONE
		binding.tvRestorePurchase.visibility = View.GONE
		binding.legalLinksGroup.visibility = View.GONE
		binding.tvLicenseLink.visibility = View.VISIBLE
		binding.tvLicenseLink.text = context.getString(R.string.dialog_enter_license_content)
	}

	/** Sets click listeners on Terms and Privacy links. */
	fun bindLegalLinks() {
		binding.tvTerms.setOnClickListener {
			context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/terms/")))
		}
		binding.tvPrivacy.setOnClickListener {
			context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/privacy/")))
		}
	}

	/** Updates subscription and lifetime button text and enabled state from resolved prices. */
	fun bindProductPrices(subscriptionPrice: String?, lifetimePrice: String?) {
		if (!subscriptionPrice.isNullOrEmpty()) {
			binding.btnSubscription.text = subscriptionPrice
			binding.btnSubscription.isEnabled = true
		}
		if (!lifetimePrice.isNullOrEmpty()) {
			binding.btnLifetime.text = lifetimePrice
			binding.btnLifetime.isEnabled = true
		}
	}

	/** Updates purchase-related view visibility based on license state. */
	fun bindPurchaseState(unlocked: Boolean, hasPaidLicense: Boolean) {
		if (isIapFlavor) {
			binding.purchaseOptionsGroup.visibility = if (hasPaidLicense) View.GONE else View.VISIBLE
			binding.tvRestorePurchase.visibility = if (hasPaidLicense) View.GONE else View.VISIBLE
			if (hasPaidLicense) {
				binding.tvInfoText.visibility = View.GONE
			}
		} else {
			binding.btnPurchase.isEnabled = !unlocked
		}
	}

	/** Updates trial-related view visibility based on trial state. */
	fun bindTrialState(active: Boolean, expired: Boolean, expirationText: String?) {
		if (active || expired) {
			binding.trialButtonGroup.visibility = View.GONE
			binding.tvTrialStatusBadge.visibility = View.VISIBLE
			binding.tvTrialStatusBadge.text = context.getString(
				if (active) R.string.screen_license_check_trial_status_active
				else R.string.screen_license_check_trial_status_expired
			)
			binding.tvTrialExpiration.visibility = View.VISIBLE
			binding.tvTrialExpiration.text = expirationText
			if (expired) {
				binding.tvInfoText.visibility = View.VISIBLE
				binding.tvInfoText.text = context.getString(R.string.screen_license_check_trial_expired_info)
			} else {
				binding.tvInfoText.visibility = View.GONE
			}
		} else {
			binding.trialButtonGroup.visibility = View.VISIBLE
			binding.tvTrialStatusBadge.visibility = View.GONE
			binding.tvTrialExpiration.visibility = View.GONE
			binding.tvInfoText.visibility = View.GONE
			binding.btnTrial.isEnabled = true
		}
	}
}
