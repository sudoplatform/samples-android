package com.sudoplatform.telephonyexample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.sudoplatform.sudologging.AndroidUtilsLogDriver
import com.sudoplatform.sudologging.Logger
import com.sudoplatform.sudologging.LogLevel
import com.sudoplatform.sudouser.RegisterResult
import com.sudoplatform.sudouser.RegistrationChallengeType
import com.sudoplatform.sudouser.SignInResult
import com.sudoplatform.sudouser.TESTAuthenticationProvider
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    lateinit var app: App
    private val errorLogger = Logger("telephonyExample", AndroidUtilsLogDriver(LogLevel.ERROR))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        app = application as App

        buttonRegister.setOnClickListener {
            registerAndSignIn()
        }

        // proceed to signIn activity if already registered
        if (app.sudoUserClient.isRegistered()) {
            registerAndSignIn()
        }
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
        buttonRegister.text = "Register / Login"
        buttonRegister.isEnabled = true

        val data = this.intent.data
        if (data != null) {
            app.sudoUserClient.processFederatedSignInTokens(data)
            val intent = Intent(this, SudosActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerAndSignIn() {
        buttonRegister.text = ""
        buttonRegister.isEnabled = false
        showLoading()

        val showRegistrationFailure = { e: Throwable ->
            hideLoading()
            runOnUiThread {
                buttonRegister.text = "Register / Login"
                buttonRegister.isEnabled = true
                Toast.makeText(this, "Failed to register: $e", Toast.LENGTH_LONG).show()
            }
        }

        val challengeTypes = app.sudoUserClient.getSupportedRegistrationChallengeType()
        if (challengeTypes.contains(RegistrationChallengeType.FSSO)) {
            app.sudoUserClient.presentFederatedSignInUI { result ->
                when (result) {
                    is SignInResult.Success -> {
                        val intent = Intent(this, SudosActivity::class.java)
                        startActivity(intent)
                    }
                    is SignInResult.Failure -> {
                        hideLoading()
                        runOnUiThread {
                            buttonRegister.text = "Register / Login"
                            buttonRegister.isEnabled = true
                            Toast.makeText(this, "Failed to Login: ${result.error}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            if (app.sudoUserClient.isRegistered()) {
                // If already registered, sign in
                signIn()
            } else {
                val privateKey: String
                var keyId: String

                try {
                    privateKey =
                        app.assets.open("register_key.private").bufferedReader().use {
                            it.readText().trim()
                        }
                    keyId =
                        app.assets.open("register_key.id").bufferedReader().use {
                            it.readText().trim()
                        }
                } catch (e: java.io.IOException) {
                    errorLogger?.error("Failed to load TEST registration keys: $e")
                    errorLogger?.outputError(Error(e))
                    showRegistrationFailure(e)
                    return
                }

                val authProvider = TESTAuthenticationProvider(
                    "testRegisterAudience",
                    privateKey,
                    null,
                    app.keyManager,
                    keyId
                )
                // register with auth provider
                app.sudoUserClient.registerWithAuthenticationProvider(
                    authProvider,
                    "dummy_rid"
                ) { result ->
                    when (result) {
                        // sign in upon successful registration
                        is RegisterResult.Success -> {
                            signIn()
                        }
                        is RegisterResult.Failure -> {
                            showRegistrationFailure(result.error)
                        }
                    }
                }
            }
        }
    }

    private fun signIn() {
        if (app.sudoUserClient.isSignedIn()) {
            // Refresh SudoUser CredentialsProvider tokens.
            app.sudoUserClient.getCredentialsProvider().logins = app.sudoUserClient.getLogins()

            val intent = Intent(this, SudosActivity::class.java)
            startActivity(intent)
            return
        }
        app.sudoUserClient.signInWithKey { result ->
            when (result) {
                // proceed to sudos activity upon successful sign in
                is SignInResult.Success -> {
                    val intent = Intent(this, SudosActivity::class.java)
                    startActivity(intent)
                }
                is SignInResult.Failure -> {
                    hideLoading()
                    runOnUiThread {
                        buttonRegister.text = "Register / Login"
                        buttonRegister.isEnabled = true
                        Toast.makeText(this, "Failed to Login: ${result.error}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showLoading() = runOnUiThread {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() = runOnUiThread {
        progressBar.visibility = View.GONE
    }
}
