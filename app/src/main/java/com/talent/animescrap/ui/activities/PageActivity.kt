package com.talent.animescrap.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.Fuel
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Picasso
import com.talent.animescrap.R
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.room.FavRoomModel
import com.talent.animescrap.room.LinksRoomDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup


class PageActivity : AppCompatActivity() {

    private var contentLink: String? = "null"
    private lateinit var pageLayout: LinearLayout
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var animeModel: AnimeDetails
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)
        supportActionBar?.title = "Anime Details"
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

        sharedPreferences = getSharedPreferences("LastWatchedPref", MODE_PRIVATE)
        val lastWatchedText = findViewById<TextView>(R.id.lastWatched)
        val lastWatchedPref = sharedPreferences.getString(contentLink, "Not Started Yet")
        lastWatchedText.text =
            if (lastWatchedPref == "Not Started Yet") lastWatchedPref else "Last Watched : $lastWatchedPref"

        pageLayout = findViewById(R.id.pageLayout)
        val buttonFavorite = findViewById<ImageButton>(R.id.button_favorite)

        // open DB
        var db = Room.databaseBuilder(
            applicationContext,
            LinksRoomDatabase::class.java, "fav-db"
        ).build()
        var linkDao = db.linkDao()

        CoroutineScope(Dispatchers.IO).launch {
            // get fav list
            val favList = linkDao.getLinks()
            db.close()
            withContext(Dispatchers.Main) {
                // check Fav
                var isFav = false
                for (fav in favList) {
                    if (fav.linkString == contentLink) {
                        isFav = true
                        break
                    }
                }

                if (isFav) {
                    inFav(buttonFavorite)
                } else {
                    notInFav(buttonFavorite)
                }

                // end of main thread
            }

            // end of io thread
        }

        /*
        btn on click ->
            open db
            check is fav
            if fav ->
                remove from fav
                set icon
            not fav
                add to fav
                set icon
            close db
        */

        buttonFavorite.setOnClickListener {
            // open DB
            db = Room.databaseBuilder(
                applicationContext,
                LinksRoomDatabase::class.java, "fav-db"
            ).build()
            linkDao = db.linkDao()

            CoroutineScope(Dispatchers.IO).launch {
                // get fav list
                val favList = linkDao.getLinks()
                withContext(Dispatchers.Main) {
                    // check Fav
                    var isFav = false
                    var foundFav = FavRoomModel("null", "null", "null")
                    for (fav in favList) {
                        if (fav.linkString == contentLink) {
                            isFav = true
                            foundFav = fav
                            break
                        }
                    }

                    if (isFav) {
                        inFav(buttonFavorite)
                        CoroutineScope(Dispatchers.IO).launch {
                            linkDao.deleteOne(foundFav)
                            withContext(Dispatchers.Main) {
                                notInFav(buttonFavorite)
                            }
                        }.start()

                    } else {
                        notInFav(buttonFavorite)
                        CoroutineScope(Dispatchers.IO).launch {
                            linkDao.insert(
                                FavRoomModel(
                                    contentLink.toString(),
                                    animeModel.animeCover,
                                    animeModel.animeName
                                )
                            )
                            withContext(Dispatchers.Main) {
                                inFav(buttonFavorite)
                            }
                        }.start()
                    }

                    // end of main thread
                }

                // end of io thread
            }
            db.close()
        }


        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        val backgroundImage = findViewById<ImageView>(R.id.backgroundImage)
        progressBar = findViewById(R.id.progressbarInPage)

        pageLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        Thread {

            val url = "https://yugen.to${contentLink}watch/?sort=episode"
            val doc = Jsoup.connect(url).get()
            val animeContent = doc.getElementsByClass("p-10-t")
            val animeEpContent = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                .select("div:nth-child(6)").select("span").text()
            val animeCover =
                doc.getElementsByClass("page-cover-inner").first()!!.getElementsByTag("img")
                    .attr("data-src")
            val animeName = animeContent.first()!!.text()
            val animDesc = animeContent[1].text()

            println(animeContent)
            println(animeEpContent)
            println(animeCover)
            println(animeName)
            println(animDesc)

            animeModel =
                AnimeDetails(animeName, animDesc, animeCover, animeEpContent)

            runOnUiThread {

                textView.text = animeModel.animeName
                textView2.text = animeModel.animeDesc
                // load background image.
                Picasso.get().load(animeModel.animeCover).error(R.drawable.ic_broken_image)
                    .into(backgroundImage)
                // load cover image.
                Picasso.get().load(animeModel.animeCover).error(R.drawable.ic_broken_image)
                    .into(coverImage)
                progressBar.visibility = View.GONE
                pageLayout.visibility = View.VISIBLE
                setupSpinner(animeModel.animeEpisodes, animeName)

            }
        }.start()
    }

    private fun setupSpinner(num: String, animeName: String) {

        val epList = arrayListOf<String>()

        for (i in num.toInt() downTo 1) {
            epList.add(i.toString())
        }
        pageLayout = findViewById(R.id.pageLayout)

        val textView2 = findViewById<TextView>(R.id.content_link_2)
        textView2.movementMethod = ScrollingMovementMethod()

        progressBar = findViewById(R.id.progressbarInPage)

        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val episodeButtonForSpinner = findViewById<ImageButton>(R.id.episodeButtonForSpinner)

        spinner.visibility = View.VISIBLE
        episodeButtonForSpinner.visibility = View.VISIBLE

        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, epList
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        episodeButtonForSpinner.setOnClickListener {
            pageLayout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            sharedPreferences.edit()
                .putString(contentLink, spinner.selectedItem.toString()).apply()

            sharedPreferences = getSharedPreferences("LastWatchedPref", MODE_PRIVATE)
            val lastWatchedText = findViewById<TextView>(R.id.lastWatched)
            val lastWatchedPref = sharedPreferences.getString(contentLink, "Not Started Yet")
            lastWatchedText.text =
                if (lastWatchedPref == "Not Started Yet") lastWatchedPref else "Last Watched : $lastWatchedPref"

            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to${watchLink}${spinner.selectedItem}"
            println(animeEpUrl)
            Thread {
                try {

                    val arrayLinks: ArrayList<String> = ArrayList()
                    val arrayLinksNames: ArrayList<String> = ArrayList()

                    var yugenEmbedLink = Jsoup.connect(animeEpUrl)
                        .get().getElementById("main-embed")!!.attr("src")
                    if (!yugenEmbedLink.contains("https:")) {
                        yugenEmbedLink = "https:$yugenEmbedLink"
                    }

                    println(yugenEmbedLink)

                    val mapOfHeaders = mutableMapOf(
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                        "Accept-Encoding" to "gzip, deflate",
                        "Accept-Language" to "en-US,en;q=0.5",
                        "Connection" to "keep-alive",
                        "Upgrade-Insecure-Requests" to "1",
                        "User-Agent" to "Mozilla/5.0 (X11; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0",
                        "Host" to "yugen.to",
                        "TE" to "Trailers",
                        "Origin" to "https://yugen.to",
                        "X-Requested-With" to "XMLHttpRequest",
                        "Referer" to yugenEmbedLink
                    )

                    val apiRequest = "https://yugen.to/api/embed/"

/*                    Fuel.get(yugenEmbedLink).header(mapOfHeaders)
                        .response { _, response, _ ->
                            val cookie = response.headers["Set-Cookie"].first()
                        }*/

                    val id = yugenEmbedLink.split("/")
                    val dataMap = mapOf("id" to id[id.size - 2], "ac" to "0")
                    println(dataMap)
                    Fuel.post(apiRequest, dataMap.toList()).header(mapOfHeaders)
                        .response { _, _, results ->
                            val (bytes, _) = results
                            if (bytes != null) {
                                val json = ObjectMapper().readTree(String(bytes))
                                println(json)
                                println(json.get("hls"))
                                val link = json.get("hls").asText()
//                                val link = json["multi"][0]["src"].asText()
//                                val linkName = json["multi"][0]["size"].asText()

                                arrayLinks.add(link)

                                arrayLinksNames.add(animeName)
                                arrayLinksNames.add("Episode ${spinner.selectedItem}")

                                println(arrayLinks)
                                println(arrayLinksNames)
                                runOnUiThread {
                                    Intent(this, PlayerActivity::class.java).apply {
                                        putExtra("nameOfLinks", arrayLinksNames)
                                        putExtra("theLinks", arrayLinks)
                                        startActivity(this)
                                        progressBar.visibility = View.GONE
                                        pageLayout.visibility = View.VISIBLE
                                    }

                                }
                            }
                        }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()


        }
    }

    override fun onResume() {
        super.onResume()
        if (pageLayout.visibility == View.VISIBLE) {
            progressBar.visibility = View.GONE
        }
    }


    private fun inFav(buttonFavorite: ImageButton) {
        println("In Fav")
        buttonFavorite.setImageResource(R.drawable.ic_heart_minus)
    }

    private fun notInFav(buttonFavorite: ImageButton) {
        println("Not in Fav")
        buttonFavorite.setImageResource(R.drawable.ic_heart_plus)

    }

}