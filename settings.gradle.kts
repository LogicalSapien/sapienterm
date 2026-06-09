@file:Suppress("ktlint:standard:property-naming")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "SapienTerm"

// Vendored ConnectBot terminal (libvterm + JNI + Compose). Source: external/termlib (see UPSTREAM.txt).
// includeBuild + dependencySubstitution replace org.connectbot:termlib so Terminal.kt and native code can be forked in-tree.
includeBuild("external/termlib") {
    dependencySubstitution {
        substitute(module("org.connectbot:termlib")).using(project(":lib"))
    }
}

val TRANSLATIONS_ONLY: String? by settings

if (TRANSLATIONS_ONLY.isNullOrBlank()) {
    include(":app")
}
include(":translations")
