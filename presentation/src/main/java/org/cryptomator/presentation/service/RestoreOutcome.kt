package org.cryptomator.presentation.service

import androidx.fragment.app.DialogFragment
import org.cryptomator.presentation.ui.dialog.NoFullVersionDialog
import org.cryptomator.presentation.ui.dialog.RestoreFailedDialog
import org.cryptomator.presentation.ui.dialog.RestoreSuccessfulDialog

sealed interface RestoreOutcome {
	data object RESTORED : RestoreOutcome
	data object NOTHING_TO_RESTORE : RestoreOutcome
	data class FAILED(val cause: Throwable? = null) : RestoreOutcome

	/** Stable identifier for persisting a pending outcome via SharedPreferences (see [RestoreOutcomeDialogObserver]). */
	val kind: Kind
		get() = when (this) {
			RESTORED -> Kind.RESTORED
			NOTHING_TO_RESTORE -> Kind.NOTHING_TO_RESTORE
			is FAILED -> Kind.FAILED
		}

	enum class Kind { RESTORED, NOTHING_TO_RESTORE, FAILED }
}

fun RestoreOutcome.Kind.toDialogFragment(): DialogFragment = when (this) {
	RestoreOutcome.Kind.RESTORED -> RestoreSuccessfulDialog()
	RestoreOutcome.Kind.NOTHING_TO_RESTORE -> NoFullVersionDialog()
	RestoreOutcome.Kind.FAILED -> RestoreFailedDialog()
}

fun RestoreOutcome.toDialogFragment(): DialogFragment = kind.toDialogFragment()
