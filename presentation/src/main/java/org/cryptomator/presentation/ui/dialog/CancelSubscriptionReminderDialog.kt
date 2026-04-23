package org.cryptomator.presentation.ui.dialog

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogCancelSubscriptionReminderBinding
import org.cryptomator.presentation.service.ProductInfo

@Dialog
class CancelSubscriptionReminderDialog : BaseInformationalDialog<DialogCancelSubscriptionReminderBinding>(DialogCancelSubscriptionReminderBinding::inflate) {

	override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog = builder
		.setTitle(R.string.dialog_cancel_subscription_reminder_title)
		.setPositiveButton(getString(R.string.dialog_cancel_subscription_reminder_manage_button)) { _, _ ->
			val url = "https://play.google.com/store/account/subscriptions?sku=${ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION}&package=${requireContext().packageName}"
			requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
		}
		.setNegativeButton(getString(R.string.dialog_cancel_subscription_reminder_close_button), null)
		.dismissOnBackKey()
		.create()

	companion object {
		fun newInstance() = CancelSubscriptionReminderDialog()
	}
}
