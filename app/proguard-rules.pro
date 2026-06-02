# Keep kotlinx.serialization generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.charles.skypulse.app.**$$serializer { *; }
-keepclassmembers class com.charles.skypulse.app.** {
    *** Companion;
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepattributes Signature, Exceptions

# osmdroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
