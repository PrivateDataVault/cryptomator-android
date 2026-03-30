package org.cryptomator.presentation.service

import com.android.billingclient.api.Purchase
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
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

		verify(sharedPreferencesHandler, never()).setLicenseToken(anyString())
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

		verify(sharedPreferencesHandler, never()).setLicenseToken(anyString())
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

		verify(sharedPreferencesHandler, never()).setHasRunningSubscription(anyBoolean())
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

	// -- UNSPECIFIED_STATE handling --

	@Test
	fun `handleInAppPurchases with UNSPECIFIED_STATE does not set token or acknowledge`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.UNSPECIFIED_STATE, "token-1", isAcknowledged = false)

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setLicenseToken(anyString())
		assertEquals(emptyList<String>(), acknowledgedTokens)
	}

	@Test
	fun `handleSubscriptionPurchases with UNSPECIFIED_STATE does not set subscription`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION, Purchase.PurchaseState.UNSPECIFIED_STATE, "sub-token", isAcknowledged = false)

		purchaseManager.handleSubscriptionPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setHasRunningSubscription(anyBoolean())
	}

	@Test
	fun `handleInAppPurchases clears token when clearIfNotFound and only UNSPECIFIED purchase`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.UNSPECIFIED_STATE, "token-1", isAcknowledged = false)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("existing-token")

		purchaseManager.handleInAppPurchases(listOf(purchase), clearIfNotFound = true, acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setLicenseToken("")
	}

	// -- Mixed/multiple purchase lists --

	@Test
	fun `handleInAppPurchases skips non-matching product then processes matching PURCHASED`() {
		val otherProduct = mockPurchase("other_product", Purchase.PurchaseState.PURCHASED, "token-other", isAcknowledged = true)
		val fullVersion = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PURCHASED, "token-full", isAcknowledged = true)
		`when`(sharedPreferencesHandler.licenseToken()).thenReturn("")

		purchaseManager.handleInAppPurchases(listOf(otherProduct, fullVersion), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler).setLicenseToken("token-full")
	}

	@Test
	fun `handleInAppPurchases returns on PENDING before reaching PURCHASED for same product`() {
		val pending = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PENDING, "token-pending", isAcknowledged = false)
		val purchased = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, Purchase.PurchaseState.PURCHASED, "token-purchased", isAcknowledged = true)

		purchaseManager.handleInAppPurchases(listOf(pending, purchased), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setLicenseToken(anyString())
	}

	// -- Unknown state --

	@Test
	fun `handleInAppPurchases with unknown state 99 does not set token`() {
		val purchase = mockPurchase(ProductInfo.PRODUCT_FULL_VERSION, 99, "token-1", isAcknowledged = false)

		purchaseManager.handleInAppPurchases(listOf(purchase), acknowledgePurchase = acknowledgePurchase)

		verify(sharedPreferencesHandler, never()).setLicenseToken(anyString())
		assertEquals(emptyList<String>(), acknowledgedTokens)
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
