package org.cryptomator.util

object FlavorConfig {

	/** Premium flavors (Play Store and Accrescent editions) ship with full access, so license enforcement is bypassed. */
	val isPremiumFlavor: Boolean
		get() = BuildConfig.FLAVOR == "playstore" || BuildConfig.FLAVOR == "accrescent"

	/** The freemium IAP (in-app purchase) flavor distributed on Google Play. */
	val isFreemiumFlavor: Boolean
		get() = BuildConfig.FLAVOR == "playstoreiap"

	/** The APK Store edition (website direct download). Supports self-update checks. */
	val isApkStoreFlavor: Boolean
		get() = BuildConfig.FLAVOR == "apkstore"

	/** The lite (F-Droid main repo) edition. Excludes all API-key-based cloud providers. */
	val isLiteFlavor: Boolean
		get() = BuildConfig.FLAVOR == "lite"

	/** Flavors that exclude Google Drive (no Google Play Services available). */
	val excludesGoogleDrive: Boolean
		get() = BuildConfig.FLAVOR == "fdroid" || BuildConfig.FLAVOR == "accrescent"
}
