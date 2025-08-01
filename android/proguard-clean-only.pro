# ProGuard rules for Open Release build
# No obfuscation - keep all names readable for learning/debugging

# Don't obfuscate anything - keep all names readable
-dontobfuscate
-dontoptimize

# Keep all class and method names
-keepnames class ** { *; }
-keepclassmembernames class * { *; }

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

# Keep inner class information
-keepattributes InnerClasses,EnclosingMethod

# Only remove debug logs
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove BuildConfig debug fields but keep structure
-assumenosideeffects class **.BuildConfig {
    boolean DEBUG return false;
}

# Keep everything else as-is for learning purposes
-keep class com.victory.poolassistant.** { *; }

# Keep annotations for better readability
-keepattributes *Annotation*

# Keep generic signatures
-keepattributes Signature

# This configuration is perfect for:
# - Learning and understanding the code
# - Debugging production issues
# - Reverse engineering your own app
# - Code analysis and optimization
# - Educational purposes