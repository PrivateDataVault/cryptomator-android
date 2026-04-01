package org.cryptomator.presentation.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import org.cryptomator.generator.Activity
import org.cryptomator.generator.InjectIntent
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityLicenseCheckBinding
import org.cryptomator.presentation.intent.Intents.vaultListIntent
import org.cryptomator.presentation.intent.LicenseCheckIntent
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.licensing.LicenseStateOrchestrator
import org.cryptomator.presentation.presenter.LicenseCheckPresenter
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.presentation.ui.dialog.LicenseConfirmationDialog
import org.cryptomator.presentation.ui.layout.LicenseContentViewBinder
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import org.cryptomator.util.FlavorConfig
import javax.inject.Inject

@Activity
class LicenseCheckActivity : BaseActivity<ActivityLicenseCheckBinding>(ActivityLicenseCheckBinding::inflate), //
	LicenseConfirmationDialog.Callback, //
	UpdateLicenseView {

	@Inject
	lateinit var licenseCheckPresenter: LicenseCheckPresenter

	@Inject
	lateinit var licenseEnforcer: LicenseEnforcer

	@InjectIntent
	lateinit var licenseCheckIntent: LicenseCheckIntent

	private var lockedAction: LicenseEnforcer.LockedAction? = null
	private val licenseContentViewBinder by lazy { LicenseContentViewBinder(binding.licenseContent, FlavorConfig.isFreemiumFlavor) }

	private val orchestrator by lazy {
		LicenseStateOrchestrator(
			sharedPreferencesHandler, licenseEnforcer, { this },
			target = object : LicenseStateOrchestrator.Target {
				override fun onPurchaseStateChanged(hasWriteAccess: Boolean, hasPaidLicense: Boolean) {
					licenseContentViewBinder.bindPurchaseState(hasWriteAccess, hasPaidLicense)
				}
				override fun onTrialStateChanged(active: Boolean, expired: Boolean, expirationText: String?) {
					licenseContentViewBinder.bindTrialState(active, expired, expirationText)
				}
			},
			priceLoader = { licenseContentViewBinder.loadAndBindPrices(application as CryptomatorApp) }
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		lockedAction = LicenseEnforcer.LockedAction.fromName(licenseCheckIntent.lockedAction())
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
		orchestrator.onResume()
	}

	override fun onPause() {
		super.onPause()
		orchestrator.onPause()
	}

	override fun setupView() {
		// handled in onCreate via setupUpsellView
	}

	private fun setupUpsellView() {
		setSupportActionBar(binding.mtToolbar.toolbar)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
		supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_clear)
		binding.mtToolbar.toolbar.setNavigationOnClickListener { finish() }

		val action = lockedAction
		if (action != null) {
			binding.licenseContent.tvInfoText.visibility = View.VISIBLE
			binding.licenseContent.tvInfoText.text = getString(action.headerMessageRes)
		} else {
			binding.licenseContent.tvInfoText.visibility = View.GONE
			binding.licenseContent.tvInfoText.text = null
		}

		if (FlavorConfig.isFreemiumFlavor) {
			setupIapView()
		} else {
			setupLicenseEntryView()
		}
	}

	private fun setupIapView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title_full_version)
		licenseContentViewBinder.bindInitialIapLayout()
		licenseContentViewBinder.bindLegalLinks()
		licenseContentViewBinder.bindPurchaseButtons(
			activity = this,
			app = application as CryptomatorApp,
			onTrialClicked = {
				licenseEnforcer.startTrial()
				orchestrator.updateState()
			}
		)
	}

	private fun setupLicenseEntryView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title)
		licenseContentViewBinder.bindInitialLicenseEntryLayout()
		binding.licenseContent.btnPurchase.visibility = View.VISIBLE
		binding.licenseContent.btnPurchase.text = getString(R.string.dialog_enter_license_ok_button)
		binding.licenseContent.btnPurchase.setOnClickListener { onLicenseSubmit() }
		binding.licenseContent.tvLicenseLink.setOnClickListener {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/android/")))
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		Activities.setIntent(this)
		lockedAction = LicenseEnforcer.LockedAction.fromName(licenseCheckIntent.lockedAction())
		setupUpsellView()
		validate(intent)
	}

	private fun validate(intent: Intent) {
		val data: Uri? = intent.data
		licenseCheckPresenter.validate(data)
	}

	override fun showOrUpdateLicenseEntry(license: String) {
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

}
