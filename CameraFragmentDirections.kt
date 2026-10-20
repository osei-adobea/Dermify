package com.dermify.dermify.ui.camera

import android.os.Bundle
import androidx.`annotation`.CheckResult
import androidx.navigation.NavDirections
import com.dermify.dermify.R
import kotlin.Int
import kotlin.String

public class CameraFragmentDirections private constructor() {
  private data class ActionCameraToResult(
    public val imagePath: String,
  ) : NavDirections {
    public override val actionId: Int = R.id.action_camera_to_result

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("imagePath", this.imagePath)
        return result
      }
  }

  public companion object {
    @CheckResult
    public fun actionCameraToResult(imagePath: String): NavDirections = ActionCameraToResult(imagePath)
  }
}
