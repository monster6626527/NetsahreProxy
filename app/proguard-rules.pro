# Aggressive R8 Obfuscation Rules
-repackageclasses ''
-allowaccessmodification
-overloadaggressively
-optimizationpasses 5

# Remove Source and Line numbers for max obfuscation
-keepattributes !SourceFile,!LineNumberTable

# Strip all debug logging from release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** e(...);
    public static *** w(...);
}

# Keep Coroutines safe
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose attributes
-keepattributes *Annotation*
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable *;
}

# Keep necessary Android classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep serialization/parcelable if needed
-keepclassmembers class * implements android.os.Parcelable {
    static android.os.Parcelable$Creator CREATOR;
}

# Keep Go Mobile & Datapath JNI bindings
-keep class go.** { *; }
-dontwarn go.**
-keep class datapath.** { *; }
-dontwarn datapath.**
-keep class com.admin.vpnshare.shizuku.** { *; }
-dontwarn com.admin.vpnshare.shizuku.**
-keep class rikka.shizuku.** { *; }

