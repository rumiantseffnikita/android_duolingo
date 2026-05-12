# Supabase
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.linguaai.app.models.**$$serializer { *; }
-keepclassmembers class com.linguaai.app.models.** { *** Companion; }
-keepclasseswithmembers class com.linguaai.app.models.** { kotlinx.serialization.KSerializer serializer(...); }
