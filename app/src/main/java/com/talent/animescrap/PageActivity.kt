package com.talent.animescrap

import android.content.Context
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import com.squareup.picasso.Picasso
import com.talent.animescrap.model.AnimeDetails
import org.jsoup.Jsoup


class PageActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<AnimeDetails> {

    private var contentLink: String? = "null"
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        supportLoaderManager.initLoader(56, null, this)
    }


    class AsyncScrap(context: Context, contentLink: String?) :
        AsyncTaskLoader<AnimeDetails>(context) {
        private val url = "https://yugenani.me${contentLink}watch/?sort=episode"
        override fun loadInBackground(): AnimeDetails {
            println(url)
            val doc = Jsoup.connect(url).get()
            val animeContent = doc.getElementsByClass("p-10-t")
            val animeCover = doc.getElementsByClass("cover").attr("src")
            println(animeCover)
            val animeDetails = arrayListOf<String>()
            for (element in animeContent) {
                animeDetails.add(element.text())
            }
            return AnimeDetails(animeDetails[0], animeDetails[1], animeCover)
        }

        override fun onStartLoading() {
            forceLoad()
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<AnimeDetails> {
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
        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        val progressBar = findViewById<ProgressBar>(R.id.progressbarInPage)
        textView.visibility = View.GONE
        textView2.visibility = View.GONE
        coverImage.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        return AsyncScrap(this, contentLink)
    }

    override fun onLoadFinished(loader: Loader<AnimeDetails>, data: AnimeDetails) {
        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        textView.text = data.animeName
        textView2.text = data.animeDesc
        Picasso.get().load(data.animeCover).error(R.drawable.ic_broken_image)
            .placeholder(R.drawable.pgi2).into(coverImage)
        var progressBar = findViewById<ProgressBar>(R.id.progressbarInPage)
        progressBar.visibility = View.GONE
        textView.visibility = View.VISIBLE
        textView2.visibility = View.VISIBLE
        coverImage.visibility = View.VISIBLE

    }

    override fun onLoaderReset(loader: Loader<AnimeDetails>) {
        TODO("Not yet implemented")
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}