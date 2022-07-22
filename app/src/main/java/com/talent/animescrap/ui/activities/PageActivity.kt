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
    private val animeDetailsViewModel: AnimeDetailsViewModel by viewModels()

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

        handleFavorite(buttonFavorite)

        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        val backgroundImage = findViewById<ImageView>(R.id.backgroundImage)
        progressBar = findViewById(R.id.progressbarInPage)

        pageLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        animeDetailsViewModel.animeDetails.observe(this@PageActivity) {
            animeModel = it
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
            setupSpinner(animeModel.animeEpisodes, animeModel.animeName)
        }

        CoroutineScope(Dispatchers.IO).launch {
            contentLink?.let { animeDetailsViewModel.getAnimeDetails(it) }
        }
    }


    private fun setupSpinner(num: String, animeName: String) {

        pageLayout = findViewById(R.id.pageLayout)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        textView2.movementMethod = ScrollingMovementMethod()
        progressBar = findViewById(R.id.progressbarInPage)
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val playAnimeButton = findViewById<ImageButton>(R.id.episodeButtonForSpinner)

        val epList = arrayListOf<String>()

        for (i in num.toInt() downTo 1) {
            epList.add(i.toString())
        }

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
            val lastWatchedText = findViewById<TextView>(R.id.lastWatched)
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


    private fun handleFavorite(buttonFavorite: ImageButton) {
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