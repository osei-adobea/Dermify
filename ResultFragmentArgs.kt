package com.dermify.dermify.ui.result

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class ResultFragmentArgs(
  public val imagePath: String,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("imagePath", this.imagePath)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("imagePath", this.imagePath)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): ResultFragmentArgs {
      bundle.setClassLoader(ResultFragmentArgs::class.java.classLoader)
      val __imagePath : String?
      if (bundle.containsKey("imagePath")) {
        __imagePath = bundle.getString("imagePath")
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      return ResultFragmentArgs(__imagePath)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): ResultFragmentArgs {
      val __imagePath : String?
      if (savedStateHandle.contains("imagePath")) {
        __imagePath = savedStateHandle["imagePath"]
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      return ResultFragmentArgs(__imagePath)
    }
  }
}
