package uk.ac.tees.plantdiseasedetection

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashScreenActivity : AppCompatActivity() {

    val SHARED_PREFS = "shared_prefs"
    val EMAIL_KEY = "email_key"
    val PASSWORD_KEY = "password_key"
    private lateinit var sharedpreferences: SharedPreferences
    var email: String? = ""
    var password: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        sharedpreferences =
            getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        email = sharedpreferences.getString(EMAIL_KEY, null)
        password = sharedpreferences.getString(PASSWORD_KEY, null)

        Handler().postDelayed({
            if (email != null && password != null && email!!.isEmpty() && password!!.isEmpty()) {
                startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
            }
            finish()
        }, 3000)
    }
}