package com.dermify.dermify.ui.history

import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.dermify.dermify.R

public class HistoryFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun actionHistoryToCamera(): NavDirections = ActionOnlyNavDirections(R.id.action_history_to_camera)
  }
}
