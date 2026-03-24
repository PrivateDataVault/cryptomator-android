package org.cryptomator.presentation.licensing

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import org.cryptomator.domain.di.PerView
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.R
import org.cryptomator.presentation.ui.activity.LicenseCheckActivity
import org.cryptomator.util.SharedPreferencesHandler
import javax.inject.Inject

@PerView
class LicenseEnforcer @Inject constructor(private val sharedPreferencesHandler: SharedPreferencesHandler) {

	enum class LockedAction(
		@StringRes val toastMessageRes: Int
	) {
		CREATE_VAULT(
			R.string.read_only_reason_create_vault,
		),
		UPLOAD_FILES(
			R.string.read_only_reason_add_file,
		),
		CREATE_FOLDER(
			R.string.read_only_reason_create_folder,
		),
		CREATE_TEXT_FILE(
			R.string.read_only_reason_create_text_file,
		),
		SHARE_NODE(
			R.string.read_only_reason_share_node,
		);

		companion object {
			fun fromName(name: String?): LockedAction? {
				return values().firstOrNull { it.name == name }
			}
		}
	}

	fun hasWriteAccess(): Boolean {
		if (BuildConfig.FLAVOR == "playstore" || BuildConfig.FLAVOR == "accrescent") {
			return true
		}
		if (sharedPreferencesHandler.licenseToken().isNotEmpty()) {
			return true
		}
		if (sharedPreferencesHandler.hasRunningSubscription()) {
			return true
		}
		val trialExpiration = sharedPreferencesHandler.trialExpirationDate()
		if (trialExpiration > 0 && trialExpiration > System.currentTimeMillis()) {
			return true
		}
		return false
	}

	fun startTrial() {
		val trialExpiration = System.currentTimeMillis() + TRIAL_DURATION_MS
		sharedPreferencesHandler.setTrialExpirationDate(trialExpiration)
	}

	fun hasActiveTrial(): Boolean {
		val trialExpiration = sharedPreferencesHandler.trialExpirationDate()
		return trialExpiration > 0 && trialExpiration > System.currentTimeMillis()
	}

	fun hasExpiredTrial(): Boolean {
		val trialExpiration = sharedPreferencesHandler.trialExpirationDate()
		return trialExpiration > 0 && trialExpiration <= System.currentTimeMillis()
	}

	@StringRes
	fun defaultReasonRes(): Int = R.string.read_only_banner

	fun ensureWriteAccess(activity: Activity, action: LockedAction): Boolean {
		if (hasWriteAccess()) {
			return true
		}

		Toast.makeText(activity, activity.getString(action.toastMessageRes), Toast.LENGTH_LONG).show()

		if (BuildConfig.FLAVOR == "playstore" || BuildConfig.FLAVOR == "accrescent") {
			return false
		}

		val intent = Intent(activity, LicenseCheckActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK
			data = Uri.parse("app://cryptomator/")
			putExtra(LicenseCheckActivity.EXTRA_EXIT_ON_CANCEL, false)
			putExtra(LicenseCheckActivity.EXTRA_LOCKED_ACTION, action.name)
		}
		activity.startActivity(intent)
		return false
	}

	companion object {
		private const val TRIAL_DURATION_MS = 30L * 24 * 60 * 60 * 1000
	}
}
