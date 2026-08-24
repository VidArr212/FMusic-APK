# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-dontwarn javax.annotation.**

# Retrofit & Gson
-keepattributes Signature
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.fmusic.app.data.model.** { *; }

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
