package org.cryptomator.presentation.presenter

import android.Manifest
import android.os.Build
import org.cryptomator.domain.di.PerView
import org.cryptomator.domain.usecases.DoLicenseCheckUseCase
import org.cryptomator.generator.Callback
import org.cryptomator.presentation.R
import org.cryptomator.presentation.exception.ExceptionHandlers
import org.cryptomator.presentation.ui.activity.view.WelcomeView
import org.cryptomator.presentation.workflow.PermissionsResult
import org.cryptomator.util.SharedPreferencesHandler
import javax.inject.Inject
import timber.log.Timber

@PerView
class WelcomePresenter @Inject internal constructor(
	exceptionHandlers: ExceptionHandlers,
	doLicenseCheckUseCase: DoLicenseCheckUseCase,
	sharedPreferencesHandler: SharedPreferencesHandler
) : BaseLicensePresenter<WelcomeView>(exceptionHandlers, doLicenseCheckUseCase, sharedPreferencesHandler) {

	private val onLicenseStateChanged: (String) -> Unit = this::onLicenseChanged

	override fun resumed() {
		super.resumed()
		sharedPreferencesHandler.addLicenseChangedListeners(onLicenseStateChanged);
	}

	override fun destroyed() {
		super.destroyed()
		sharedPreferencesHandler.removeLicenseChangedListeners(onLicenseStateChanged)
	}

	fun requestNotificationPermission() {
		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
			view?.onNotificationPermissionResult(true)
			return
		}
		requestPermissions(
			PermissionsResultCallbacks.requestWelcomeNotificationPermission(),
			R.string.permission_snackbar_notifications,
			Manifest.permission.POST_NOTIFICATIONS
		)
	}

	@Callback
	fun requestWelcomeNotificationPermission(result: PermissionsResult) {
		if (!result.granted()) {
			Timber.tag("WelcomePresenter").e("Notification permission not granted, notifications will not show")
		}
		view?.onNotificationPermissionResult(result.granted())
	}

	private fun onLicenseChanged(license: String) {
		view?.onLicenseStateChanged()
	}

}
