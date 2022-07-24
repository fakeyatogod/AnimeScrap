package com.talent.animescrap.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.room.Room
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.squareup.picasso.Picasso
import com.talent.animescrap.R
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.room.FavRoomModel
import com.talent.animescrap.room.LinksRoomDatabase
import com.talent.animescrap.ui.activities.PlayerActivity
import com.talent.animescrap.ui.viewmodels.AnimeDetailsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class AnimeFragment : Fragment() {


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

    private val args: AnimeFragmentArgs by navArgs()
    private val animeDetailsViewModel: AnimeDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_anime, container, false)

        animeNameTxt = view.findViewById(R.id.anime_name_txt)
        animeDetailsTxt = view.findViewById(R.id.anime_details_txt)
        coverImage = view.findViewById(R.id.coverAnime)
        backgroundImage = view.findViewById(R.id.backgroundImage)
        progressBar = view.findViewById(R.id.progressbarInPage)
        lastWatchedText = view.findViewById(R.id.last_watched_txt)
        pageLayout = view.findViewById(R.id.pageLayout)
        buttonFavorite = view.findViewById(R.id.button_favorite)
        spinner = view.findViewById(R.id.episodeSpinner)
        playAnimeButton = view.findViewById(R.id.episodeButtonForSpinner)

        animeDetailsTxt.movementMethod = ScrollingMovementMethod()

        contentLink = args.animeLink

        if (contentLink == "null") {
            activity?.onBackPressed()
            Toast.makeText(activity, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences =
            activity!!.getSharedPreferences("LastWatchedPref", AppCompatActivity.MODE_PRIVATE)
        lastWatchedPrefString =
            sharedPreferences.getString(contentLink, "Not Started Yet").toString()
        lastWatchedText.text =
            if (lastWatchedPrefString == "Not Started Yet") lastWatchedPrefString else "Last Watched : $lastWatchedPrefString"

        // Check Favorite
        handleFavorite()

        pageLayout.visibility = View.GONE
        progressBar.visibility = View.VISIBLE

        animeDetailsViewModel.animeDetails.observe(viewLifecycleOwner) {
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

        return view
    }

    private fun setupSpinner(num: String, animeName: String) {

        val epList = (num.toInt() downTo 1).map { it.toString() }
        val arrayAdapter =
            ArrayAdapter(activity as Context, android.R.layout.simple_spinner_item, epList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        playAnimeButton.setOnClickListener {

            // Store Last Watched Episode
            sharedPreferences.edit()
                .putString(contentLink, spinner.selectedItem.toString()).apply()

            // Update to new value
            sharedPreferences = activity!!.getSharedPreferences(
                "LastWatchedPref",
                AppCompatActivity.MODE_PRIVATE
            )
            sharedPreferences.getString(contentLink, "Not Started Yet").apply {
                lastWatchedText.text =
                    if (this == "Not Started Yet") this else "Last Watched : $this"
            }

            // Get the link of episode
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to${watchLink}${spinner.selectedItem}"
            println(animeEpUrl)

            Intent(activity, PlayerActivity::class.java).apply {
                putExtra("anime_name", animeName)
                putExtra("anime_episode", "Episode ${spinner.selectedItem}")
                putExtra("anime_url", animeEpUrl)
                startActivity(this)
            }

        }


    }

    private fun handleFavorite() {
        // open DB
        var db = Room.databaseBuilder(
            activity as Context,
            LinksRoomDatabase::class.java, "fav-db"
        ).build()
        var linkDao = db.linkDao()

        CoroutineScope(Dispatchers.IO).launch {
            // get fav list
            val favList = linkDao.getLinks()
            db.close()
            withContext(Dispatchers.Main) {
                // check Fav
                val isFav = favList.any { it.linkString == contentLink }
                if (isFav)
                    inFav(buttonFavorite)
                else
                    notInFav(buttonFavorite)
            }
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
                activity as Context,
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