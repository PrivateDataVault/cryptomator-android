package org.cryptomator.presentation.ui.fragment

import android.view.View
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.databinding.FragmentWelcomeScreenLockBinding

@Fragment
class WelcomeScreenLockFragment : BaseFragment<FragmentWelcomeScreenLockBinding>(FragmentWelcomeScreenLockBinding::inflate) {

	interface Listener {
		fun onSetScreenLock(setScreenLock: Boolean)
	}

	private var listener: Listener? = null

	fun setListener(listener: Listener) {
		this.listener = listener
	}

	override fun setupView() {
		setupUi()
	}

	private fun setupUi() {
		binding.btnSetScreenLock.setOnClickListener {
			listener?.onSetScreenLock(binding.cbSetScreenLock.isChecked)
		}
	}

	fun updateScreenLockState(isSecure: Boolean) {
		binding.btnSetScreenLock.isEnabled = !isSecure
		binding.cbSetScreenLock.isEnabled = !isSecure
		if (isSecure) {
			binding.cbSetScreenLock.isChecked = false
		}
		binding.tvScreenLockStatus.visibility = if (isSecure) View.VISIBLE else View.GONE
	}
}
