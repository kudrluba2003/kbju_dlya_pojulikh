# ProGuard / R8 rules for SeniorKbju

# Room entities and database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Data models and entities
-keep class com.senior.kbju.data.local.entity.** { *; }
-keep class com.senior.kbju.data.local.projection.** { *; }
-keep class com.senior.kbju.data.model.** { *; }

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
