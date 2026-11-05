# JavaMail and MIME activation load protocol providers and content handlers from META-INF resources.
-keep class com.sun.mail.** { *; }

# JNI entry points use this class name in the native bridge.
-keep class hrln.interactive.sidekit.apps.emulator.EmulatorCore { *; }
