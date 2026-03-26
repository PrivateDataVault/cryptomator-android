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
import org.cryptomator.presentation.ui.layout.LicenseContentViewBinder

@Fragment
class WelcomeLicenseFragment : BaseFragment<FragmentWelcomeLicenseBinding>(FragmentWelcomeLicenseBinding::inflate) {

	interface Listener {
		fun onLicenseTextChanged(license: String?)
		fun onOpenLicenseLink()
		fun onPurchaseClick()
		fun onStartTrial()
		fun onPurchaseSubscription()
		fun onPurchaseLifetime()
		fun onRestorePurchases()
		fun onSkipLicense()
	}

	private val isIapFlavor = BuildConfig.FLAVOR == "playstoreiap"
	private val licenseContentViewBinder by lazy { LicenseContentViewBinder(binding.licenseContent, isIapFlavor) }
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
		if (isIapFlavor) {
			setupIapUi()
		} else {
			setupLicenseEntryUi()
		}
	}

	private fun setupIapUi() {
		licenseContentViewBinder.bindInitialIapLayout()
		licenseContentViewBinder.bindLegalLinks()

		binding.licenseContent.btnTrial.setOnClickListener { listener?.onStartTrial() }
		binding.licenseContent.btnSubscription.setOnClickListener { listener?.onPurchaseSubscription() }
		binding.licenseContent.btnLifetime.setOnClickListener { listener?.onPurchaseLifetime() }
		binding.licenseContent.tvRestorePurchase.setOnClickListener { listener?.onRestorePurchases() }
	}

	private fun setupLicenseEntryUi() {
		binding.licenseContent.licenseEntryGroup.visibility = View.VISIBLE
		binding.licenseContent.tvLicenseLink.visibility = View.VISIBLE
		binding.licenseContent.tvLicenseLink.text = getString(R.string.dialog_enter_license_content)
		binding.licenseContent.tvLicenseLink.setOnClickListener { listener?.onOpenLicenseLink() }
		binding.licenseContent.purchaseOptionsGroup.visibility = View.GONE
		binding.licenseContent.tvRestorePurchase.visibility = View.GONE
		binding.licenseContent.legalLinksGroup.visibility = View.GONE
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

	fun updateUnlocked(unlocked: Boolean, hasPaidLicense: Boolean) {
		if (!isAdded) return
		licenseContentViewBinder.bindPurchaseState(unlocked, hasPaidLicense)
	}

	fun updateTrialState(active: Boolean, expired: Boolean, expirationText: String?) {
		if (!isAdded) return
		licenseContentViewBinder.bindTrialState(active, expired, expirationText)
	}

	fun updateProductPrices(subscriptionPrice: String, lifetimePrice: String) {
		if (!isAdded) return
		licenseContentViewBinder.bindProductPrices(subscriptionPrice, lifetimePrice)
	}

	fun prefillLicense(license: String) {
		if (!isAdded) return
		binding.licenseContent.etLicense.setText(license)
		binding.licenseContent.licenseEntryGroup.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
	}

	companion object {
		private const val DEBOUNCE_DELAY_MS = 600L
	}
}
