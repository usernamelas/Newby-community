# R8 Advanced Stealth Obfuscation Rules
# For maximum anti-reverse engineering protection

# Enable aggressive optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7
-allowaccessmodification
-mergeinterfacesaggressively

# Aggressive obfuscation
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Repackage to look like system framework
-repackageclasses 'com.android.internal.system'
-allowaccessmodification

# Advanced string obfuscation (R8 feature)
-adaptresourcefilenames **.xml
-adaptresourcefilecontents **.xml

# Class name obfuscation with system-like names
-classobfuscationdictionary system-classes.txt
-packageobfuscationdictionary system-packages.txt
-obfuscationdictionary system-methods.txt

# Advanced control flow obfuscation (R8 specific)
-assumevalues class android.os.Build$VERSION {
    int SDK_INT return 24..34;
}

# Remove more debug information
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!SourceFile,!LineNumberTable

# Hide reflection usage
-keepattributes *Annotation*
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Make it look like system framework
-keep class com.android.internal.system.** { *; }
-keep class android.system.** { *; }

# Remove stack traces (make debugging harder)
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public void printStackTrace(java.io.PrintStream);
    public void printStackTrace(java.io.PrintWriter);
}

# Obfuscate string constants
-assumenosideeffects class java.lang.String {
    public static java.lang.String valueOf(boolean);
    public static java.lang.String valueOf(char);
    public static java.lang.String valueOf(double);
    public static java.lang.String valueOf(float);
    public static java.lang.String valueOf(int);
    public static java.lang.String valueOf(long);
}

# Control flow obfuscation
-optimizations !method/removal/parameter
-optimizations !method/marking/static

# Remove unnecessary metadata
-keepattributes !SourceDir,!InnerClasses

# ===== ADD THESE TO FIX R8 MISSING CLASSES ERROR =====

# JSR-305 and javax.annotation (CRITICAL FIX)
-dontwarn javax.annotation.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.CheckForNull
-dontwarn javax.annotation.ParametersAreNonnullByDefault

# OKIO (used by libsu, causing the main error)
-dontwarn okio.**
-keep class okio.Buffer {
    okio.Segment head;
    <methods>;
}
-keep class okio.Segment {
    <fields>;
    <methods>;
}

# LibSU Root Framework (critical for Pro flavor)
-dontwarn com.topjohnwu.superuser.**
-keep class com.topjohnwu.superuser.Shell {
    <methods>;
}
-keep class com.topjohnwu.superuser.internal.** { *; }

# Additional missing classes that might cause issues
-dontwarn java.lang.instrument.**
-dontwarn sun.misc.Unsafe
-dontwarn java.nio.file.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Keep Pool Assistant core classes during stealth obfuscation
-keep class com.victory.poolassistant.MainActivity {
    protected void onCreate(android.os.Bundle);
}
-keep class com.victory.poolassistant.PoolAssistantApplication {
    public void onCreate();
}

# Keep BuildConfig for runtime feature detection
-keep class com.victory.poolassistant.BuildConfig {
    public static final boolean ROOT_FEATURES;
    public static final boolean LSPOSED_SUPPORT;
    public static final boolean OBFUSCATED;
    public static final boolean DEBUG_MODE;
}

# ===== END CRITICAL FIXES =====