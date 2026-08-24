# Room generates code that is referenced reflectively by name.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Glance / AppWidget receivers are instantiated by the framework from the manifest.
-keep class com.pavithran.paisa.widget.** { *; }

# Kotlin metadata used by Compose tooling.
-keepattributes *Annotation*, InnerClasses, Signature
