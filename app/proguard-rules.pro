# Room
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# androidx.security-crypto pulls in Google Tink, which references optional annotation-only
# libraries (error-prone, JSR-305, checker-framework) that are never present at runtime and
# never actually called - safe to silence rather than keep.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.checkerframework.**
-dontwarn com.google.crypto.tink.**

# Kotlin coroutines / Compose runtime keep defaults are handled by the AGP/Compose plugin
