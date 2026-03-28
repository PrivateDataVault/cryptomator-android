package org.cryptomator.presentation.licensing

import android.content.Context
import org.cryptomator.util.FlavorConfig
import org.cryptomator.util.SharedPreferencesHandler
import java.util.function.Consumer

class LicenseStateOrchestrator(
	private val sharedPreferencesHandler: SharedPreferencesHandler,
	private val licenseEnforcer: LicenseEnforcer,
	private val contextProvider: () -> Context,
	private val target: Target,
	private val priceLoader: (() -> Unit)? = null
) {

	interface Target {
		fun onPurchaseStateChanged(hasWriteAccess: Boolean, hasPaidLicense: Boolean)
		fun onTrialStateChanged(active: Boolean, expired: Boolean, expirationText: String?)
	}

	private val licenseChangeListener = Consumer<String> { _ -> updateState() }

	fun onResume() {
		sharedPreferencesHandler.addLicenseChangedListeners(licenseChangeListener)
		updateState()
		if (FlavorConfig.isFreemiumFlavor) {
			priceLoader?.invoke()
		}
	}

	fun onPause() {
		sharedPreferencesHandler.removeLicenseChangedListeners(licenseChangeListener)
	}

	fun updateState() {
		val uiState = licenseEnforcer.evaluateUiState(contextProvider())
		target.onPurchaseStateChanged(uiState.hasWriteAccess, uiState.hasPaidLicense)
		if (FlavorConfig.isFreemiumFlavor && !uiState.hasPaidLicense) {
			target.onTrialStateChanged(
				uiState.trialState.isActive, uiState.trialState.isExpired, uiState.trialExpirationText
			)
		}
	}
}
