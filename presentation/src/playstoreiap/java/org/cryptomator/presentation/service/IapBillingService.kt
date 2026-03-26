package org.cryptomator.presentation.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.AcknowledgePurchaseResponseListener
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import org.cryptomator.util.SharedPreferencesHandler
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import timber.log.Timber

class IapBillingService : Service(), PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

	private val fullVersionProductId = ProductInfo.PRODUCT_FULL_VERSION
	private val yearlySubscriptionProductId = ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION

	private lateinit var billingClient: BillingClient
	private lateinit var sharedPreferencesHandler: SharedPreferencesHandler

	private val productDetailsMap = ConcurrentHashMap<String, ProductDetails>()
	private val pendingProductDetailsCallbacks = mutableListOf<(List<ProductInfo>) -> Unit>()

	private fun initBillingClient(context: Context) {
		this.sharedPreferencesHandler = SharedPreferencesHandler(context)
		val pendingPurchasesParams = PendingPurchasesParams.newBuilder()
			.enableOneTimeProducts()
			.enablePrepaidPlans()
			.build()
		billingClient = BillingClient.newBuilder(context)
			.setListener(this)
			.enablePendingPurchases(pendingPurchasesParams)
			.enableAutoServiceReconnection()
			.build()
		billingClient.startConnection(object : BillingClientStateListener {
			override fun onBillingSetupFinished(billingResult: BillingResult) {
				if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
					Timber.tag("IapBillingService").d("Billing setup successful")
					queryExistingPurchases()
					if (pendingProductDetailsCallbacks.isNotEmpty()) {
						val callbacks = ArrayList(pendingProductDetailsCallbacks)
						pendingProductDetailsCallbacks.clear()
						queryProductDetails { products ->
							callbacks.forEach { it(products) }
						}
					}
				} else {
					Timber.tag("IapBillingService").e("Billing setup not successful, error: %d", billingResult.responseCode)
				}
			}

			override fun onBillingServiceDisconnected() {
				Timber.tag("IapBillingService").i("Billing service disconnected")
			}
		})
	}

	override fun onCreate() {
		super.onCreate()
		Timber.tag("IapBillingService").d("Service created")
	}

	fun queryExistingPurchases() {
		val inappParams = QueryPurchasesParams.newBuilder()
			.setProductType(BillingClient.ProductType.INAPP)
			.build()
		billingClient.queryPurchasesAsync(inappParams) { billingResult: BillingResult, purchases: List<Purchase> ->
			if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
				handleInAppPurchases(purchases)
			}
		}
		val subsParams = QueryPurchasesParams.newBuilder()
			.setProductType(BillingClient.ProductType.SUBS)
			.build()
		billingClient.queryPurchasesAsync(subsParams) { billingResult: BillingResult, purchases: List<Purchase> ->
			if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
				handleSubscriptionPurchases(purchases)
			}
		}
	}

	private fun handleInAppPurchases(purchases: List<Purchase>) {
		for (purchase in purchases) {
			if (!purchase.products.contains(fullVersionProductId)) {
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				Timber.tag("IapBillingService").i("In-app purchase found: %s", purchase.signature)
				if (sharedPreferencesHandler.licenseToken().isEmpty()) {
					sharedPreferencesHandler.setLicenseToken(purchase.purchaseToken)
				}
				if (!purchase.isAcknowledged) {
					val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchase.purchaseToken)
						.build()
					billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
				}
				return
			}
		}
		if (sharedPreferencesHandler.licenseToken().isNotEmpty()) {
			Timber.tag("IapBillingService").i("Remove license, purchase does not exist anymore")
			sharedPreferencesHandler.setLicenseToken("")
		}
	}

	private fun handleSubscriptionPurchases(purchases: List<Purchase>) {
		for (purchase in purchases) {
			if (!purchase.products.contains(yearlySubscriptionProductId)) {
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				Timber.tag("IapBillingService").i("Subscription found: %s", purchase.signature)
				sharedPreferencesHandler.setHasRunningSubscription(true)
				if (!purchase.isAcknowledged) {
					val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchase.purchaseToken)
						.build()
					billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
				}
				return
			}
		}
		sharedPreferencesHandler.setHasRunningSubscription(false)
	}

	override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
		Timber.tag("IapBillingService").i("Purchase acknowledged")
	}

	fun queryProductDetails(callback: (List<ProductInfo>) -> Unit) {
		if (!billingClient.isReady) {
			pendingProductDetailsCallbacks.add(callback)
			return
		}
		val lock = Any()
		val results = mutableListOf<ProductInfo>()
		var queriesCompleted = 0
		val totalQueries = 2

		fun onQueryComplete() {
			val readyResults: List<ProductInfo>?
			synchronized(lock) {
				queriesCompleted++
				readyResults = if (queriesCompleted == totalQueries) ArrayList(results) else null
			}
			readyResults?.let { callback(it) }
		}

		// Query INAPP products
		val inappParams = QueryProductDetailsParams.newBuilder().setProductList(
			listOf(
				QueryProductDetailsParams.Product.newBuilder()
					.setProductId(fullVersionProductId)
					.setProductType(BillingClient.ProductType.INAPP)
					.build()
			)
		).build()
		billingClient.queryProductDetailsAsync(inappParams) { billingResult, productDetailsResult ->
			if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
				synchronized(lock) {
					for (productDetails in productDetailsResult.productDetailsList) {
						productDetailsMap[productDetails.productId] = productDetails
						results.add(
							ProductInfo(
								productDetails.productId,
								productDetails.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
							)
						)
					}
				}
			}
			onQueryComplete()
		}

		// Query SUBS products (must be separate — Billing Library requires same product type per query)
		val subsParams = QueryProductDetailsParams.newBuilder().setProductList(
			listOf(
				QueryProductDetailsParams.Product.newBuilder()
					.setProductId(yearlySubscriptionProductId)
					.setProductType(BillingClient.ProductType.SUBS)
					.build()
			)
		).build()
		billingClient.queryProductDetailsAsync(subsParams) { billingResult, productDetailsResult ->
			if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
				synchronized(lock) {
					for (productDetails in productDetailsResult.productDetailsList) {
						productDetailsMap[productDetails.productId] = productDetails
						val pricingPhase = productDetails.subscriptionOfferDetails
							?.firstOrNull()
							?.pricingPhases
							?.pricingPhaseList
							?.firstOrNull()
						results.add(
							ProductInfo(
								productDetails.productId,
								pricingPhase?.formattedPrice ?: ""
							)
						)
					}
				}
			}
			onQueryComplete()
		}
	}

	fun launchPurchaseFlow(activity: WeakReference<Activity>, productId: String) {
		val details = productDetailsMap[productId]
		if (details == null) {
			Timber.tag("IapBillingService").w("Product details not loaded for %s", productId)
			return
		}
		val paramsBuilder = ProductDetailsParams.newBuilder().setProductDetails(details)
		if (details.productType == BillingClient.ProductType.SUBS) {
			details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let {
				paramsBuilder.setOfferToken(it)
			}
		}
		val billingFlowParams = BillingFlowParams.newBuilder()
			.setProductDetailsParamsList(listOf(paramsBuilder.build()))
			.build()
		activity.get()?.let { billingClient.launchBillingFlow(it, billingFlowParams) }
	}

	override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
		if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
			handleInAppPurchases(purchases)
			handleSubscriptionPurchases(purchases)
		} else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
			Timber.tag("IapBillingService").i("User canceled purchase flow")
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		billingClient.endConnection()
		Timber.tag("IapBillingService").i("Service destroyed")
	}

	override fun onBind(intent: Intent?): IBinder = Binder(this)

	class Binder(private val service: IapBillingService) : android.os.Binder() {

		fun init(context: Context) {
			service.initBillingClient(context)
		}

		fun startPurchaseFlow(activity: WeakReference<Activity>, productId: String) {
			service.launchPurchaseFlow(activity, productId)
		}

		fun queryProductDetails(callback: (List<ProductInfo>) -> Unit) {
			service.queryProductDetails(callback)
		}

		fun restorePurchases() {
			service.queryExistingPurchases()
		}
	}
}
