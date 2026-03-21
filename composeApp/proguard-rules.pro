# kotlinx.serialization — keep @Serializable classes and generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.jordankurtz.piawaremobile.**$$serializer { *; }
-keepclassmembers class com.jordankurtz.piawaremobile.** {
    *** Companion;
}
-keepclasseswithmembers class com.jordankurtz.piawaremobile.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Koin — keep generated DI module code
-keep class org.koin.ksp.generated.** { *; }

# Ktor — keep engine and plugin classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Sentry — keep crash reporting classes
-dontwarn io.sentry.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# MapCompose — keep tile provider interface
-dontwarn ovh.plrapps.mapcompose.**

# Keep enums used in serialization/settings
-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
