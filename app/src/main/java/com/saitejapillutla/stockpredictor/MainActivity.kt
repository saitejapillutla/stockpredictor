package com.saitejapillutla.stockpredictor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.collections.hashMapOf as hashMapOf1

class MainActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private  val TAG = "MainActivityTAG"
    private  val RC_SIGN_IN = 9001
    private var signup =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


// Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.firebase_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance();

        gSignup.setOnClickListener {
            signIn()
            gSignup.visibility= View.GONE
            verifying.visibility =View.VISIBLE
            maildetail.visibility =View.GONE
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUser(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    //updateUI(null)
                }
            }
    }

    private fun updateUser(user: FirebaseUser?){

        if (user!=null){
            maildetail.visibility =View.VISIBLE
            presentemail.text=user.email
            var firstWrite =FirebaseDatabase.getInstance().reference

            var data = hashMapOf1("userName" to user.displayName,
            "userEmail" to user.email,
            "lastLogin" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "userUID" to user.uid)
            firstWrite.child("users").child(user.uid).updateChildren(data as Map<String, Any>).addOnSuccessListener {
                val intent=Intent(this,Home::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out)
                finish()
            }.addOnFailureListener {
                auth = FirebaseAuth.getInstance()
                auth.signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.firebase_client_id))
                    .requestEmail()
                    .build()
                googleSignInClient = GoogleSignIn.getClient(this, gso)
                googleSignInClient.signOut().addOnSuccessListener {Log.d(TAG, "Signed out Current User ${user}")
                    gSignup.visibility= View.VISIBLE
                    verifying.visibility =View.GONE
                    maildetail.visibility =View.VISIBLE}

            }
        }
    }
    public override fun onStart() {
        super.onStart()
        var user=auth.currentUser
        if (user!=null){
            gSignup.visibility= View.GONE
            verifying.visibility =View.VISIBLE
            maildetail.visibility =View.GONE
            maildetail.visibility =View.VISIBLE
            presentemail.text=user.email
            updateUser(user)
        }

    }
}