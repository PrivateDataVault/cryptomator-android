package org.cryptomator.presentation.service

import com.android.billingclient.api.Purchase
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PurchaseManagerTest {

	private val sharedPreferencesHandler: SharedPreferencesHandler = mock()
	private lateinit var purchaseManager: PurchaseManager

	private var acknowledgedTokens = mutableListOf<String>()
	private val acknowledgePurchase: (String) -> Unit = { token -> acknowledgedTokens.add(token) }

	@BeforeEach
	fun setUp() {
		purchaseManager = PurchaseManager(sharedPreferencesHandler)
		acknowledgedTokens.clear()
	}

	// -- handleInAppPurchases --

	@Test
	fun `handleInAppPurchases sets license token when purchase is PURCHASED`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PURCHASED, "token-1", isAcknowledged = true)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setLicenseToken("token-1")
	}

	@Test
	fun `handleInAppPurchases skips pending purchase`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PENDING, "token-1", isAcknowledged = false)

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setLicenseToken(org.mockito.ArgumentMatchers.anyString())
	}

	@Test
	fun `handleInAppPurchases clears token when clearIfNotFound and no matching purchase`() {
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("old-token")

		purchaseManager.handleInAppPurchases(emptyList(), clearIfNotFound = true, acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setLicenseToken("")
	}

	@Test
	fun `handleInAppPurchases acknowledges unacknowledged purchase`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PURCHASED, "token-1", isAcknowledged = false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		assertEquals(listOf("token-1"), acknowledgedTokens)
	}

	@Test
	fun `handleInAppPurchases does not overwrite existing license token`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PURCHASED, "token-2", isAcknowledged = true)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("existing-token")

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setLicenseToken(org.mockito.ArgumentMatchers.anyString())
	}

	// -- handleSubscriptionPurchases --

	@Test
	fun `handleSubscriptionPurchases sets running subscription when purchase is PURCHASED`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION, Purchase.PurchaseState.PURCHASED, "sub-token", isAcknowledged = true)

		purchaseManager.handleSubscriptionPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setHasRunningSubscription(true)
	}

	@Test
	fun `handleSubscriptionPurchases skips pending subscription`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION, Purchase.PurchaseState.PENDING, "sub-token", isAcknowledged = false)

		purchaseManager.handleSubscriptionPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setHasRunningSubscription(org.mockito.ArgumentMatchers.anyBoolean())
	}

	@Test
	fun `handleSubscriptionPurchases clears subscription when clearIfNotFound and no matching purchase`() {
		purchaseManager.handleSubscriptionPurchases(emptyList(), clearIfNotFound = true, acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setHasRunningSubscription(false)
	}

	@Test
	fun `handleSubscriptionPurchases acknowledges unacknowledged subscription`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION, Purchase.PurchaseState.PURCHASED, "sub-token", isAcknowledged = false)

		purchaseManager.handleSubscriptionPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		assertEquals(listOf("sub-token"), acknowledgedTokens)
	}

	private fun mockPurchase(productId: String, purchaseState: Int, token: String, isAcknowledged: Boolean): Purchase {
		val purchase: Purchase = mock()
		`when`(purchase.products).thenReturn(listOf(productId))
		`when`(purchase.purchaseState).thenReturn(purchaseState)
		`when`(purchase.purchaseToken).thenReturn(token)
		`when`(purchase.isAcknowledged).thenReturn(isAcknowledged)
		`when`(purchase.signature).thenReturn("mock-signature")
		return purchase
	}
}
