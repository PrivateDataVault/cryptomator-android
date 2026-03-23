package org.cryptomator.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityLicenseCheckBinding
import org.cryptomator.presentation.intent.Intents.vaultListIntent
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.presenter.LicenseCheckPresenter
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.presentation.ui.dialog.LicenseConfirmationDialog
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import java.lang.ref.WeakReference
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
		updatePurchaseState()
	}

	override fun setupView() {
		// handled in onCreate via setupUpsellView
	}

	private fun setupUpsellView() {
		setSupportActionBar(binding.mtToolbar.toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear)
		binding.mtToolbar.toolbar.setNavigationOnClickListener { finish() }
		supportActionBar?.title = getString(R.string.screen_license_check_title)
		binding.licenseContent.btnPurchase.text = if (isIapFlavor) {
			getString(R.string.screen_license_check_button_purchase)
		} else {
			getString(R.string.dialog_enter_license_ok_button)
		}
		binding.licenseContent.tvLicenseLink.text = getString(R.string.dialog_enter_license_ok_button)
		binding.licenseContent.licenseEntryGroup.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.btnPurchase.setOnClickListener {
			if (isIapFlavor) {
				(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this))
			} else {
				onLicenseSubmit()
			}
		}
		binding.licenseContent.tvLicenseLink.visibility = if (isIapFlavor) View.GONE else View.VISIBLE
		binding.licenseContent.tvLicenseLink.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/android/")))
		}
	}

	private fun updatePurchaseState() {
		val unlocked = licenseEnforcer.hasWriteAccess()
		binding.licenseContent.btnPurchase.isEnabled = !unlocked
		binding.licenseContent.tvUnlocked.visibility = if (unlocked) View.VISIBLE else View.GONE
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
