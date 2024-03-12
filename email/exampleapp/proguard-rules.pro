# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#### Ignore issues with minified MQTT classes which we do not use ####
-dontwarn org.eclipse.paho.android.service.MqttAndroidClient
-dontwarn org.eclipse.paho.client.mqttv3.IMqttActionListener
-dontwarn org.eclipse.paho.client.mqttv3.IMqttMessageListener
-dontwarn org.eclipse.paho.client.mqttv3.IMqttToken
-dontwarn org.eclipse.paho.client.mqttv3.MqttCallback
-dontwarn org.eclipse.paho.client.mqttv3.MqttClientPersistence
-dontwarn org.eclipse.paho.client.mqttv3.MqttConnectOptions
-dontwarn org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
-dontwarn java.awt.Image
-dontwarn java.awt.Toolkit
-dontwarn javax.security.auth.callback.NameCallback
-dontwarn javax.security.sasl.RealmCallback
-dontwarn javax.security.sasl.RealmChoiceCallback
-dontwarn javax.security.sasl.Sasl
-dontwarn javax.security.sasl.SaslClient
-dontwarn javax.security.sasl.SaslClientFactory
-dontwarn javax.security.sasl.SaslException

#### Java Mail ####
-keeppackagenames javax.mail.**
-keeppackagenames javax.activation.**
-keeppackagenames com.sun.mail.**
-keep class javamail.** {*;}
-keep class javax.mail.** {*;}
-keep class javax.activation.** {*;}
-keep class com.sun.mail.dsn.** {*;}
-keep class com.sun.mail.handlers.** {*;}
-keep class com.sun.mail.smtp.** {*;}
-keep class com.sun.mail.util.** {*;}


