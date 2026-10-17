package org.dazai.osmium

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import org.dazai.osmium.db.DatabaseDriverFactory
import org.dazai.osmium.ui.App

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle Fragment restoration crash for fragments without default constructors (like Readium's)
        supportFragmentManager.fragmentFactory = object : FragmentFactory() {
            override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
                return try {
                    super.instantiate(classLoader, className)
                } catch (_: Exception) {
                    // Fallback to a blank Fragment to avoid FATAL EXCEPTION during restoration.
                    // The real fragment will be re-created/replaced by the Compose UI once the data is loaded.
                    Fragment()
                }
            }
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(DatabaseDriverFactory(this))
        }
    }
}

