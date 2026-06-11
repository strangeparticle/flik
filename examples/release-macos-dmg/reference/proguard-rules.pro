# ILLUSTRATIVE EXAMPLE — not the real Springboard release ProGuard rules.
#
# Release (minified) Compose Desktop builds run ProGuard. Without a rules file the
# release packaging can fail on warnings or strip classes referenced only via
# reflection. These keep-rules are representative, not exhaustive.

# Compose Desktop / Skiko load native + reflective entry points; keep them intact.
-keep class org.jetbrains.skiko.** { *; }
-keep class androidx.compose.** { *; }

# Kotlin coroutines internal service loading.
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# The application entry point.
-keep class com.strangeparticle.springboard.** { *; }

# Silence warnings from optional/desktop-only dependencies pulled in transitively.
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
