package org.cryptomator.presentation.service

sealed interface RestoreOutcome {
	data object RESTORED : RestoreOutcome
	data object NOTHING_TO_RESTORE : RestoreOutcome
	data class FAILED(val cause: Throwable? = null) : RestoreOutcome
}
