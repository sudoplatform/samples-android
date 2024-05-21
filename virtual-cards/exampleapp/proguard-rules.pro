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

#### Stripe - reported by minifier ####
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheet$Companion
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheet$Configuration
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheet
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheetResult$Canceled
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheetResult$Completed
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheetResult$Failed
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheetResult
-dontwarn com.stripe.android.financialconnections.FinancialConnectionsSheetResultCallback
-dontwarn com.stripe.android.financialconnections.model.BankAccount
-dontwarn com.stripe.android.financialconnections.model.FinancialConnectionsAccount
-dontwarn com.stripe.android.financialconnections.model.FinancialConnectionsSession
-dontwarn com.stripe.android.financialconnections.model.PaymentAccount
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheet$CardScanResultCallback
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheet$Companion
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheet
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheetResult$Completed
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheetResult$Failed
-dontwarn com.stripe.android.stripecardscan.cardscan.CardScanSheetResult
-dontwarn com.stripe.android.stripecardscan.cardscan.exception.UnknownScanException
-dontwarn com.stripe.android.stripecardscan.payment.card.ScannedCard

# Without the below rules, Plaid Link crashes when opening the browser window
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
