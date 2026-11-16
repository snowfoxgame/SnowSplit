# Keep Kotlin metadata & Hilt / Room / DataStore / Navigation symbols for R8 release builds
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}

-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
