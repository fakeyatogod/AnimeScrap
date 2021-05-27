package com.talent.animescrap

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager


class PageActivity : AppCompatActivity() , LoaderManager.LoaderCallbacks<String>{

    private var contentLink: String? = "null"
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        supportLoaderManager.initLoader(56,null,this)
    }


    class AsyncScrap(context: Context, contentLink: String?) : AsyncTaskLoader<String>(context) {
        private val url = "https://yugenani.me${contentLink}watch/?sort=episode"

        override fun loadInBackground(): String {
            println(url)
            return url
        }

        override fun onStartLoading() {
            forceLoad()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<String> {
        if (intent != null) {
            contentLink = intent.getStringExtra("content_link")
            if (contentLink == "null") {
                finish()
                Toast.makeText(this, "Some Unexpected error occurred", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            finish()
            Toast.makeText(this, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
        println("Hi Loader")

        return AsyncScrap(this,contentLink)
    }

    override fun onLoadFinished(loader: Loader<String>, data: String) {
        val textView = findViewById<TextView>(R.id.content_link)
        textView.text = data
    }

    override fun onLoaderReset(loader: Loader<String>) {
        TODO("Not yet implemented")
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}