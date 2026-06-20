# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }

# usb-serial-for-android probes drivers reflectively
-keep class com.hoho.android.usbserial.driver.** { *; }
