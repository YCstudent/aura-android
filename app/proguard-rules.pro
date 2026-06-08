# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================================
# R8: 仅裁剪无用代码，不做混淆和字节码优化
# Gson / Retrofit / Kotlin suspend 等反射依赖类库在混淆和优化下极易出问题
# ============================================================================
-dontoptimize
-dontobfuscate

# Keep Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
# Keep Retrofit interfaces AND their members — Signature needed for generic return types
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
# Keep Retrofit internals (suspend/await type resolution)
-keep class retrofit2.** { *; }
-keep class retrofit2.KotlinExtensions { *; }
-keep class retrofit2.KotlinExtensions$* { *; }
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Gson TypeToken — generic type resolution (prevents "Class cannot be cast to ParameterizedType")
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep @SerializedName fields so Gson can find them at runtime
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep data models
-keep class com.edistrive.aura.data.model.** { *; }
