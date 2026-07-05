# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.rms.customs.**$$serializer { *; }
-keepclassmembers class com.rms.customs.** { *** Companion; }
-keepclasseswithmembers class com.rms.customs.** { kotlinx.serialization.KSerializer serializer(...); }
# Keep all @Serializable DTOs
-keep @kotlinx.serialization.Serializable class com.rms.customs.data.remote.dto.** { *; }

# Retrofit
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface com.rms.customs.data.remote.api.** { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.** { *; }
-dontwarn retrofit2.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Hilt
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepnames @dagger.hilt.InstallIn class *
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Domain model enums (used via reflection by Room)
-keepclassmembers enum com.rms.customs.domain.model.enums.** { *; }

# Error-prone annotations are compile-time only (pulled in transitively by security-crypto/Tink)
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
