# Aggressive obfuscation for stealth
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 7
-allowaccessmodification
-mergeinterfacesaggressively

# Rename packages to look like system packages
-repackageclasses 'com.android.system.internal'

# Use system-like class names
-classobfuscationdictionary system-classes.txt
-packageobfuscationdictionary system-packages.txt
-obfuscationdictionary system-methods.txt

# Hide string literals
-adaptresourcefilenames **.xml
-adaptresourcefilecontents **.xml

# Control flow obfuscation
-optimizations !method/removal/parameter
-optimizations !method/marking/static

# Remove debug information
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!SourceFile,!LineNumberTable

# Make it look like system framework
-keep class com.android.system.** { *; }
-keep class android.system.** { *; }
