package org.cryptomator.presentation.ui.fragment

import android.os.Bundle
import android.view.View
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.FragmentWelcomeLicenseBinding

@Fragment
class WelcomeLicenseFragment : BaseFragment<FragmentWelcomeLicenseBinding>(FragmentWelcomeLicenseBinding::inflate) {

	interface Listener {
		fun onSubmitLicense(license: String?)
		fun onOpenLicenseLink()
		fun onPurchaseClick()
		fun onSkipLicense()
	}

	private val isIapFlavor = BuildConfig.FLAVOR == "playstoreiap"
	private var listener: Listener? = null

	fun setListener(listener: Listener) {
		this.listener = listener
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupUi()
	}

	override fun setupView() {
		setupUi()
	}

	private fun setupUi() {
		binding.licenseContent.licenseEntryGroup.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.tvLicenseLink.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.btnPurchase.text = if (isIapFlavor) {
			getString(R.string.screen_license_check_button_purchase)
		} else {
			getString(R.string.dialog_enter_license_ok_button)
		}
		binding.licenseContent.tvLicenseLink.text = getString(R.string.dialog_enter_license_content)
		binding.licenseContent.tvLicenseLink.setOnClickListener { listener?.onOpenLicenseLink() }
		binding.licenseContent.btnPurchase.setOnClickListener {
			if (isIapFlavor) {
				listener?.onPurchaseClick()
			} else {
				listener?.onSubmitLicense(binding.licenseContent.etLicense.text?.toString())
			}
		}
	}

	fun updateUnlocked(unlocked: Boolean) {
		binding.licenseContent.btnPurchase.isEnabled = !unlocked
		binding.licenseContent.tvUnlocked.visibility = if (unlocked) View.VISIBLE else View.GONE
	}

	fun prefillLicense(license: String) {
		binding.licenseContent.etLicense.setText(license)
		binding.licenseContent.licenseEntryGroup.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
	}
}
