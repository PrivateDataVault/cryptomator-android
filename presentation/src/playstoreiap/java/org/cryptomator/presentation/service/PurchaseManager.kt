package org.cryptomator.presentation.service

import com.android.billingclient.api.Purchase
import org.cryptomator.util.SharedPreferencesHandler
import timber.log.Timber

class PurchaseManager(
	private val sharedPreferencesHandler: SharedPreferencesHandler
) {

	fun handleInAppPurchases(purchases: List<Purchase>, clearIfNotFound: Boolean = false, acknowledgePurchase: (String) -> Unit) {
		for (purchase in purchases) {
			if (!purchase.products.contains(ProductInfo.PRODUCT_FULL_VERSION)) {
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
				Timber.tag("PurchaseManager").d("In-app purchase pending, skipping")
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				Timber.tag("PurchaseManager").d("In-app purchase found: %s", purchase.signature)
				if (sharedPreferencesHandler.licenseToken().isEmpty()) {
					sharedPreferencesHandler.setLicenseToken(purchase.purchaseToken)
				}
				if (!purchase.isAcknowledged) {
					acknowledgePurchase(purchase.purchaseToken)
				}
				return
			}
		}
		if (clearIfNotFound && sharedPreferencesHandler.licenseToken().isNotEmpty()) {
			Timber.tag("PurchaseManager").i("Remove license, purchase does not exist anymore")
			sharedPreferencesHandler.setLicenseToken("")
		}
	}

	fun handleSubscriptionPurchases(purchases: List<Purchase>, clearIfNotFound: Boolean = false, acknowledgePurchase: (String) -> Unit) {
		for (purchase in purchases) {
			if (!purchase.products.contains(ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION)) {
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
				Timber.tag("PurchaseManager").d("Subscription purchase pending, skipping")
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				Timber.tag("PurchaseManager").d("Subscription found: %s", purchase.signature)
				sharedPreferencesHandler.setHasRunningSubscription(true)
				if (!purchase.isAcknowledged) {
					acknowledgePurchase(purchase.purchaseToken)
				}
				return
			}
		}
		if (clearIfNotFound) {
			sharedPreferencesHandler.setHasRunningSubscription(false)
		}
	}
}
