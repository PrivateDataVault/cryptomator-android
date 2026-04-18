package org.cryptomator.presentation.ui.dialog

import android.content.DialogInterface
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogRestoreSuccessfulBinding

@Dialog
class RestoreSuccessfulDialog : BaseDialog<RestoreSuccessfulDialog.Callback, DialogRestoreSuccessfulBinding>(DialogRestoreSuccessfulBinding::inflate) {

	interface Callback {

		fun onRestoreSuccessfulDialogFinished()
	}

	public override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog {
		builder //
			.setTitle(R.string.dialog_restore_successful_title) //
			.setNeutralButton(getString(R.string.dialog_restore_successful_positive_button)) { _: DialogInterface, _: Int -> callback?.onRestoreSuccessfulDialogFinished() }
			.setOnKeyListener { _, keyCode, _ ->
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog?.dismiss()
					callback?.onRestoreSuccessfulDialogFinished()
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

		fun newInstance(): RestoreSuccessfulDialog {
			return RestoreSuccessfulDialog()
		}
	}
}
