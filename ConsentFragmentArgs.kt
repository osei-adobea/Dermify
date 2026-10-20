package com.dermify.dermify.ui.consent

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.Float
import kotlin.String
import kotlin.jvm.JvmStatic

public data class ConsentFragmentArgs(
  public val imagePath: String,
  public val label: String,
  public val confidence: Float = 0.0F,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("imagePath", this.imagePath)
    result.putString("label", this.label)
    result.putFloat("confidence", this.confidence)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("imagePath", this.imagePath)
    result.set("label", this.label)
    result.set("confidence", this.confidence)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle): ConsentFragmentArgs {
      bundle.setClassLoader(ConsentFragmentArgs::class.java.classLoader)
      val __imagePath : String?
      if (bundle.containsKey("imagePath")) {
        __imagePath = bundle.getString("imagePath")
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      val __label : String?
      if (bundle.containsKey("label")) {
        __label = bundle.getString("label")
        if (__label == null) {
          throw IllegalArgumentException("Argument \"label\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"label\" is missing and does not have an android:defaultValue")
      }
      val __confidence : Float
      if (bundle.containsKey("confidence")) {
        __confidence = bundle.getFloat("confidence")
      } else {
        __confidence = 0.0F
      }
      return ConsentFragmentArgs(__imagePath, __label, __confidence)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): ConsentFragmentArgs {
      val __imagePath : String?
      if (savedStateHandle.contains("imagePath")) {
        __imagePath = savedStateHandle["imagePath"]
        if (__imagePath == null) {
          throw IllegalArgumentException("Argument \"imagePath\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"imagePath\" is missing and does not have an android:defaultValue")
      }
      val __label : String?
      if (savedStateHandle.contains("label")) {
        __label = savedStateHandle["label"]
        if (__label == null) {
          throw IllegalArgumentException("Argument \"label\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"label\" is missing and does not have an android:defaultValue")
      }
      val __confidence : Float?
      if (savedStateHandle.contains("confidence")) {
        __confidence = savedStateHandle["confidence"]
        if (__confidence == null) {
          throw IllegalArgumentException("Argument \"confidence\" of type float does not support null values")
        }
      } else {
        __confidence = 0.0F
      }
      return ConsentFragmentArgs(__imagePath, __label, __confidence)
    }
  }
}
