// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}