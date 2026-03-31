package org.cryptomator.presentation.licensing

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import org.cryptomator.domain.di.PerView
import org.cryptomator.presentation.R
import org.cryptomator.presentation.model.VaultModel
import org.cryptomator.presentation.ui.activity.LicenseCheckActivity
import org.cryptomator.util.FlavorConfig
import org.cryptomator.util.SharedPreferencesHandler
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

@PerView
class LicenseEnforcer @Inject constructor(private val sharedPreferencesHandler: SharedPreferencesHandler) {

	enum class LockedAction(
		@StringRes val toastMessageRes: Int,
		@StringRes val headerMessageRes: Int
	) {
		CREATE_VAULT(
			R.string.read_only_reason_create_vault,
			R.string.screen_license_check_locked_create_vault,
		),
		UPLOAD_FILES(
			R.string.read_only_reason_add_file,
			R.string.screen_license_check_locked_upload_files,
		),
		CREATE_FOLDER(
			R.string.read_only_reason_create_folder,
			R.string.screen_license_check_locked_create_folder,
		),
		CREATE_TEXT_FILE(
			R.string.read_only_reason_create_text_file,
			R.string.screen_license_check_locked_create_text_file,
		),
		SHARE_NODE(
			R.string.read_only_reason_share_node,
			R.string.screen_license_check_locked_share_node,
		),
		RENAME_NODE(
			R.string.read_only_reason_rename_node,
			R.string.screen_license_check_locked_rename_node,
		),
		MOVE_NODE(
			R.string.read_only_reason_move_node,
			R.string.screen_license_check_locked_move_node,
		),
		DELETE_NODE(
			R.string.read_only_reason_delete_node,
			R.string.screen_license_check_locked_delete_node,
		);

		companion object {
			fun fromName(name: String?): LockedAction? {
				return values().firstOrNull { it.name == name }
			}
		}
	}

	fun hasWriteAccess(): Boolean {
		return hasPaidLicense() || hasActiveTrial()
	}

	fun hasPaidLicense(): Boolean {
		if (FlavorConfig.isPremiumFlavor) {
			return true
		}
		if (sharedPreferencesHandler.licenseToken().isNotEmpty()) {
			return true
		}
		if (sharedPreferencesHandler.hasRunningSubscription()) {
			return true
		}
		return false
	}

	fun startTrial() {
		if (sharedPreferencesHandler.trialExpirationDate() > 0) {
			return
		}
		val trialExpiration = System.currentTimeMillis() + TRIAL_DURATION_MS
		sharedPreferencesHandler.setTrialExpirationDate(trialExpiration)
	}

	fun hasActiveTrial(): Boolean {
		val trialExpiration = sharedPreferencesHandler.trialExpirationDate()
		return trialExpiration > 0 && trialExpiration > System.currentTimeMillis()
	}

	fun evaluateTrialState(): TrialState {
		val trialExpiration = sharedPreferencesHandler.trialExpirationDate()
		val now = System.currentTimeMillis()
		val active = trialExpiration > 0 && trialExpiration > now
		val expired = trialExpiration > 0 && trialExpiration <= now
		val formattedDate = if (active || expired) {
			DateFormat.getDateInstance().format(Date(trialExpiration))
		} else null
		return TrialState(active, expired, formattedDate)
	}

	data class TrialState(val isActive: Boolean, val isExpired: Boolean, val formattedExpirationDate: String?)

	data class LicenseUiState(
		val hasWriteAccess: Boolean,
		val hasPaidLicense: Boolean,
		val trialState: TrialState,
		val trialExpirationText: String?
	)

	fun evaluateUiState(context: Context): LicenseUiState {
		val trialState = evaluateTrialState()
		val expirationText = if (trialState.isActive || trialState.isExpired) {
			context.getString(R.string.screen_license_check_trial_expiration, trialState.formattedExpirationDate)
		} else null
		return LicenseUiState(
			hasWriteAccess = hasWriteAccess(),
			hasPaidLicense = hasPaidLicense(),
			trialState = trialState,
			trialExpirationText = expirationText
		)
	}

	@StringRes
	fun defaultReasonRes(): Int = R.string.read_only_banner

	fun ensureWriteAccess(activity: Activity, action: LockedAction): Boolean {
		if (hasWriteAccess()) {
			return true
		}

		Toast.makeText(activity, activity.getString(action.toastMessageRes), Toast.LENGTH_LONG).show()

		if (FlavorConfig.isPremiumFlavor) {
			return false
		}

		val intent = Intent(activity, LicenseCheckActivity::class.java).apply {
			flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
			data = Uri.parse("app://cryptomator/")
			putExtra(LicenseCheckActivity.EXTRA_LOCKED_ACTION, action.name)
		}
		activity.startActivity(intent)
		return false
	}

	fun hasWriteAccessForVault(vault: VaultModel?): Boolean {
		if (vault?.isHubVault == true) return vault.hasHubPaidLicense
		return hasWriteAccess()
	}

	fun ensureWriteAccessForVault(activity: Activity, vault: VaultModel?, action: LockedAction): Boolean {
		if (vault?.isHubVault == true) {
			if (hasWriteAccessForVault(vault)) return true
			Toast.makeText(activity, R.string.read_only_reason_hub_inactive, Toast.LENGTH_LONG).show()
			return false
		}
		return ensureWriteAccess(activity, action)
	}

	companion object {
		private const val TRIAL_DURATION_DAYS = 30L
		private const val TRIAL_DURATION_MS = TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000
	}
}
