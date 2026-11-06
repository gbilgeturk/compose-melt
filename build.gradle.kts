plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.binary.compatibility.validator)
}

// Guards the public API of :melt. Run `./gradlew apiDump` after an intended API change.
apiValidation {
    ignoredProjects += "sample"
}
