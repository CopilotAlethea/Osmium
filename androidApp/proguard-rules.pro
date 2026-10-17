# Add project-specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\user\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any custom rules here.

# Readium
-keep class org.readium.r2.navigator.** { *; }
-keep class org.readium.r2.shared.** { *; }
-keep class org.readium.r2.streamer.** { *; }

# Keep all Fragments and their constructors (if they have one)
-keep public class * extends androidx.fragment.app.Fragment {
    public <init>();
}
