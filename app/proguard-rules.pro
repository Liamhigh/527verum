# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Kotlin reflect / Metadata
-keep class kotlin.reflect.jvm.internal.** {*;}
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Metadata

# Moshi Proguard rules
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Keep our data models and their generated Moshi JSON adapters
-keep class com.example.omnis.model.** { *; }
-keep class * implements com.squareup.moshi.JsonAdapter { *; }
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keepclassmembers class com.example.omnis.model.** { 
    <fields>; 
    <init>(...); 
}
-keep class com.example.omnis.model.*JsonAdapter { *; }

# Jetpack Room rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-dontwarn androidx.room.**

# Jetpack Compose lifecycle/viewmodel keep
-keep class androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel

