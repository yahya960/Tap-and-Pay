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
-flattenpackagehierarchy test.hcesdk.mpay

#Keeping line number
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

#Google play
-keep class com.google.android.gms.internal.** { *; }

#okhttp
-dontwarn com.squareup.okhttp.**
-dontwarn org.apache.http.**

-dontwarn javax.naming.**

#JNA
-dontwarn com.sun.jna.**

#Others
-keep public class androidx.viewpager.widget.ViewPager {*;}

-keepclassmembers interface * extends com.sun.jna.Library { <methods>; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }

#HCE SDK
-dontwarn util.**
-dontwarn com.gemalto.mfs.mwsdk.**

#-keep public class util.** { public protected *; }
-keep class util.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
# Annotated interfaces
-keep public @interface com.gemalto.medl.MK {*;}

-keep public class util.m.rd { public *; }
-keep public class util.m.md { public *; }
-keep public class util.m.mj { public *; }
-keep public class util.d.a { public *; }
-keep public class com.gemalto.medl.rd {public *;}


