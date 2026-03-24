package org.cryptomator.presentation.ui.fragment

import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.FragmentWelcomeLicenseBinding

@Fragment
class WelcomeLicenseFragment : BaseFragment<FragmentWelcomeLicenseBinding>(FragmentWelcomeLicenseBinding::inflate) {

	interface Listener {
		fun onLicenseTextChanged(license: String?)
		fun onOpenLicenseLink()
		fun onPurchaseClick()
		fun onSkipLicense()
	}

	private val isIapFlavor = BuildConfig.FLAVOR == "playstoreiap"
	private var listener: Listener? = null
	private val debounceHandler = Handler(Looper.getMainLooper())
	private var debounceRunnable: Runnable? = null

	fun setListener(listener: Listener) {
		this.listener = listener
	}

	override fun setupView() {
		setupUi()
	}

	override fun onDestroyView() {
		debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
		super.onDestroyView()
	}

	private fun setupUi() {
		binding.licenseContent.licenseEntryGroup.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.tvLicenseLink.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.tvLicenseLink.text = getString(R.string.dialog_enter_license_content)
		binding.licenseContent.tvLicenseLink.setOnClickListener { listener?.onOpenLicenseLink() }
		if (isIapFlavor) {
			binding.licenseContent.btnPurchase.text = getString(R.string.screen_license_check_button_purchase)
			binding.licenseContent.btnPurchase.visibility = View.VISIBLE
			binding.licenseContent.btnPurchase.setOnClickListener { listener?.onPurchaseClick() }
		} else {
			binding.licenseContent.btnPurchase.visibility = View.GONE
			binding.licenseContent.etLicense.addTextChangedListener(object : TextWatcher {
				override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
				override fun afterTextChanged(s: Editable?) {
					debounceRunnable?.let { debounceHandler.removeCallbacks(it) }
					val text = s?.toString()
					if (!text.isNullOrBlank()) {
						val runnable = Runnable { listener?.onLicenseTextChanged(text) }
						debounceRunnable = runnable
						debounceHandler.postDelayed(runnable, DEBOUNCE_DELAY_MS)
					}
				}
			})
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

	companion object {
		private const val DEBOUNCE_DELAY_MS = 600L
	}
}
