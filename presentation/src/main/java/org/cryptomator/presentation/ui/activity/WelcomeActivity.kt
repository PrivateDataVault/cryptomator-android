package org.cryptomator.presentation.ui.activity

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import org.cryptomator.generator.Activity
import org.cryptomator.presentation.CryptomatorApp
import org.cryptomator.presentation.R
import org.cryptomator.presentation.databinding.ActivityWelcomeBinding
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.licensing.LicenseStateOrchestrator
import org.cryptomator.presentation.presenter.WelcomePresenter
import org.cryptomator.presentation.ui.activity.view.UpdateLicenseView
import org.cryptomator.presentation.ui.activity.view.WelcomeView
import org.cryptomator.presentation.ui.fragment.WelcomeIntroFragment
import org.cryptomator.presentation.ui.fragment.WelcomeLicenseFragment
import org.cryptomator.presentation.ui.fragment.WelcomeNotificationsFragment
import org.cryptomator.presentation.ui.fragment.WelcomeScreenLockFragment
import org.cryptomator.presentation.ui.layout.ObscuredAwareCoordinatorLayout
import org.cryptomator.util.FlavorConfig
import javax.inject.Inject

@Activity
class WelcomeActivity : BaseActivity<ActivityWelcomeBinding>(ActivityWelcomeBinding::inflate), //
	UpdateLicenseView, //
	WelcomeView, //
	WelcomeLicenseFragment.Listener, //
	WelcomeNotificationsFragment.Listener, //
	WelcomeScreenLockFragment.Listener {

	@Inject
	lateinit var welcomePresenter: WelcomePresenter

	@Inject
	lateinit var licenseEnforcer: LicenseEnforcer

	private val shouldShowLicenseSection: Boolean
		get() = !FlavorConfig.isPremiumFlavor

	private val isFreemiumFlavor: Boolean
		get() = FlavorConfig.isFreemiumFlavor

	private val keyguardManager by lazy { getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager }

	private val orchestrator by lazy {
		LicenseStateOrchestrator(
			sharedPreferencesHandler, licenseEnforcer, { this },
			target = object : LicenseStateOrchestrator.Target {
				override fun onPurchaseStateChanged(hasWriteAccess: Boolean, hasPaidLicense: Boolean) {
					if (!this@WelcomeActivity::pagerAdapter.isInitialized) return
					pagerAdapter.licenseFragment?.updateUnlocked(hasWriteAccess, hasPaidLicense)
				}
				override fun onTrialStateChanged(active: Boolean, expired: Boolean, expirationText: String?) {
					if (!this@WelcomeActivity::pagerAdapter.isInitialized) return
					pagerAdapter.licenseFragment?.updateTrialState(active, expired, expirationText)
				}
			},
			priceLoader = {
				if (this@WelcomeActivity::pagerAdapter.isInitialized) {
					pagerAdapter.licenseFragment?.loadAndBindPrices(application as CryptomatorApp)
				}
			}
		)
	}

	private lateinit var pagerAdapter: WelcomePagerAdapter
	private val pages = mutableListOf<FragmentPage>()
	private var navBasePaddingBottom: Int = 0

	override fun onNewIntent(intent: Intent) {
		super.onNewIntent(intent)
		validate(intent)
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
		orchestrator.updateState()
		updateScreenLockState()
	}

	override fun onResume() {
		super.onResume()
		if (sharedPreferencesHandler.hasCompletedWelcomeFlow() && !isFinishing) {
			openVaultList()
			return
		}
		orchestrator.onResume()
		updateNotificationPermissionState()
		updateScreenLockState()
	}

	override fun onPause() {
		super.onPause()
		orchestrator.onPause()
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
				if (isFreemiumFlavor && pages[position] is FragmentPage.License) {
					orchestrator.updateState()
				}
			}
		})
		updateNavigationButtons(0)
		binding.btnBack.setOnClickListener {
			val pos = binding.welcomePager.currentItem
			if (pos > 0) binding.welcomePager.currentItem = pos - 1
		}
		binding.btnNext.setOnClickListener {
			advanceOrComplete()
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

	override fun showOrUpdateLicenseEntry(license: String) {
		pagerAdapter.licenseFragment?.prefillLicense(license)
	}

	// In onboarding, a valid license auto-advances to the next page instead of showing a dialog
	override fun showConfirmationDialog(mail: String) {
		orchestrator.updateState()
		autoAdvanceToNextPage()
	}

	override fun onNotificationPermissionResult(granted: Boolean) {
		updateNotificationPermissionState(granted)
	}

	// WelcomeLicenseFragment.Listener

	override fun onLicenseTextChanged(license: String?) {
		welcomePresenter.validateDialogAware(license)
	}

	override fun onOpenLicenseLink() {
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cryptomator.org/android/")))
	}

	override fun onStartTrial() {
		licenseEnforcer.startTrial()
		orchestrator.updateState()
		autoAdvanceToNextPage()
	}

	override fun onSkipLicense() {
		advanceOrComplete()
	}

	// WelcomeNotificationsFragment.Listener

	override fun onRequestNotifications() {
		welcomePresenter.requestNotificationPermission()
	}

	// WelcomeScreenLockFragment.Listener

	override fun onSetScreenLock(setScreenLock: Boolean) {
		welcomePresenter.onSetScreenLock(setScreenLock)
	}

	private fun advanceOrComplete() {
		val pos = binding.welcomePager.currentItem
		if (pos < pagerAdapter.itemCount - 1) {
			binding.welcomePager.currentItem = pos + 1
		} else {
			completeWelcomeFlow()
		}
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

		val licenseFragment: WelcomeLicenseFragment?
			get() = findPageFragment<FragmentPage.License, WelcomeLicenseFragment>()

		val notificationsFragment: WelcomeNotificationsFragment?
			get() = findPageFragment<FragmentPage.Notifications, WelcomeNotificationsFragment>()

		val screenLockFragment: WelcomeScreenLockFragment?
			get() = findPageFragment<FragmentPage.ScreenLock, WelcomeScreenLockFragment>()

		private inline fun <reified P : FragmentPage, reified F : Fragment> findPageFragment(): F? {
			val pos = pages.indexOfFirst { it is P }
			return if (pos >= 0) supportFragmentManager.findFragmentByTag("f$pos") as? F else null
		}

		override fun getItemCount(): Int = pages.size

		override fun createFragment(position: Int): Fragment {
			return when (pages[position]) {
				is FragmentPage.Intro -> WelcomeIntroFragment()
				is FragmentPage.License -> WelcomeLicenseFragment()
				is FragmentPage.Notifications -> WelcomeNotificationsFragment()
				is FragmentPage.ScreenLock -> WelcomeScreenLockFragment()
			}
		}
	}

	companion object {
		private const val AUTO_ADVANCE_DELAY_MS = 500L
	}
}
