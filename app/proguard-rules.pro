# Keep reflection targets for vivo SuperX / OriginIsland hidden APIs.
# These are resolved by name at runtime (NotificationManager.setSuperXInfosSceneList, etc.)
-keep class com.originisle.android.** { *; }
-keep class com.vivo.** { *; }
-keepclassmembers class android.app.NotificationManager {
    *** setSuperXInfosSceneList(...);
    *** getSceneStatus(...);
    *** isSupportCustomFun(...);
}
-dontwarn com.vivo.**
-dontwarn android.app.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Shizuku API & Binder/AIDL classes
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn moe.shizuku.**
