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
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.preference.PreferenceManager
import coil.load
import com.talent.animescrap.R
import com.talent.animescrap.databinding.FragmentAnimeBinding
import com.talent.animescrap.model.AnimeDetails
import com.talent.animescrap.model.AnimeStreamLink
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
    private lateinit var selectedSource: String
    private lateinit var settingsPreferenceManager: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnimeBinding.inflate(inflater, container, false)

        settingsPreferenceManager = PreferenceManager.getDefaultSharedPreferences(requireContext())
        selectedSource = settingsPreferenceManager.getString("source", "yugen")!!

        animeStreamViewModel.animeStreamLink.observe(viewLifecycleOwner) {
            binding.progressbarInPage.visibility = View.GONE
            binding.pageLayout.visibility = View.VISIBLE
            if (it.link.isNotBlank()) animeName?.let { name -> startExternalPlayer(it, name) }
            else Toast.makeText(requireContext(), "No Streaming Url Found", Toast.LENGTH_SHORT)
                .show()
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
        contentLink?.let { animeDetailsViewModel.checkFavorite(it, selectedSource) }
        animeDetailsViewModel.isAnimeFav.observe(viewLifecycleOwner) { isFav ->
            if (isFav) {
                inFav()
                binding.favCard.setOnClickListener {
                    animeDetailsViewModel.removeFav(contentLink!!, selectedSource)
                }
            } else {
                notInFav()
                binding.favCard.setOnClickListener {
                    animeDetailsViewModel.addToFav(
                        contentLink!!,
                        animeDetails.animeName,
                        animeDetails.animeCover,
                        selectedSource
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
                    else "Last Watched : $lastWatchedPrefString/${animeDetails.animeEpisodes.size}"

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
                setupSpinner(animeDetails.animeEpisodes)
            } else {
                binding.errorCard?.visibility = View.VISIBLE
            }
        }

        println(contentLink)
        contentLink?.let { animeDetailsViewModel.getAnimeDetails(it) }


        return binding.root
    }

    private fun setupSpinner(animeEpisodes: Map<String, String>) {

        val epList = animeEpisodes.keys.toList().reversed()
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
                    if (this == "Not Started Yet") this else "Last Watched : $this/${animeEpisodes.size}"
            }

            binding.progressbarInPage.visibility = View.VISIBLE
            binding.pageLayout.visibility = View.GONE

            // Navigate to Internal Player
            val isExternalPlayerEnabled =
                settingsPreferenceManager.getBoolean("external_player", false)
            if (!isExternalPlayerEnabled) {
                val navController =
                    requireActivity().findNavController(R.id.nav_host_fragment_activity_main_bottom_nav)
                val action = AnimeFragmentDirections.actionNavigationAnimeToNavigationPlayer(
                    animeName!!,
                    animeEpisodes[binding.episodeSpinner.selectedItem]!!,
                    animeEpisodes.size.toString(),
                    contentLink!!
                )
                navController.navigate(action)
            } else {
                animeStreamViewModel.setAnimeLink(
                    contentLink!!,
                    animeEpisodes[binding.episodeSpinner.selectedItem]!!
                )
            }

        }


    }

    private fun startExternalPlayer(
        animeStreamLink: AnimeStreamLink,
        animeName: String,
    ) {
        val title = "$animeName Episode ${binding.episodeSpinner.selectedItem}"

        val customIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(animeStreamLink.link), "video/*")
            if (!animeStreamLink.extraHeaders.isNullOrEmpty())
                putExtra("headers", animeStreamLink.extraHeaders.toString())
            if (animeStreamLink.subsLink.isNotBlank())
                putExtra("subs", Uri.parse(animeStreamLink.subsLink))
            putExtra("title", title)
        }
        val isMX =
            settingsPreferenceManager.getBoolean("mx_player", false)

        if (isMX) {
            startMX(customIntent)
        } else {
            startActivity(Intent.createChooser(customIntent, "Play using"))
        }


    }

    private fun startMX(
        customIntent: Intent
    ) {
        try {
            customIntent.apply {
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
                    customIntent.apply {
                        setPackage("com.mxtech.videoplayer.ad")
                        startActivity(this)
                    }
                }
            } catch (e: ActivityNotFoundException) {
                Log.i(
                    R.string.app_name.toString(),
                    "No version of MX Player is installed, falling back to other external player"
                )
                startActivity(Intent.createChooser(customIntent, "Play using"))
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