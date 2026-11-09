# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class app.tony.data.** { *** Companion; }
-keepclasseswithmembers class app.tony.data.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class app.tony.data.**$$serializer { *; }
