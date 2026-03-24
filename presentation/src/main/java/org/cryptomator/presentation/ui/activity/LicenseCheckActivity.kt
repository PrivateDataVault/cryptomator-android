package org.cryptomator.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityLicenseCheckBinding
import org.cryptomator.presentation.intent.Intents.vaultListIntent
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.presenter.LicenseCheckPresenter
import org.cryptomator.presentation.service.ProductInfo
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.presentation.ui.dialog.LicenseConfirmationDialog
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import java.lang.ref.WeakReference
import java.text.DateFormat
import java.util.Date
import java.util.function.Consumer
import javax.inject.Inject

@Activity
class LicenseCheckActivity : BaseActivity<ActivityLicenseCheckBinding>(ActivityLicenseCheckBinding::inflate), //
	LicenseConfirmationDialog.Callback, //
	UpdateLicenseView {

	@Inject
	lateinit var licenseCheckPresenter: LicenseCheckPresenter

	@Inject
	lateinit var licenseEnforcer: LicenseEnforcer

	private var exitOnCancel = true
	private var lockedAction: LicenseEnforcer.LockedAction? = null
	private val isIapFlavor = BuildConfig.FLAVOR == "playstoreiap"

	private val licenseChangeListener = Consumer<String> { _ -> updatePurchaseState() }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		exitOnCancel = intent.getBooleanExtra(EXTRA_EXIT_ON_CANCEL, true)
		lockedAction = LicenseEnforcer.LockedAction.fromName(intent.getStringExtra(EXTRA_LOCKED_ACTION))
		binding.activityRootView.setOnFilteredTouchEventForSecurityListener(object : ObscuredAwareCoordinatorLayout.Listener {
			override fun onFilteredTouchEventForSecurity() {
				licenseCheckPresenter.onFilteredTouchEventForSecurity()
			}
		})
		setupUpsellView()
		validate(intent)
	}

	override fun onResume() {
		super.onResume()
		sharedPreferencesHandler.addLicenseChangedListeners(licenseChangeListener)
		updatePurchaseState()
		if (isIapFlavor) {
			loadProductPrices()
		}
	}

	override fun onPause() {
		super.onPause()
		sharedPreferencesHandler.removeLicenseChangedListeners(licenseChangeListener)
	}

	override fun setupView() {
		// handled in onCreate via setupUpsellView
	}

	private fun setupUpsellView() {
		setSupportActionBar(binding.mtToolbar.toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear)
		binding.mtToolbar.toolbar.setNavigationOnClickListener { finish() }

		if (isIapFlavor) {
			setupIapView()
		} else {
			setupLicenseEntryView()
		}
	}

	private fun setupIapView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title_full_version)
		binding.licenseContent.licenseEntryGroup.visibility = View.GONE
		binding.licenseContent.btnPurchase.visibility = View.GONE
		binding.licenseContent.purchaseOptionsGroup.visibility = View.VISIBLE
		binding.licenseContent.tvRestorePurchase.visibility = View.VISIBLE
		binding.licenseContent.legalLinksGroup.visibility = View.VISIBLE

		binding.licenseContent.btnTrial.setOnClickListener {
			licenseEnforcer.startTrial()
			updatePurchaseState()
		}
		binding.licenseContent.btnSubscription.isEnabled = false
		binding.licenseContent.btnSubscription.setOnClickListener {
			(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this), ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION)
		}
		binding.licenseContent.btnLifetime.isEnabled = false
		binding.licenseContent.btnLifetime.setOnClickListener {
			(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this), ProductInfo.PRODUCT_FULL_VERSION)
		}
		binding.licenseContent.tvRestorePurchase.setOnClickListener {
			(application as CryptomatorApp).restorePurchases()
			Toast.makeText(this, getString(R.string.screen_license_check_restore_purchase), Toast.LENGTH_SHORT).show()
		}
		binding.licenseContent.tvTerms.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/terms/")))
		}
		binding.licenseContent.tvPrivacy.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/privacy/")))
		}
	}

	private fun setupLicenseEntryView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title)
		binding.licenseContent.licenseEntryGroup.visibility = View.VISIBLE
		binding.licenseContent.purchaseOptionsGroup.visibility = View.GONE
		binding.licenseContent.tvRestorePurchase.visibility = View.GONE
		binding.licenseContent.legalLinksGroup.visibility = View.GONE
		binding.licenseContent.btnPurchase.visibility = View.VISIBLE
		binding.licenseContent.btnPurchase.text = getString(R.string.dialog_enter_license_ok_button)
		binding.licenseContent.btnPurchase.setOnClickListener { onLicenseSubmit() }
		binding.licenseContent.tvLicenseLink.text = getString(R.string.dialog_enter_license_content)
		binding.licenseContent.tvLicenseLink.visibility = View.VISIBLE
		binding.licenseContent.tvLicenseLink.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/android/")))
		}
	}

	private fun loadProductPrices() {
		(application as CryptomatorApp).queryProductDetails { products ->
			runOnUiThread {
				val subscription = products.find { it.productId == ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION }
				val lifetime = products.find { it.productId == ProductInfo.PRODUCT_FULL_VERSION }
				if (subscription != null) {
					binding.licenseContent.btnSubscription.text = subscription.formattedPrice
					binding.licenseContent.btnSubscription.isEnabled = true
				}
				if (lifetime != null) {
					binding.licenseContent.btnLifetime.text = lifetime.formattedPrice
					binding.licenseContent.btnLifetime.isEnabled = true
				}
			}
		}
	}

	private fun updatePurchaseState() {
		val unlocked = licenseEnforcer.hasWriteAccess()
		if (isIapFlavor) {
			val hasPaidLicense = licenseEnforcer.hasPaidLicense()
			binding.licenseContent.tvUnlocked.visibility = if (unlocked) View.VISIBLE else View.GONE
			binding.licenseContent.purchaseOptionsGroup.visibility = if (hasPaidLicense) View.GONE else View.VISIBLE
			binding.licenseContent.tvRestorePurchase.visibility = if (hasPaidLicense) View.GONE else View.VISIBLE
			updateTrialState()
		} else {
			binding.licenseContent.btnPurchase.isEnabled = !unlocked
			binding.licenseContent.tvUnlocked.visibility = if (unlocked) View.VISIBLE else View.GONE
		}
	}

	private fun updateTrialState() {
		if (licenseEnforcer.hasActiveTrial()) {
			binding.licenseContent.btnTrial.isEnabled = false
			binding.licenseContent.tvTrialStatus.visibility = View.VISIBLE
			val expirationDate = DateFormat.getDateInstance().format(Date(sharedPreferencesHandler.trialExpirationDate()))
			binding.licenseContent.tvTrialStatus.text = getString(R.string.screen_license_check_trial_active, expirationDate)
		} else if (licenseEnforcer.hasExpiredTrial()) {
			binding.licenseContent.btnTrial.isEnabled = false
			binding.licenseContent.tvTrialStatus.visibility = View.VISIBLE
			binding.licenseContent.tvTrialStatus.text = getString(R.string.screen_license_check_trial_expired)
		} else {
			binding.licenseContent.btnTrial.isEnabled = true
			binding.licenseContent.tvTrialStatus.visibility = View.GONE
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		exitOnCancel = intent.getBooleanExtra(EXTRA_EXIT_ON_CANCEL, true)
		lockedAction = LicenseEnforcer.LockedAction.fromName(intent.getStringExtra(EXTRA_LOCKED_ACTION)) ?: lockedAction
		setupUpsellView()
		validate(intent)
	}

	private fun validate(intent: Intent) {
		val data: Uri? = intent.data
		licenseCheckPresenter.validate(data)
	}

	override fun showOrUpdateLicenseDialog(license: String) {
		binding.licenseContent.etLicense.setText(license)
		binding.licenseContent.licenseEntryGroup.visibility = View.VISIBLE
	}

	override fun showConfirmationDialog(mail: String) {
		showDialog(LicenseConfirmationDialog.newInstance(mail))
	}

	override fun licenseConfirmationClicked() {
		vaultListIntent() //
			.preventGoingBackInHistory() //
			.startActivity(this) //
	}

	private fun onLicenseSubmit() {
		licenseCheckPresenter.validateDialogAware(binding.licenseContent.etLicense.text?.toString())
	}

	companion object {
		const val EXTRA_EXIT_ON_CANCEL = "exitOnCancel"
		const val EXTRA_LOCKED_ACTION = "lockedAction"
	}
}
