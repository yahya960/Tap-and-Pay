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
-printusage
-printmapping
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*,!code/allocation/variable,!class/unboxing/enum
-optimizationpasses 1
-allowaccessmodification
-dontpreverify
-verbose
-flattenpackagehierarchy util.h.xz

#Keeping line number
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

#Google play
-keep class com.google.android.gms.internal.** { *; }
-dontnote com.google.android.gms.internal.**
-dontnote com.google.android.gms.common.**
-dontnote com.google.android.material.**
-dontnote com.google.firebase.**
-dontnote com.journeyapps.barcodescanner.**

#okhttp
-dontwarn com.squareup.okhttp.**
-dontwarn org.apache.http.**

-dontwarn javax.naming.**
# supress warning
-dontwarn util.a.z.**
-dontwarn com.sun.jna.**
-dontwarn javax.naming.**

# supress notes
-dontnote util.**
-dontnote android.net.**
-dontnote org.apache.**
# Suppress notes about dynamic referenced class used by PRNGFixes (not part of Android API)
-dontnote org.apache.harmony.xnet.provider.jsse.NativeCrypto
-dontnote android.os.WorkSource
-dontnote kotlin.internal.jdk8.JDK8PlatformImplementations
-dontnote kotlin.internal.JRE8PlatformImplementations
-dontnote kotlin.internal.JRE7PlatformImplementations
-dontnote kotlin.reflect.jvm.internal.ReflectionFactoryImpl


#Others
-keep public class androidx.viewpager.widget.ViewPager {*;}

#HCE SDK
-dontwarn util.**
-dontwarn com.gemalto.mfs.mwsdk.**
-dontnote com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler

#-keep public class util.** { public protected *; }
-keep class util.* { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# Global JNA Rules
-keep,allowobfuscation interface com.sun.jna.Library
-keep,allowobfuscation interface com.sun.jna.Callback
-keep,allowobfuscation interface com.sun.jna.Function
#-keep,allowobfuscation interface com.sun.jna.CallbackReference$AttachOptions

-keep,allowobfuscation interface * implements com.sun.jna.Library

-keep,allowobfuscation interface * implements com.sun.jna.Callback

-keepclassmembers interface * implements com.sun.jna.Library {
    <methods>;
}

-keepclassmembers interface * implements com.sun.jna.Callback {
    <methods>;
}

-keep class com.sun.jna.CallbackReference {
    void dispose();
    com.sun.jna.Callback getCallback(java.lang.Class,com.sun.jna.Pointer,boolean);
    com.sun.jna.Pointer getFunctionPointer(com.sun.jna.Callback,boolean);
    com.sun.jna.Pointer getNativeString(java.lang.Object,boolean);
    java.lang.ThreadGroup initializeThread(com.sun.jna.Callback,com.sun.jna.CallbackReference$AttachOptions);
}

-keep,includedescriptorclasses class com.sun.jna.Native {
    com.sun.jna.Callback$UncaughtExceptionHandler callbackExceptionHandler;
    void dispose();
    java.lang.Object fromNative(com.sun.jna.FromNativeConverter,java.lang.Object,java.lang.reflect.Method);
    com.sun.jna.NativeMapped fromNative(java.lang.Class,java.lang.Object);
    com.sun.jna.NativeMapped fromNative(java.lang.reflect.Method,java.lang.Object);
    java.lang.Class nativeType(java.lang.Class);
    java.lang.Object toNative(com.sun.jna.ToNativeConverter,java.lang.Object);
    int getNativeSize(java.lang.Class);
}

-keep class com.sun.jna.Native$ffi_callback {
    void invoke(long,long,long);
}

-keep class com.sun.jna.Structure {
    long typeInfo;
    com.sun.jna.Pointer memory;
    <init>(int);
    void autoRead();
    void autoWrite();
    com.sun.jna.Pointer getTypeInfo();
    com.sun.jna.Structure newInstance(java.lang.Class,long);
}

-keep class com.sun.jna.Structure$FFIType$FFITypes {
    <fields>;
}

-keep class com.sun.jna.Structure$ByValue {
}

-keep class com.sun.jna.CallbackReference$AttachOptions {
    <fields>;
}

-keep class com.sun.jna.Callback$UncaughtExceptionHandler {
    void uncaughtException(com.sun.jna.Callback,java.lang.Throwable);
}

-keep class com.sun.jna.ToNativeConverter {
    java.lang.Class nativeType();
}

-keep class com.sun.jna.NativeMapped {
    java.lang.Object toNative();
}

-keep class com.sun.jna.IntegerType {
    long value;
}

-keep class com.sun.jna.PointerType {
    com.sun.jna.Pointer pointer;
}

-keep class com.sun.jna.LastErrorException {
    <init>(int);
    <init>(java.lang.String);
}

-keep class com.sun.jna.Pointer {
    long peer;
    <init>(long);
}

-keep class com.sun.jna.WString {
    <init>(java.lang.String);
}

-keep class com.sun.jna.JNIEnv { *; }

# Removing logging code - applicable for release mode library
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String,int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static int wtf(...);
}