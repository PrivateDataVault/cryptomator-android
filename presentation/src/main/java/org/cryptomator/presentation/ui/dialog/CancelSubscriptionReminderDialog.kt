package org.cryptomator.presentation.ui.dialog

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogCancelSubscriptionReminderBinding
import org.cryptomator.presentation.service.ProductInfo

@Dialog
class CancelSubscriptionReminderDialog : BaseDialog<CancelSubscriptionReminderDialog.Callback, DialogCancelSubscriptionReminderBinding>(DialogCancelSubscriptionReminderBinding::inflate) {

	interface Callback {

		fun onCancelSubscriptionReminderDismissed()
	}

	public override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog {
		builder //
			.setTitle(R.string.dialog_cancel_subscription_reminder_title) //
			.setPositiveButton(getString(R.string.dialog_cancel_subscription_reminder_manage_button)) { _: DialogInterface, _: Int ->
				val url = "https://play.google.com/store/account/subscriptions?sku=${ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION}&package=${requireContext().packageName}"
				requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
				callback?.onCancelSubscriptionReminderDismissed()
			}
			.setNegativeButton(getString(R.string.dialog_cancel_subscription_reminder_close_button)) { _: DialogInterface, _: Int -> callback?.onCancelSubscriptionReminderDismissed() }
			.setOnKeyListener { _, keyCode, _ ->
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog?.dismiss()
					callback?.onCancelSubscriptionReminderDismissed()
					true
				} else {
					false
				}
			}
		return builder.create()
	}

	public override fun setupView() {
		super.onStart()
		val dialog = dialog as AlertDialog?
		dialog?.setCanceledOnTouchOutside(false)
	}

	companion object {

		fun newInstance(): CancelSubscriptionReminderDialog {
			return CancelSubscriptionReminderDialog()
		}
	}
}
