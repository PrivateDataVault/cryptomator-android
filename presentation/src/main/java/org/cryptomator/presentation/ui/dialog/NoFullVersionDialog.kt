package org.cryptomator.presentation.ui.dialog

import android.content.DialogInterface
import android.view.KeyEvent
import androidx.appcompat.app.AlertDialog
import org.cryptomator.generator.Dialog
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.DialogNoFullVersionBinding

@Dialog
class NoFullVersionDialog : BaseDialog<NoFullVersionDialog.Callback, DialogNoFullVersionBinding>(DialogNoFullVersionBinding::inflate) {

	interface Callback {

		fun onNoFullVersionDialogFinished()
	}

	public override fun setupDialog(builder: AlertDialog.Builder): android.app.Dialog {
		builder //
			.setTitle(R.string.dialog_no_full_version_title) //
			.setNeutralButton(getString(R.string.dialog_no_full_version_positive_button)) { _: DialogInterface, _: Int -> callback?.onNoFullVersionDialogFinished() }
			.setOnKeyListener { _, keyCode, _ ->
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog?.dismiss()
					callback?.onNoFullVersionDialogFinished()
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

		fun newInstance(): NoFullVersionDialog {
			return NoFullVersionDialog()
		}
	}
}
