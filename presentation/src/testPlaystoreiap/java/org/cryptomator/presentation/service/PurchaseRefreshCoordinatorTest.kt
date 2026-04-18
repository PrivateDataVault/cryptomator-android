package org.cryptomator.presentation.service

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.util.SharedPreferencesHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PurchaseRefreshCoordinatorTest {

	private val sharedPreferencesHandler: SharedPreferencesHandler = mock()
	private val licenseEnforcer: LicenseEnforcer = mock()
	private val billingClient: BillingClient = mock()
	private val purchaseManager: PurchaseManager = mock()

	private lateinit var coordinator: PurchaseRefreshCoordinator
	private val acknowledge: (String) -> Unit = { }

	@BeforeEach
	fun setUp() {
		coordinator = PurchaseRefreshCoordinator(sharedPreferencesHandler, licenseEnforcer)
	}

	@Test
	fun `onComplete fires exactly once after both callbacks`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = false, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
	}

	@Test
	fun `onComplete fires FAILED when billingClient isReady is false`() {
		stubBillingReady(false)

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
		assertTrue(outcomes[0] is RestoreOutcome.FAILED)
		verify(billingClient, never()).queryPurchasesAsync(any<QueryPurchasesParams>(), any<PurchasesResponseListener>())
	}

	@Test
	fun `onComplete fires FAILED when a callback has non-OK responseCode`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = false)
		doAnswer { invocation ->
			val listener = invocation.getArgument<PurchasesResponseListener>(1)
			val billingResult = buildBillingResult(BillingClient.BillingResponseCode.ERROR)
			listener.onQueryPurchasesResponse(billingResult, emptyList())
			null
		}.`when`(billingClient).queryPurchasesAsync(any<QueryPurchasesParams>(), any<PurchasesResponseListener>())

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
		assertTrue(outcomes[0] is RestoreOutcome.FAILED)
	}

	@Test
	fun `arms purchaseRevokedPending with LIFETIME_REFUNDED when inappChange cleared and writeAccess transitions true to false`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = true, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = true, after = false, cleared = true),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(sharedPreferencesHandler).setPurchaseRevokedState(pending = true, reason = PurchaseRevokedReason.LIFETIME_REFUNDED.name)
	}

	@Test
	fun `arms purchaseRevokedPending with SUBSCRIPTION_INACTIVE when only subsChange cleared`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = true, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = false, cleared = false),
			subsChange = PurchaseFieldChange(before = true, after = false, cleared = true),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(sharedPreferencesHandler).setPurchaseRevokedState(pending = true, reason = PurchaseRevokedReason.SUBSCRIPTION_INACTIVE.name)
	}

	@Test
	fun `prefers LIFETIME_REFUNDED when both cleared`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = true, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = true, after = false, cleared = true),
			subsChange = PurchaseFieldChange(before = true, after = false, cleared = true),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(sharedPreferencesHandler).setPurchaseRevokedState(pending = true, reason = PurchaseRevokedReason.LIFETIME_REFUNDED.name)
	}

	@Test
	fun `does not arm purchaseRevokedPending when writeAccess was already false before refresh`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = false, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(sharedPreferencesHandler, never()).setPurchaseRevokedState(anyBoolean(), anyString())
	}

	@Test
	fun `does not arm purchaseRevokedPending when writeAccess transitions false to true`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = true)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = true, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(sharedPreferencesHandler, never()).setPurchaseRevokedState(anyBoolean(), anyString())
	}

	@Test
	fun `onComplete is RESTORED when either change after is true`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = true)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = true, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
		assertEquals(RestoreOutcome.RESTORED, outcomes[0])
	}

	@Test
	fun `onComplete is NOTHING_TO_RESTORE when nothing was restored and nothing was cleared`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = false, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
		assertEquals(RestoreOutcome.NOTHING_TO_RESTORE, outcomes[0])
	}

	@Test
	fun `onComplete is NOTHING_TO_RESTORE when clear-only revoke path fires`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = true, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = true, after = false, cleared = true),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		val outcomes = mutableListOf<RestoreOutcome>()
		coordinator.refresh(billingClient, purchaseManager, acknowledge) { outcomes.add(it) }

		assertEquals(1, outcomes.size)
		assertEquals(RestoreOutcome.NOTHING_TO_RESTORE, outcomes[0])
	}

	@Test
	fun `coordinator calls licenseEnforcer hasWriteAccess before and after`() {
		stubBillingReady(true)
		stubHasWriteAccess(before = false, after = false)
		stubQueryPurchasesOk()
		stubHandlers(
			inappChange = PurchaseFieldChange(before = false, after = false, cleared = false),
			subsChange = PurchaseFieldChange(before = false, after = false, cleared = false),
		)

		coordinator.refresh(billingClient, purchaseManager, acknowledge) { }

		verify(licenseEnforcer, atLeast(2)).hasWriteAccess()
	}

	// -- helpers --

	private fun stubBillingReady(ready: Boolean) {
		`when`(billingClient.isReady).thenReturn(ready)
	}

	private fun stubHasWriteAccess(before: Boolean, after: Boolean) {
		`when`(licenseEnforcer.hasWriteAccess()).thenReturn(before, after)
	}

	private fun stubQueryPurchasesOk() {
		doAnswer { invocation ->
			val listener = invocation.getArgument<PurchasesResponseListener>(1)
			val billingResult = buildBillingResult(BillingClient.BillingResponseCode.OK)
			listener.onQueryPurchasesResponse(billingResult, emptyList())
			null
		}.`when`(billingClient).queryPurchasesAsync(any<QueryPurchasesParams>(), any<PurchasesResponseListener>())
	}

	private fun stubHandlers(inappChange: PurchaseFieldChange, subsChange: PurchaseFieldChange) {
		`when`(purchaseManager.handleInAppPurchases(any(), eq(true), any())).thenReturn(inappChange)
		`when`(purchaseManager.handleSubscriptionPurchases(any(), eq(true), any())).thenReturn(subsChange)
	}

	private fun buildBillingResult(responseCode: Int): BillingResult {
		return BillingResult.newBuilder().setResponseCode(responseCode).setDebugMessage("").build()
	}

	private inline fun <reified T> any(): T = org.mockito.ArgumentMatchers.any(T::class.java)
}
