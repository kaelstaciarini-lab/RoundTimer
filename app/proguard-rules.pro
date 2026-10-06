# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# Modelos usados via reflexão/serialização
-keepclassmembers class com.roundtimer.app.data.db.** { *; }
