plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // 🔑 Plugin de Google Services (versión recomendada por Firebase)
    id("com.google.gms.google-services") version "4.4.3" apply false
}