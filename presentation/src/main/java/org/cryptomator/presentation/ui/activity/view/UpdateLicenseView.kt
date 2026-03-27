package org.cryptomator.presentation.ui.activity.view

interface UpdateLicenseView : View {

	fun showOrUpdateLicenseEntry(license: String)
	fun showConfirmationDialog(mail: String)

}
