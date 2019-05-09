package com.example.hudsonmcashan.guradianaangel

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*

class LoginActivity : AppCompatActivity() {
    val EMAIL = "email"
    val PUBLIC_PROFILE = "public_profile"
    val USER_PERMISSION = "user_friends"
    override fun onCreate(savedInstanceState: Bundle?) {
        var callbackManager: CallbackManager? = null
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        login_button.setOnClickListener {
            performLogin()
        }

        dont_have_account_textView.setOnClickListener {
            Log.d("LoginActivity", "Trying to show register page")
            // launch the login activity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        callbackManager = CallbackManager.Factory.create()
        facebook_login_button.setOnClickListener {
            facebook_login_button.setReadPermissions(Arrays.asList(EMAIL, PUBLIC_PROFILE, USER_PERMISSION))
            facebook_login_button.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {

                }

                override fun onCancel() {

                }

                override fun onError(exception: FacebookException) {

                }
            })
        }

    }

    private fun performLogin() {
        val email = email_editText_login.text.toString()
        val password = password_editText_login.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill out email/password", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    it.result?.let {
                        Log.d("Login", "Successfully logged in: ${it.user.uid}")
                    } ?: run {
                        Log.d("Login", "Successfully logged in but result is null")
                    }

                    val intent = Intent(this, DeviceActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
                }
    }
}
