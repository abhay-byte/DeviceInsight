# Add project specific ProGuard rules here.
-keep class com.ivarna.deviceinsight.** { *; }
-keepclassmembers class com.ivarna.deviceinsight.** { *; }

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
