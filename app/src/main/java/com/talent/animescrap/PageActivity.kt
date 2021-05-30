package com.talent.animescrap

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
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
            val animeEpContent = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                .select("div:nth-child(6)").select("span").text()
            val animeCover = doc.getElementsByClass("cover").attr("src")
            println(animeCover)
            val animeDetails = arrayListOf<String>()
            for (element in animeContent) {
                animeDetails.add(element.text())
            }
            return AnimeDetails(animeDetails[0], animeDetails[1], animeCover, animeEpContent)
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
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        spinner.visibility = View.GONE
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
        val progressBar = findViewById<ProgressBar>(R.id.progressbarInPage)
        progressBar.visibility = View.GONE
        textView.visibility = View.VISIBLE
        textView2.visibility = View.VISIBLE
        coverImage.visibility = View.VISIBLE
        setupSpinner(data.animeEpisodes)

    }

    override fun onLoaderReset(loader: Loader<AnimeDetails>) {
        TODO("Not yet implemented")
    }


    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }

    private fun setupSpinner(num: String) {

        val epList = arrayListOf<String>()

        for (i in num.toInt() downTo 1) {
            epList.add(i.toString())
        }

        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        spinner.visibility = View.VISIBLE
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                Toast.makeText(parent.context, parent.selectedItem.toString(), Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, epList
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        val episodeButtonForSpinner = findViewById<Button>(R.id.episodeButtonForSpinner)
        episodeButtonForSpinner.setOnClickListener {
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugenani.me${watchLink}${spinner.selectedItem}"
            var streamLink = "null"

            Thread {
                try {
                    val streamAniLink = Jsoup.connect(animeEpUrl)
                        .get().getElementsByClass("anime-download").attr("href")

                    val goGoStreamLink = streamAniLink.replace(
                        "https://streamani.net/streaming.php?id=",
                        "https://gogo-stream.com/download?id="
                    )
                    val linksHashMap = LinkedHashMap<String, String>()

                    for (i in Jsoup.connect(goGoStreamLink).get().getElementsByClass("dowload")) {
                        linksHashMap[i.getElementsByTag("a").text().toString()
                            .replace("Download", "")] =
                            i.getElementsByTag("a").attr("href").toString()

                    }
                    for (i in linksHashMap.keys) {
                        println("$i = ${linksHashMap.get(i)}")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()

        }
    }


}