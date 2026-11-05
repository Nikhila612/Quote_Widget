# Sunote release rules. The libraries used (Glance, WorkManager, DataStore, Compose) ship their own
# keep rules, and quotes are parsed with android's built-in org.json, so nothing extra is needed yet.

# Keep line numbers so crash reports from the Play Console can be read (with the mapping file).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# WorkManager opens its Room database by reflection (WorkDatabase_Impl's no-argument constructor),
# which R8 full mode would otherwise remove: the app then crashes at start-up.
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# Glance creates widget actions by class name (actionRunCallback<MoodAction>), so keep their constructors.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
