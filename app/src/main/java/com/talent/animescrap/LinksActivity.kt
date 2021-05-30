package com.talent.animescrap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class LinksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_links)

        if (intent != null) {
            val textNames = findViewById<TextView>(R.id.textNames)
            val textLinks = findViewById<TextView>(R.id.textLinks)
            val linksNamesArray = intent.getStringArrayListExtra("nameOfLinks")
            val linksArray = intent.getStringArrayListExtra("theLinks")
            textNames.text = linksNamesArray?.toString() ?: "Nothing"
            textLinks.text = linksArray?.toString() ?: "Nothing"
        }
    }
}