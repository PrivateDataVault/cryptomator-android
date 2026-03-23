package org.cryptomator.presentation.ui.fragment

import android.os.Bundle
import android.view.View
import org.cryptomator.generator.Fragment
import org.cryptomator.presentation.databinding.FragmentWelcomeIntroBinding

@Fragment
class WelcomeIntroFragment : BaseFragment<FragmentWelcomeIntroBinding>(FragmentWelcomeIntroBinding::inflate) {

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
	}

	override fun setupView() {
		// static content only
	}
}
