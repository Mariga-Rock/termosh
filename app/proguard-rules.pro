# ============================================================
# Termosh — release ProGuard/R8 rules
# ============================================================

# --- sshj (через рефлексию) ---
-keep class net.schmizz.** { *; }
-keepclassmembers class net.schmizz.** { *; }
-dontwarn net.schmizz.**
-dontwarn org.slf4j.**

# --- BouncyCastle ---
-keep class org.bouncycastle.** { *; }
-keepclassmembers class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class javax.crypto.** { *; }

# --- SQLCipher ---
-keep class net.zetetic.** { *; }
-keep class net.sqlcipher.** { *; }
-dontwarn net.zetetic.**
-dontwarn net.sqlcipher.**

# --- AndroidX Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# --- Hilt / Dagger (обычно хватает встроенных правил, но на всякий) ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper

# --- JNI (mosh_pty) ---
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class app.termosh.core.mosh.MoshPty { *; }
-keep class app.termosh.core.mosh.MoshPty$Companion { *; }

# --- Kotlin Metadata / coroutines ---
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**

# --- Compose (встроенные правила обычно достаточны) ---
-dontwarn androidx.compose.**

# --- OkHttp / Conscrypt ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# --- Gson / JSON, если используется косвенно ---
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Общие метаданные ---
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, SourceFile, LineNumberTable

# --- Лицензирование (Ed25519 подписи через BouncyCastle) ---
-keep class app.termosh.core.licensing.** { *; }

# --- JSON (org.json — часть Android, не шринкается, но на всякий) ---
-keep class org.json.** { *; }
