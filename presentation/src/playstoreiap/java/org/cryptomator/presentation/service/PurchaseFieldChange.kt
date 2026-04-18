package org.cryptomator.presentation.service

data class PurchaseFieldChange(
	val before: Boolean,
	val after: Boolean,
	val cleared: Boolean
)
