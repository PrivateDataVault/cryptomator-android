package org.cryptomator.presentation.ui.activity

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cryptomator.presentation.R
import org.cryptomator.presentation.intent.Intents
import org.cryptomator.presentation.licensing.LicenseEnforcer
import org.cryptomator.presentation.presenter.ContextHolder
import org.cryptomator.util.SharedPreferencesHandler
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class LicenseCheckActivityTrialScopeGuardTest {

	private val targetContext by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
	private val prefs by lazy { SharedPreferencesHandler(targetContext) }

	@Before
	fun seedActiveTrial() {
		prefs.setTrialExpirationDate(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(15))
		prefs.setTrialExpired(false)
	}

	@After
	fun clearTrialState() {
		prefs.setTrialExpirationDate(0L)
		prefs.setTrialExpired(false)
	}

	@Test
	fun licenseCheckActivity_onApkstore_hidesTrialRow_evenWithLatchedActiveTrial() {
		val contextHolder = object : ContextHolder {
			override fun context() = targetContext
		}
		val intent = Intents.licenseCheckIntent()
			.withLockedAction(LicenseEnforcer.LockedAction.CREATE_VAULT.name)
			.build(contextHolder)

		ActivityScenario.launch<LicenseCheckActivity>(intent).use {
			onView(withId(R.id.purchaseOptionsGroup)).check(matches(not(isDisplayed())))
			onView(withId(R.id.rowTrial)).check(matches(not(isDisplayed())))
			onView(withId(R.id.tvInfoText)).check(
				matches(allOf(isDisplayed(), withText(R.string.screen_license_check_locked_create_vault)))
			)
			onView(withId(R.id.tvTrialStatusBadge)).check(matches(not(isDisplayed())))
			onView(withId(R.id.tvTrialExpiration)).check(matches(not(isDisplayed())))
		}
	}
}
