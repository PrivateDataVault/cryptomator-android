package org.cryptomator.presentation.service

import android.app.Activity
import androidx.fragment.app.FragmentActivity
import org.cryptomator.util.NoOpActivityLifecycleCallbacks
import org.cryptomator.util.SharedPreferencesHandler
import timber.log.Timber

class RestoreOutcomeDialogObserver(
	private val sharedPreferencesHandler: SharedPreferencesHandler
) : NoOpActivityLifecycleCallbacks() {

	override fun onActivityResumed(activity: Activity) {
		val kindName = sharedPreferencesHandler.pendingRestoreOutcome()
		if (kindName.isEmpty()) {
			return
		}
		sharedPreferencesHandler.clearPendingRestoreOutcome()
		val kind = runCatching { RestoreOutcome.Kind.valueOf(kindName) }.getOrNull()
		if (kind == null) {
			Timber.tag("RestoreOutcomeDialogObserver").w("Invalid pending outcome %s; cleared", kindName)
			return
		}
		(activity as? FragmentActivity)?.let {
			kind.toDialogFragment().show(it.supportFragmentManager, "RestoreOutcome")
		}
	}
}
