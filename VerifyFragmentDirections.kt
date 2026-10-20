package com.dermify.dermify.ui.verify

import android.os.Bundle
import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import com.dermify.dermify.R
import kotlin.Float
import kotlin.Int
import kotlin.String

public class VerifyFragmentDirections private constructor() {
  private data class ActionVerifyToConsent(
    public val imagePath: String,
    public val label: String,
    public val confidence: Float = 0.0F,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_verify_to_consent

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("imagePath", this.imagePath)
        result.putString("label", this.label)
        result.putFloat("confidence", this.confidence)
        return result
      }
  }

  public companion object {
    @CheckResult
    public fun actionVerifyToCamera(): NavDirections = ActionOnlyNavDirections(R.id.action_verify_to_camera)

    @CheckResult
    public fun actionVerifyToConsent(
      imagePath: String,
      label: String,
      confidence: Float = 0.0F,
    ): NavDirections = ActionVerifyToConsent(imagePath, label, confidence)
  }
}
