# Preserve source file and line number information for debugging.
-keepattributes SourceFile,LineNumberTable

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Data Classes
-keepclassmembers class * {
    java.lang.String component1();
    java.lang.String component2();
    java.lang.String component3();
    java.lang.String component4();
    java.lang.String component5();
}

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Debug Logger Optimization
-assumenosideeffects class rajnishkmehta.sakshi.portal.debug.DebugLogger {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** init(...);
}
