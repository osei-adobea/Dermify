package com.dermify.dermify.ui.result

import android.os.Bundle
import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.dermify.dermify.R
import kotlin.Int
import kotlin.String

public class ResultFragmentDirections private constructor() {
  private data class ActionResultToVerify(
    public val imagePath: String,
    public val predictedDisease: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_result_to_verify

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("imagePath", this.imagePath)
        result.putString("predictedDisease", this.predictedDisease)
        return result
      }
  }

  public companion object {
    @CheckResult
    public fun actionResultToVerify(imagePath: String, predictedDisease: String): NavDirections = ActionResultToVerify(imagePath, predictedDisease)

    @CheckResult
    public fun actionResultToCamera(): NavDirections = ActionOnlyNavDirections(R.id.action_result_to_camera)
  }
}
