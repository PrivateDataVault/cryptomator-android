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
		fun onStateChanged(uiState: LicenseEnforcer.LicenseUiState)
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
		target.onStateChanged(licenseEnforcer.evaluateUiState(contextProvider()))
	}
}
