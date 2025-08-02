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