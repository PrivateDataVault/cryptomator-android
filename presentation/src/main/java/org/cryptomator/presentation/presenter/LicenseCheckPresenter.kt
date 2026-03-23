package org.cryptomator.presentation.presenter

import org.cryptomator.domain.usecases.DoLicenseCheckUseCase
import org.cryptomator.presentation.exception.ExceptionHandlers
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.util.SharedPreferencesHandler
import javax.inject.Inject

class LicenseCheckPresenter @Inject internal constructor(
	exceptionHandlers: ExceptionHandlers,
	doLicenseCheckUseCase: DoLicenseCheckUseCase,
	sharedPreferencesHandler: SharedPreferencesHandler
) : BaseLicensePresenter<UpdateLicenseView>(exceptionHandlers, doLicenseCheckUseCase, sharedPreferencesHandler)
