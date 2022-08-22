package com.talent.animescrap.ui.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import coil.load
import com.talent.animescrap.R
import com.talent.animescrap.databinding.FragmentAnimeBinding
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.ui.activities.PlayerActivity
import com.talent.animescrap.ui.viewmodels.AnimeDetailsViewModel
import com.talent.animescrap.ui.viewmodels.AnimeStreamViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnimeFragment : Fragment() {

    private val animeStreamViewModel by viewModels<AnimeStreamViewModel>()
    private var _binding: FragmentAnimeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var contentLink: String? = "null"
    private var animeName: String? = null
    private lateinit var animeDetails: AnimeDetails
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var lastWatchedPrefString: String

    private val args: AnimeFragmentArgs by navArgs()
    private val animeDetailsViewModel: AnimeDetailsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)

        animeStreamViewModel.animeStreamLink.observe(viewLifecycleOwner) {
            binding.progressbarInPage.visibility = View.GONE
            binding.pageLayout.visibility = View.VISIBLE
            println("ob = $it")
            animeName?.let { name -> startPlayer(it, name) }
        }

        binding.animeDetailsTxt.movementMethod = ScrollingMovementMethod()

        contentLink = args.animeLink

        if (contentLink == "null") {
            findNavController().popBackStack()
            Toast.makeText(activity, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }

        sharedPreferences =
            requireActivity().getSharedPreferences(
                "LastWatchedPref",
                AppCompatActivity.MODE_PRIVATE
            )
        lastWatchedPrefString =
            sharedPreferences.getString(contentLink, "Not Started Yet").toString()

        // Check Favorite
        contentLink?.let { animeDetailsViewModel.checkFavorite(it) }
        animeDetailsViewModel.isAnimeFav.observe(viewLifecycleOwner) { isFav ->
            if (isFav) {
                inFav()
                binding.favCard.setOnClickListener {
                    animeDetailsViewModel.removeFav(contentLink!!)
                }
            } else {
                notInFav()
                binding.favCard.setOnClickListener {
                    animeDetailsViewModel.addToFav(
                        contentLink!!,
                        animeDetails.animeName,
                        animeDetails.animeCover
                    )
                }
            }
        }


        binding.pageLayout.visibility = View.GONE
        binding.progressbarInPage.visibility = View.VISIBLE

        animeDetailsViewModel.animeDetails.observe(viewLifecycleOwner) {
            binding.progressbarInPage.visibility = View.GONE
            if (it != null) {
                animeDetails = it
                binding.animeNameTxt.text = animeDetails.animeName
                binding.animeDetailsTxt.text = animeDetails.animeDesc

                binding.lastWatchedTxt.text =
                    if (lastWatchedPrefString == "Not Started Yet") lastWatchedPrefString
                    else "Last Watched : $lastWatchedPrefString/${animeDetails.animeEpisodes}"

                // load background image.
                binding.backgroundImage.load(animeDetails.animeCover) {
                    error(R.drawable.ic_broken_image)
                }
                // load cover image.
                binding.coverAnime.load(animeDetails.animeCover) {
                    error(R.drawable.ic_broken_image)
                }
                binding.errorCard?.visibility = View.GONE
                binding.pageLayout.visibility = View.VISIBLE

                animeName = animeDetails.animeName
                setupSpinner(animeDetails.animeEpisodes, animeDetails.animeEpisodes)
            } else {
                binding.errorCard?.visibility = View.VISIBLE
            }
        }


        contentLink?.let { animeDetailsViewModel.getAnimeDetails(it) }


        return binding.root
    }

    private fun setupSpinner(num: String, animeEpisodes: String) {

        val epList = (num.toInt() downTo 1).map { it.toString() }
        val arrayAdapter =
            ArrayAdapter(activity as Context, android.R.layout.simple_spinner_item, epList)
        arrayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.episodeSpinner.adapter = arrayAdapter

        // Remember Last watched in binding.episodeSpinner
        if (lastWatchedPrefString in epList)
            binding.episodeSpinner.setSelection(epList.indexOf(lastWatchedPrefString))

        binding.epCard.setOnClickListener { binding.episodeSpinner.performClick() }
        binding.playCard.setOnClickListener {

            // Store Last Watched Episode
            sharedPreferences.edit()
                .putString(contentLink, binding.episodeSpinner.selectedItem.toString()).apply()

            // Update to new value
            sharedPreferences = requireActivity().getSharedPreferences(
                "LastWatchedPref",
                AppCompatActivity.MODE_PRIVATE
            )
            sharedPreferences.getString(contentLink, "Not Started Yet").apply {
                binding.lastWatchedTxt.text =
                    if (this == "Not Started Yet") this else "Last Watched : $this/$animeEpisodes"
            }

            // Get the link of episode
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugen.to${watchLink}${binding.episodeSpinner.selectedItem}"
            println(animeEpUrl)

            binding.progressbarInPage.visibility = View.VISIBLE
            binding.pageLayout.visibility = View.GONE
            animeStreamViewModel.setAnimeLink(animeEpUrl)

        }


    }

    private fun startPlayer(
        link: String,
        animeName: String,
        animeEp: String = "Episode ${binding.episodeSpinner.selectedItem}"
    ) {

        val settingsPreferenceManager =
            PreferenceManager.getDefaultSharedPreferences(activity as Context)
        val isExternalPlayerEnabled =
            settingsPreferenceManager.getBoolean("external_player", false)
        val isMX =
            settingsPreferenceManager.getBoolean("mx_player", false)
        if (isExternalPlayerEnabled) {
            if (isMX) {
                startMX(link)
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(link), "video/*")
                    startActivity(Intent.createChooser(this, "Play using"))
                }
            }
        } else {
            Intent(activity, PlayerActivity::class.java).apply {
                putExtra("anime_name", animeName)
                putExtra("anime_episode", animeEp)
                putExtra("anime_url", link)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(this)
            }
        }

    }

    private fun startMX(animeStreamUrl: String) {
        try {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(animeStreamUrl), "application/x-mpegURL")
                setPackage("com.mxtech.videoplayer.pro")
                startActivity(this)
            }
        } catch (e: ActivityNotFoundException) {
            Log.i(
                R.string.app_name.toString(),
                "MX Player pro isn't installed, falling back to MX player Ads"
            )
            try {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(animeStreamUrl), "application/x-mpegURL")
                    setPackage("com.mxtech.videoplayer.ad")
                    startActivity(this)
                }
            } catch (e: ActivityNotFoundException) {
                Log.i(
                    R.string.app_name.toString(),
                    "No version of MX Player is installed, falling back to other external player"
                )
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(animeStreamUrl), "video/*")
                    startActivity(Intent.createChooser(this, "Play using"))
                }
            }
        }
    }


    private fun inFav() {
        println("In Fav")
        binding.buttonFavorite.setImageResource(R.drawable.ic_heart_minus)
    }

    private fun notInFav() {
        println("Not in Fav")
        binding.buttonFavorite.setImageResource(R.drawable.ic_heart_plus)

    }

}