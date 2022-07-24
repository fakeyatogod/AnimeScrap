package com.talent.animescrap.ui.activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Picasso
import com.talent.animescrap.R
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.room.FavRoomModel
import com.talent.animescrap.room.LinksRoomDatabase
import com.talent.animescrap.ui.viewmodels.AnimeDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PageActivity : AppCompatActivity() {

    private var contentLink: String? = "null"
    private lateinit var pageLayout: LinearLayout
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var animeModel: AnimeDetails
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var animeNameTxt: TextView
    private lateinit var animeDetailsTxt: TextView
    private lateinit var lastWatchedText: TextView
    private lateinit var coverImage: ImageView
    private lateinit var backgroundImage: ImageView
    private lateinit var lastWatchedPrefString: String
    private lateinit var buttonFavorite: ImageButton
    private lateinit var spinner: Spinner
    private lateinit var playAnimeButton: ImageButton

    private val animeDetailsViewModel: AnimeDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        supportActionBar?.title = "Anime Details"

        animeNameTxt = findViewById(R.id.anime_name_txt)
        animeDetailsTxt = findViewById(R.id.anime_details_txt)
        coverImage = findViewById(R.id.coverAnime)
        backgroundImage = findViewById(R.id.backgroundImage)
        progressBar = findViewById(R.id.progressbarInPage)
        lastWatchedText = findViewById(R.id.last_watched_txt)
        pageLayout = findViewById(R.id.pageLayout)
        buttonFavorite = findViewById(R.id.button_favorite)
        spinner = findViewById(R.id.episodeSpinner)
        playAnimeButton = findViewById(R.id.episodeButtonForSpinner)

        animeDetailsTxt.movementMethod = ScrollingMovementMethod()


        if (intent == null || intent.getStringExtra("content_link") == null) {
            finish()
            Toast.makeText(this, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        } else {
            contentLink = intent.getStringExtra("content_link")
        }

        sharedPreferences = getSharedPreferences("LastWatchedPref", MODE_PRIVATE)
        lastWatchedPrefString =
            sharedPreferences.getString(contentLink, "Not Started Yet").toString()
        lastWatchedText.text =
            if (lastWatchedPrefString == "Not Started Yet") lastWatchedPrefString else "Last Watched : $lastWatchedPrefString"

        // Check Favorite
        handleFavorite()

        pageLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        animeDetailsViewModel.animeDetails.observe(this@PageActivity) {
            animeModel = it
            animeNameTxt.text = animeModel.animeName
            animeDetailsTxt.text = animeModel.animeDesc
            // load background image.
            Picasso.get().load(animeModel.animeCover).error(R.drawable.ic_broken_image)
                .into(backgroundImage)
            // load cover image.
            Picasso.get().load(animeModel.animeCover).error(R.drawable.ic_broken_image)
                .into(coverImage)
            progressBar.visibility = View.GONE
            pageLayout.visibility = View.VISIBLE
            setupSpinner(animeModel.animeEpisodes, animeModel.animeName)
        }

        CoroutineScope(Dispatchers.IO).launch {
            contentLink?.let { animeDetailsViewModel.getAnimeDetails(it) }
        }
    }


    private fun setupSpinner(num: String, animeName: String) {

        val epList = (num.toInt() downTo 1).map { it.toString() }
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, epList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        playAnimeButton.setOnClickListener {
            // Show progressbar
            pageLayout.visibility = View.GONE
            progressBar.visibility = View.VISIBLE

            // Store Last Watched Episode
            sharedPreferences.edit()
                .putString(contentLink, spinner.selectedItem.toString()).apply()
            sharedPreferences = getSharedPreferences("LastWatchedPref", MODE_PRIVATE)
            val lastWatchedText = findViewById<TextView>(R.id.last_watched_txt)
            val lastWatchedPref = sharedPreferences.getString(contentLink, "Not Started Yet")
            lastWatchedText.text =
                if (lastWatchedPref == "Not Started Yet") lastWatchedPref else "Last Watched : $lastWatchedPref"

            // Get the link of episode
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to${watchLink}${spinner.selectedItem}"
            println(animeEpUrl)

            // Observe anime link
            animeDetailsViewModel.animeStreamLink.observe(this@PageActivity) {
                Intent(this, PlayerActivity::class.java).apply {
                    putExtra(
                        "nameOfLinks",
                        arrayListOf(animeName, "Episode ${spinner.selectedItem}")
                    )
                    putExtra("theLinks", arrayListOf(it))
                    startActivity(this)
                    progressBar.visibility = View.GONE
                    pageLayout.visibility = View.VISIBLE
                }
            }

            // Get the anime link
            CoroutineScope(Dispatchers.IO).launch {
                animeDetailsViewModel.getStreamLink(animeEpUrl)
            }

        }


    }

    override fun onResume() {
        super.onResume()
        if (pageLayout.visibility == View.VISIBLE) {
            progressBar.visibility = View.GONE
        }
    }


    private fun handleFavorite() {
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