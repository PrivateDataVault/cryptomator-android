package org.cryptomator.presentation.service

import com.android.billingclient.api.Purchase
import org.cryptomator.util.SharedPreferencesHandler
import timber.log.Timber

class PurchaseManager(
	private val sharedPreferencesHandler: SharedPreferencesHandler
) {

	fun handleInAppPurchases(purchases: List<Purchase>, clearIfNotFound: Boolean = false, acknowledgePurchase: (String) -> Unit): PurchaseFieldChange {
		val tokenBefore = sharedPreferencesHandler.licenseToken()
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
				if (tokenBefore.isEmpty()) {
					sharedPreferencesHandler.setLicenseToken(purchase.purchaseToken)
				}
				if (!purchase.isAcknowledged) {
					acknowledgePurchase(purchase.purchaseToken)
				}
				return PurchaseFieldChange(cleared = false)
			}
		}
		if (clearIfNotFound && tokenBefore.isNotEmpty()) {
			Timber.tag("PurchaseManager").i("Remove license, purchase does not exist anymore")
			sharedPreferencesHandler.setLicenseToken("")
			return PurchaseFieldChange(cleared = true)
		}
		return PurchaseFieldChange(cleared = false)
	}

	fun handleSubscriptionPurchases(purchases: List<Purchase>, clearIfNotFound: Boolean = false, acknowledgePurchase: (String) -> Unit): PurchaseFieldChange {
		val hadSubscription = sharedPreferencesHandler.hasRunningSubscription()
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
				return PurchaseFieldChange(cleared = false)
			}
		}
		if (clearIfNotFound) {
			sharedPreferencesHandler.setHasRunningSubscription(false)
			return PurchaseFieldChange(cleared = hadSubscription)
		}
		return PurchaseFieldChange(cleared = false)
	}
}
