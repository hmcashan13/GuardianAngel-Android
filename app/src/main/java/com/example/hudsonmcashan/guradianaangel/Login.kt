package com.example.hudsonmcashan.guradianaangel

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login_page.*
import java.util.*
import com.google.android.gms.common.SignInButton
import com.example.hudsonmcashan.guradianaangel.R
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider


class LoginActivity : AppCompatActivity() {
    val TAG = "LoginActivity"
    // Facebook Login property
    var callbackManager: CallbackManager? = null

    // Google Login properties
    val RC_SIGN_IN: Int = 1
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var mGoogleSignInOptions: GoogleSignInOptions

    // Firebase Login property
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)
        setTitle("Welcome!")
        // TODO: I don't think we need to do this
        //verifyUserIsLoggedIn()

        setupLoginButton()
        setupRegisterButton()
        setupFacebookLogin()
        setupGoogleLogin()
    }

    private fun verifyUserIsLoggedIn() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            launchDeviceActivity()
        }
    }

    private fun setupLoginButton() {
        login_button.setOnClickListener {
            firebaseLogin()
        }
    }

    private fun setupRegisterButton() {
        dont_have_account_textView.setOnClickListener {
            Log.d(TAG, "Trying to show register page")
            // launch the login activity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()
        facebook_login_button.setOnClickListener {
            facebook_login_button.setReadPermissions(Arrays.asList("email", "public_profile"))
            facebook_login_button.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(TAG, "facebook:onSuccess:$loginResult")
                    firebaseAuthWithFacebook(loginResult.accessToken)
                }

                override fun onCancel() {
                    Log.d(TAG, "facebook:onCancel")
                }

                override fun onError(exception: FacebookException) {
                    Log.d(TAG, "facebook:onError", exception)
                }
            })
        }
    }

    private fun setupGoogleLogin() {
        // Configure Google Login
        mGoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("195902502074-q748r8jh4u7kudfa03tsqioblv5ei22s.apps.googleusercontent.com")
                .requestEmail()
                .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, mGoogleSignInOptions)
        // Set the dimensions of the google login button
        google_login_button.setSize(SignInButton.SIZE_WIDE)
        google_login_button.setOnClickListener{
            val signInIntent: Intent = mGoogleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            // Google Login
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }

            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_LONG).show()
            }
        } else {
            // Facebook Login
            callbackManager?.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                launchDeviceActivity()
            } else {
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        Log.d(TAG, "handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = auth.currentUser
                        launchDeviceActivity()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Toast.makeText(baseContext, "Authentication with Facebook failed",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun firebaseLogin() {
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
                        Log.d(TAG, "Successfully logged in: ${it.user.uid}")
                    } ?: run {
                        Log.d(TAG, "Successfully logged in but result is null")
                    }
                    launchDeviceActivity()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to log in: ${it.message}", Toast.LENGTH_SHORT).show()
                }
    }

    private fun launchDeviceActivity() {
        val intent = Intent(this, DeviceActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
