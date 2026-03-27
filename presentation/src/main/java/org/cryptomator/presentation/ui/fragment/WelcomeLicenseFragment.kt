package org.cryptomator.presentation.ui.fragment

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.FragmentWelcomeLicenseBinding
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.ui.layout.LicenseContentViewBinder

@Fragment
class WelcomeLicenseFragment : BaseFragment<FragmentWelcomeLicenseBinding>(FragmentWelcomeLicenseBinding::inflate) {

	interface Listener {
		fun onLicenseTextChanged(license: String?)
		fun onOpenLicenseLink()
		fun onStartTrial()
		fun onSkipLicense()
	}

	private val isIapFlavor: Boolean
		get() = LicenseEnforcer.isIapFlavor
	private val licenseContentViewBinder by lazy { LicenseContentViewBinder(binding.licenseContent, isIapFlavor) }
	private var listener: Listener? = null
	private val debounceHandler = Handler(Looper.getMainLooper())
	private var debounceRunnable: Runnable? = null

	override fun onAttach(context: Context) {
		super.onAttach(context)
		listener = context as? Listener
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
		licenseContentViewBinder.bindPurchaseButtons(
			activity = requireActivity(),
			app = requireActivity().application as CryptomatorApp,
			onTrialClicked = { listener?.onStartTrial() }
		)
	}

	private fun setupLicenseEntryUi() {
		licenseContentViewBinder.bindInitialLicenseEntryLayout()
		binding.licenseContent.tvLicenseLink.setOnClickListener { listener?.onOpenLicenseLink() }
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

	fun loadAndBindPrices(app: CryptomatorApp) {
		if (!isAdded) return
		licenseContentViewBinder.loadAndBindPrices(app)
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
