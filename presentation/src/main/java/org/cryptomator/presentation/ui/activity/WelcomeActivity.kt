package org.cryptomator.presentation.ui.activity

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.BuildConfig
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityWelcomeBinding
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.presenter.WelcomePresenter
import org.cryptomator.presentation.service.ProductInfo
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.presentation.ui.activity.view.WelcomeView
import org.cryptomator.presentation.ui.fragment.WelcomeIntroFragment
import org.cryptomator.presentation.ui.fragment.WelcomeLicenseFragment
import org.cryptomator.presentation.ui.fragment.WelcomeNotificationsFragment
import org.cryptomator.presentation.ui.fragment.WelcomeScreenLockFragment
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import java.lang.ref.WeakReference
import javax.inject.Inject

@Activity
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate), //
	UpdateLicenseView, //
	WelcomeView {

	@Inject
	lateinit var welcomePresenter: WelcomePresenter

	@Inject
	lateinit var licenseEnforcer: LicenseEnforcer

	private val shouldShowLicenseSection: Boolean
		get() = BuildConfig.FLAVOR != "playstore"

	private val isIapFlavor: Boolean
		get() = BuildConfig.FLAVOR == "playstoreiap"

	private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

	private lateinit var pagerAdapter: WelcomePagerAdapter
	private val pages = mutableListOf<FragmentPage>()
	private var navBasePaddingBottom: Int = 0

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		validate(intent)
	}

	override fun onCreate(savedInstanceState: android.os.Bundle?) {
		installSplashScreen()
		super.onCreate(savedInstanceState)
	}

	override fun setupView() {
		if (sharedPreferencesHandler.hasCompletedWelcomeFlow()) {
			openVaultList()
			return
		}

		setSupportActionBar(binding.mtToolbar.toolbar)
		supportActionBar?.title = getString(R.string.screen_welcome_title)
		supportActionBar?.setDisplayHomeAsUpEnabled(false)
		binding.mtToolbar.toolbar.navigationIcon = null

		binding.activityRootView.setOnFilteredTouchEventForSecurityListener(object : ObscuredAwareCoordinatorLayout.Listener {
			override fun onFilteredTouchEventForSecurity() {
				welcomePresenter.onFilteredTouchEventForSecurity()
			}
		})
		navBasePaddingBottom = binding.navigationContainer.paddingBottom
		ViewCompat.setOnApplyWindowInsetsListener(binding.navigationContainer) { view, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
			val extra = (8 * resources.displayMetrics.density).toInt()
			view.updatePadding(bottom = navBasePaddingBottom + systemBars.bottom + extra)
			insets
		}

		setupPages()
		setupPager()

		validate(intent)
		updateNotificationPermissionState()
		updateLicenseSectionState()
		updateScreenLockState()
	}

	override fun onResume() {
		super.onResume()
		if (sharedPreferencesHandler.hasCompletedWelcomeFlow() && !isFinishing) {
			openVaultList()
			return
		}
		updateNotificationPermissionState()
		updateLicenseSectionState()
		updateScreenLockState()
		if (isIapFlavor) {
			loadProductPrices()
		}
	}

	private fun setupPages() {
		pages.clear()
		pages.add(FragmentPage.Intro)
		if (shouldShowLicenseSection) {
			pages.add(FragmentPage.License)
		}
		pages.add(FragmentPage.Notifications)
		pages.add(FragmentPage.ScreenLock)
	}

	private fun setupPager() {
		pagerAdapter = WelcomePagerAdapter(this, pages)
		binding.welcomePager.adapter = pagerAdapter
		binding.welcomePager.setCurrentItem(0, false)
		binding.welcomePager.isUserInputEnabled = true
		binding.welcomePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				updateNavigationButtons(position)
				if (isIapFlavor && pages[position] is FragmentPage.License) {
					loadProductPrices()
				}
			}
		})
		updateNavigationButtons(0)
		binding.btnBack.setOnClickListener {
			val pos = binding.welcomePager.currentItem
			if (pos > 0) binding.welcomePager.currentItem = pos - 1
		}
		binding.btnNext.setOnClickListener {
			val pos = binding.welcomePager.currentItem
			if (pos < pagerAdapter.itemCount - 1) {
				binding.welcomePager.currentItem = pos + 1
			} else {
				completeWelcomeFlow()
			}
		}
	}

	private fun updateNavigationButtons(position: Int) {
		binding.btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
		binding.btnNext.text = if (position == pagerAdapter.itemCount - 1) {
			getString(R.string.screen_welcome_continue_button)
		} else {
			getString(R.string.next)
		}
	}

	private fun updateLicenseSectionState() {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		val unlocked = licenseEnforcer.hasWriteAccess()
		val hasPaidLicense = licenseEnforcer.hasPaidLicense()
		pagerAdapter.licenseFragment?.updateUnlocked(unlocked, hasPaidLicense)
		if (isIapFlavor) {
			val state = licenseEnforcer.evaluateTrialState()
			val expirationText = if (state.isActive) {
				getString(R.string.screen_license_check_trial_active, state.formattedExpirationDate)
			} else null
			pagerAdapter.licenseFragment?.updateTrialState(state.isActive, state.isExpired, expirationText)
		}
	}

	private fun loadProductPrices() {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		(application as CryptomatorApp).queryProductDetails { products ->
			runOnUiThread {
				val subscription = products.find { it.productId == ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION }
				val lifetime = products.find { it.productId == ProductInfo.PRODUCT_FULL_VERSION }
				pagerAdapter.licenseFragment?.updateProductPrices(
					subscription?.formattedPrice ?: "",
					lifetime?.formattedPrice ?: ""
				)
			}
		}
	}

	private fun needsNotificationPermission(): Boolean {
		return Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2
	}

	private fun hasNotificationPermission(): Boolean {
		return !needsNotificationPermission() || ContextCompat.checkSelfPermission(
			this,
			Manifest.permission.POST_NOTIFICATIONS
		) == PackageManager.PERMISSION_GRANTED
	}

	private fun updateNotificationPermissionState(grantedOverride: Boolean? = null) {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		if (!needsNotificationPermission()) {
			return
		}
		val granted = grantedOverride ?: hasNotificationPermission()
		pagerAdapter.notificationsFragment?.updatePermissionState(granted)
	}

	private fun updateScreenLockState() {
		if (!this::pagerAdapter.isInitialized) {
			return
		}
		pagerAdapter.screenLockFragment?.updateScreenLockState(keyguardManager.isKeyguardSecure)
	}

	private fun completeWelcomeFlow() {
		sharedPreferencesHandler.setWelcomeFlowCompleted()
		sharedPreferencesHandler.setScreenLockDialogAlreadyShown()
		openVaultList()
	}

	private fun openVaultList() {
		startActivity(Intent(this, VaultListActivity::class.java))
		finish()
	}

	private fun validate(intent: Intent?) {
		val data = intent?.data
		if (data != null && shouldShowLicenseSection) {
			welcomePresenter.validate(data)
		}
	}

	override fun showOrUpdateLicenseDialog(license: String) {
		pagerAdapter.licenseFragment?.prefillLicense(license)
	}

	// In onboarding, a valid license auto-advances to the next page instead of showing a dialog
	override fun showConfirmationDialog(mail: String) {
		updateLicenseSectionState()
		autoAdvanceToNextPage()
	}

	override fun onNotificationPermissionResult(granted: Boolean) {
		updateNotificationPermissionState(granted)
	}

	override fun onLicenseStateChanged() {
		updateLicenseSectionState()
	}

	private fun autoAdvanceToNextPage() {
		binding.welcomePager.postDelayed({
			val pos = binding.welcomePager.currentItem
			if (pos < pagerAdapter.itemCount - 1) {
				binding.welcomePager.currentItem = pos + 1
			}
		}, AUTO_ADVANCE_DELAY_MS)
	}

	private sealed class FragmentPage {
		object Intro : FragmentPage()
		object License : FragmentPage()
		object Notifications : FragmentPage()
		object ScreenLock : FragmentPage()
	}

	private inner class WelcomePagerAdapter(activity: AppCompatActivity, private val pages: List<FragmentPage>) : androidx.viewpager2.adapter.FragmentStateAdapter(activity) {
		var licenseFragment: WelcomeLicenseFragment? = null
		var notificationsFragment: WelcomeNotificationsFragment? = null
		var screenLockFragment: WelcomeScreenLockFragment? = null

		override fun getItemCount(): Int = pages.size

		override fun createFragment(position: Int): Fragment {
			return when (pages[position]) {
				is FragmentPage.Intro -> WelcomeIntroFragment()
				is FragmentPage.License -> WelcomeLicenseFragment().also { fragment ->
					fragment.setListener(object : WelcomeLicenseFragment.Listener {
						override fun onLicenseTextChanged(license: String?) {
							welcomePresenter.validateDialogAware(license)
						}

						override fun onOpenLicenseLink() {
							startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/android/")))
						}

						override fun onPurchaseClick() {
							(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this@WelcomeActivity), ProductInfo.PRODUCT_FULL_VERSION)
						}

						override fun onStartTrial() {
							licenseEnforcer.startTrial()
							updateLicenseSectionState()
							autoAdvanceToNextPage()
						}

						override fun onPurchaseSubscription() {
							(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this@WelcomeActivity), ProductInfo.PRODUCT_YEARLY_SUBSCRIPTION)
						}

						override fun onPurchaseLifetime() {
							(application as CryptomatorApp).launchPurchaseFlow(WeakReference(this@WelcomeActivity), ProductInfo.PRODUCT_FULL_VERSION)
						}

						override fun onRestorePurchases() {
							(application as CryptomatorApp).restorePurchases()
							Toast.makeText(this@WelcomeActivity, getString(R.string.screen_license_check_restore_purchase), Toast.LENGTH_SHORT).show()
						}

						override fun onSkipLicense() {
							val current = binding.welcomePager.currentItem
							if (current < pagerAdapter.itemCount - 1) {
								binding.welcomePager.currentItem = current + 1
							} else {
								completeWelcomeFlow()
							}
						}
					})
					licenseFragment = fragment
				}

				is FragmentPage.Notifications -> WelcomeNotificationsFragment().also { fragment ->
					fragment.setListener(object : WelcomeNotificationsFragment.Listener {
						override fun onRequestNotifications() {
							welcomePresenter.requestNotificationPermission()
						}
					})
					notificationsFragment = fragment
				}

				is FragmentPage.ScreenLock -> WelcomeScreenLockFragment().also { fragment ->
					fragment.setListener(object : WelcomeScreenLockFragment.Listener {
						override fun onSetScreenLock(setScreenLock: Boolean) {
							welcomePresenter.onSetScreenLock(setScreenLock)
						}
					})
					screenLockFragment = fragment
				}
			}
		}
	}

	companion object {
		private const val AUTO_ADVANCE_DELAY_MS = 500L
	}
}
