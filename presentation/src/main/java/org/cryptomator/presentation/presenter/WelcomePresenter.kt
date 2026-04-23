package org.cryptomator.presentation.presenter

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import org.cryptomator.domain.di.PerView
import org.cryptomator.domain.usecases.DoLicenseCheckUseCase
import org.cryptomator.domain.usecases.LicenseCheck
import org.cryptomator.domain.usecases.NoOpResultHandler
import org.cryptomator.generator.Callback
import org.cryptomator.presentation.R
import org.cryptomator.presentation.exception.ExceptionHandlers
import org.cryptomator.presentation.ui.activity.view.WelcomeView
import org.cryptomator.presentation.ui.dialog.AppIsObscuredInfoDialog
import org.cryptomator.presentation.workflow.PermissionsResult
import org.cryptomator.util.SharedPreferencesHandler
import timber.log.Timber
import javax.inject.Inject

@PerView
class WelcomePresenter @Inject internal constructor(
	exceptionHandlers: ExceptionHandlers,
	private val doLicenseCheckUseCase: DoLicenseCheckUseCase,
	private val sharedPreferencesHandler: SharedPreferencesHandler
) : Presenter<WelcomeView>(exceptionHandlers) {

	fun validate(data: Uri?) {
		data?.let {
			val license = it.fragment ?: it.lastPathSegment
			if (license.isNullOrEmpty()) return
			view?.showOrUpdateLicenseEntry(license)
			doLicenseCheckUseCase.withLicense(license).run(CheckLicenseStatusSubscriber())
		}
	}

	fun validateDialogAware(license: String?) {
		doLicenseCheckUseCase.withLicense(license).run(CheckLicenseStatusSubscriber())
	}

	fun onFilteredTouchEventForSecurity() {
		view?.showDialog(AppIsObscuredInfoDialog.newInstance())
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

	fun onSetScreenLock(setScreenLock: Boolean) {
		if (setScreenLock) {
			try {
				view?.activity()?.startActivity(Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD))
			} catch (e: ActivityNotFoundException) {
				Timber.tag("WelcomePresenter").d(e, "Device Policy Manager not found")
			}
		}
	}

	private inner class CheckLicenseStatusSubscriber : NoOpResultHandler<LicenseCheck>() {
		override fun onSuccess(licenseCheck: LicenseCheck) {
			super.onSuccess(licenseCheck)
			view?.closeDialog()
			sharedPreferencesHandler.setMail(licenseCheck.mail())
			view?.showConfirmationDialog(licenseCheck.mail())
		}

		override fun onError(t: Throwable) {
			super.onError(t)
			showError(t)
		}
	}

	init {
		unsubscribeOnDestroy(doLicenseCheckUseCase)
	}
}
