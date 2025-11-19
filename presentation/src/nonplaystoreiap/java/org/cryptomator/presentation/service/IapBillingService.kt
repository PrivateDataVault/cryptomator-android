package org.cryptomator.presentation.service

import android.app.Activity
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import org.cryptomator.domain.repository.UpdateCheckRepository
import java.lang.ref.WeakReference
import timber.log.Timber

/**
 * Stub implementation used for flavors that do not bundle Google Play Billing.
 */
class IapBillingService : Service() {

	override fun onCreate() {
		super.onCreate()
		Timber.tag("IapBillingService").d("Stub service created")
	}

	override fun onBind(intent: Intent?): IBinder = Binder()

	class Binder : android.os.Binder() {

		fun init(updateCheckRepository: UpdateCheckRepository, context: Context) {
			// no-op
		}

		fun queryPurchases() {
			// no-op
		}

		fun startPurchaseFlow(activity: WeakReference<Activity>, productId: String) {
			// no-op
		}
	}
}
