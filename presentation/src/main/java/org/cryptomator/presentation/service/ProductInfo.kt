package org.cryptomator.presentation.service

data class ProductInfo(
	val productId: String,
	val formattedPrice: String
) {
	companion object {
		const val PRODUCT_FULL_VERSION = "full_version"
		const val PRODUCT_YEARLY_SUBSCRIPTION = "yearly_subscription"
	}
}
