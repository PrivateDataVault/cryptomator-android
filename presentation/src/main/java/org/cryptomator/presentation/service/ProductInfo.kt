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

data class ProductPrices(
	val subscriptionPrice: String?,
	val lifetimePrice: String?
)

fun List<ProductInfo>.resolveProductPrices(): ProductPrices {
	val subscription = find { it.productId == ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION }
	val lifetime = find { it.productId == ProductInfo.PRODUCT_FULL_VERSION }
	return ProductPrices(
		subscriptionPrice = subscription?.formattedPrice,
		lifetimePrice = lifetime?.formattedPrice
	)
}
