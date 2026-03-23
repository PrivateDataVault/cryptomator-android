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
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import org.cryptomator.util.SharedPreferencesHandler
import java.lang.ref.WeakReference
import timber.log.Timber

class IapBillingService : Service(), PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

	private val fullVersionProductId = "full_version"

	private lateinit var billingClient: BillingClient
	private lateinit var sharedPreferencesHandler: SharedPreferencesHandler

	private fun initBillingClient(context: Context) {
		this.sharedPreferencesHandler = SharedPreferencesHandler(context)
		val pendingPurchasesParams = PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
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
				} else {
					Timber.tag("IapBillingService").e("Billing setup not successful, error: " + billingResult.responseCode)
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
		val params = QueryPurchasesParams.newBuilder()
			.setProductType(BillingClient.ProductType.INAPP)
			.build()
		billingClient.queryPurchasesAsync(params) { billingResult: BillingResult, purchases: List<Purchase> ->
			if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
				handlePurchases(purchases)
			}
		}
	}

	private fun handlePurchases(purchases: List<Purchase>) {
		for (purchase in purchases) {
			if (!purchase.products.contains(fullVersionProductId)) {
				continue
			}
			if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
				Timber.tag("IapBillingService").i("Purchase found: " + purchase.signature)
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
			Timber.tag("IapBillingService").i("Remove license, purchase does not exists anymore")
			sharedPreferencesHandler.setLicenseToken("")
		}
	}

	override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
		Timber.tag("IapBillingService").i("Purchase acknowledged")
	}

	fun launchPurchaseFlow(activity: WeakReference<Activity>) {
		if (billingClient.isReady) {
			val params = QueryProductDetailsParams.newBuilder()
				.setProductList(
					listOf(
						QueryProductDetailsParams.Product.newBuilder()
							.setProductId(fullVersionProductId)
							.setProductType(BillingClient.ProductType.INAPP)
							.build()
					)
				).build()

			billingClient.queryProductDetailsAsync(params) { billingResult, queryProductDetailsResult ->
				if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && queryProductDetailsResult.productDetailsList.isNotEmpty()) {
					queryProductDetailsResult.productDetailsList.first()?.let { productDetails ->
						val billingFlowParams = BillingFlowParams.newBuilder()
							.setProductDetailsParamsList(listOf(ProductDetailsParams.newBuilder().setProductDetails(productDetails).build()))
							.build()
						activity.get()?.let {
							billingClient.launchBillingFlow(it, billingFlowParams)
						}
					}
				}
			}
		}
	}

	override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
		if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
			handlePurchases(purchases)
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

		fun startPurchaseFlow(activity: WeakReference<Activity>) {
			service.launchPurchaseFlow(activity)
		}
	}
}
