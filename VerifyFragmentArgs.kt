package com.dermify.dermify.ui.verify

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class VerifyFragmentArgs(
  public val imagePath: String,
  public val predictedDisease: String,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("imagePath", this.imagePath)
    result.putString("predictedDisease", this.predictedDisease)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("imagePath", this.imagePath)
    result.set("predictedDisease", this.predictedDisease)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): VerifyFragmentArgs {
      bundle.setClassLoader(VerifyFragmentArgs::class.java.classLoader)
      val __imagePath : String?
      if (bundle.containsKey("imagePath")) {
        __imagePath = bundle.getString("imagePath")
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      val __predictedDisease : String?
      if (bundle.containsKey("predictedDisease")) {
        __predictedDisease = bundle.getString("predictedDisease")
        if (__predictedDisease == null) {
          throw IllegalArgumentException("Argument \"predictedDisease\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"predictedDisease\" is missing and does not have an android:defaultValue")
      }
      return VerifyFragmentArgs(__imagePath, __predictedDisease)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): VerifyFragmentArgs {
      val __imagePath : String?
      if (savedStateHandle.contains("imagePath")) {
        __imagePath = savedStateHandle["imagePath"]
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      val __predictedDisease : String?
      if (savedStateHandle.contains("predictedDisease")) {
        __predictedDisease = savedStateHandle["predictedDisease"]
        if (__predictedDisease == null) {
          throw IllegalArgumentException("Argument \"predictedDisease\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"predictedDisease\" is missing and does not have an android:defaultValue")
      }
      return VerifyFragmentArgs(__imagePath, __predictedDisease)
    }
  }
}
