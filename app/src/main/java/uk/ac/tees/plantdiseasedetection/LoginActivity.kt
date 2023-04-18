package uk.ac.tees.plantdiseasedetection

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var tvRedirectSignUp: TextView
    lateinit var etEmail: EditText
    private lateinit var etPass: EditText
    lateinit var btnLogin: Button
    lateinit var auth: FirebaseAuth
    val SHARED_PREFS = "shared_prefs"
    val EMAIL_KEY = "email_key"
    val PASSWORD_KEY = "password_key"
    private lateinit var sharedpreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvRedirectSignUp = findViewById(R.id.tvRedirectSignUp)
        btnLogin = findViewById(R.id.btnLogin)
        etEmail = findViewById(R.id.etEmailAddress)
        etPass = findViewById(R.id.etPassword)
        auth = FirebaseAuth.getInstance()
        sharedpreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        btnLogin.setOnClickListener {
            userLogin()
        }

        tvRedirectSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun userLogin() {
        val email = etEmail.text.toString()
        val pass = etPass.text.toString()

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(
                applicationContext,
                "Please enter email",
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(
                applicationContext,
                "Please enter password",
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(this) {
            if (it.isSuccessful) {
                Toast.makeText(this, "Successfully LoggedIn", Toast.LENGTH_SHORT).show()
                val editor = sharedpreferences.edit()
                editor.putString(EMAIL_KEY, etEmail.text.toString())
                editor.putString(PASSWORD_KEY, etPass.text.toString())
                editor.apply()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)

            } else
                Toast.makeText(this, "Log In failed ", Toast.LENGTH_SHORT).show()
        }
    }
}