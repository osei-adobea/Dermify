package com.dermify.dermify.ui.consent

import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.dermify.dermify.R

public class ConsentFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun actionConsentToCamera(): NavDirections = ActionOnlyNavDirections(R.id.action_consent_to_camera)
  }
}
