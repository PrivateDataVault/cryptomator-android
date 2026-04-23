package org.cryptomator.presentation.ui.activity

import android.content.Intent
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
import org.cryptomator.presentation.service.RestoreOutcome
import org.cryptomator.presentation.ui.activity.view.LicenseView
import org.cryptomator.presentation.ui.dialog.CancelSubscriptionReminderDialog
import org.cryptomator.presentation.ui.dialog.EnterLicenseDialog
import org.cryptomator.presentation.ui.dialog.LicenseConfirmationDialog
import org.cryptomator.presentation.ui.dialog.NoFullVersionDialog
import org.cryptomator.presentation.ui.dialog.RestoreFailedDialog
import org.cryptomator.presentation.ui.dialog.RestoreSuccessfulDialog
import org.cryptomator.presentation.ui.layout.LicenseContentViewBinder
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import org.cryptomator.util.FlavorConfig
import javax.inject.Inject

@Activity
class LicenseCheckActivity : BaseActivity<ActivityLicenseCheckBinding>(ActivityLicenseCheckBinding::inflate), //
	LicenseConfirmationDialog.Callback, //
	LicenseView, //
	EnterLicenseDialog.Callback {

	@Inject
	lateinit var licenseCheckPresenter: LicenseCheckPresenter

	@Inject
	lateinit var licenseEnforcer: LicenseEnforcer

	@InjectIntent
	lateinit var licenseCheckIntent: LicenseCheckIntent

	private var lockedAction: LicenseEnforcer.LockedAction? = null
	private var wasSubscriptionOnly = false
	private val licenseContentViewBinder by lazy { LicenseContentViewBinder(binding.licenseContent, FlavorConfig.isFreemiumFlavor) }

	private val orchestrator by lazy {
		wasSubscriptionOnly = sharedPreferencesHandler.hasRunningSubscription() && sharedPreferencesHandler.licenseToken().isEmpty()
		LicenseStateOrchestrator(
			sharedPreferencesHandler, licenseEnforcer, { this },
			onStateChanged = { uiState ->
				if (uiState.hasLifetimeLicense && uiState.hasRunningSubscription && wasSubscriptionOnly) {
					showDialog(CancelSubscriptionReminderDialog.newInstance())
				}
				val prevSubscriptionOnly = wasSubscriptionOnly
				wasSubscriptionOnly = uiState.hasRunningSubscription && !uiState.hasLifetimeLicense
				if (uiState.hasRunningSubscription && !uiState.hasLifetimeLicense && !prevSubscriptionOnly) {
					finish()
				} else {
					licenseContentViewBinder.bindPurchaseState(uiState.hasWriteAccess, uiState.hasPaidLicense, uiState.hasLifetimeLicense, uiState.hasRunningSubscription, hasLockedActionHeader = lockedAction != null)
					licenseContentViewBinder.bindTrialState(uiState.trialState.isActive, uiState.trialState.isExpired, uiState.trialExpirationText, hasLockedActionHeader = lockedAction != null, hasSubscriptionUpgradeHint = wasSubscriptionOnly)
				}
			},
			priceLoader = { licenseContentViewBinder.loadAndBindPrices(application as CryptomatorApp) }
		)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding.activityRootView.setOnFilteredTouchEventForSecurityListener(object : ObscuredAwareCoordinatorLayout.Listener {
			override fun onFilteredTouchEventForSecurity() {
				licenseCheckPresenter.onFilteredTouchEventForSecurity()
			}
		})
		validate(intent)
	}

	override fun onResume() {
		super.onResume()
		orchestrator.onResume()
		(application as CryptomatorApp).consumeLastRestoreOutcome()?.let { outcome ->
			when (outcome) {
				RestoreOutcome.RESTORED -> showDialog(RestoreSuccessfulDialog.newInstance())
				RestoreOutcome.NOTHING_TO_RESTORE -> showDialog(NoFullVersionDialog.newInstance())
				is RestoreOutcome.FAILED -> showDialog(RestoreFailedDialog.newInstance())
			}
		}
	}

	override fun onPause() {
		super.onPause()
		orchestrator.onPause()
	}

	override fun setupView() {
		lockedAction = LicenseEnforcer.LockedAction.fromName(licenseCheckIntent.lockedAction())
		setupUpsellView()
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

	private fun onTrialClicked() {
		licenseEnforcer.startTrial()
		orchestrator.updateState()
	}

	private fun setupIapView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title_full_version)
		licenseContentViewBinder.bindInitialIapLayout()
		licenseContentViewBinder.bindLegalLinks()
		licenseContentViewBinder.bindPurchaseButtons(
			activity = this,
			app = application as CryptomatorApp,
			onTrialClicked = ::onTrialClicked
		)
	}

	private fun setupLicenseEntryView() {
		supportActionBar?.title = getString(R.string.screen_license_check_title)
		licenseContentViewBinder.bindInitialLicenseEntryWithTrialLayout()
		binding.licenseContent.btnTrial.setOnClickListener { onTrialClicked() }
		licenseContentViewBinder.bindEnterLicenseButton {
			showDialog(EnterLicenseDialog.newInstance())
		}
	}

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		setIntent(intent)
		Activities.setIntent(this)
		lockedAction = LicenseEnforcer.LockedAction.fromName(licenseCheckIntent.lockedAction())
		setupUpsellView()
		orchestrator.updateState()
		validate(intent)
	}

	private fun validate(intent: Intent) {
		licenseCheckPresenter.validate(intent.data)
	}

	override fun onLicenseEntered(license: String) {
		licenseCheckPresenter.validateDialogAware(license)
	}

	override fun showConfirmationDialog(mail: String) {
		showDialog(LicenseConfirmationDialog.newInstance(mail))
	}

	override fun licenseConfirmationClicked() {
		vaultListIntent() //
			.preventGoingBackInHistory() //
			.startActivity(this) //
	}
}
