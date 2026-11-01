# Keep Firebase and Google Play Services
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Task Gullak Data Models so Firestore reflection works cleanly
-keep class com.taskgullak.app.data.model.** { *; }
-keepclassmembers class com.taskgullak.app.data.model.** { *; }

# Keep Jackson / Gson / Firestore serializers if any
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
