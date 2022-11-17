package com.talent.animescrap.ui.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.AnimeRecyclerAdapter
import com.talent.animescrap.databinding.FragmentSearchBinding
import com.talent.animescrap.ui.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel by viewModels<SearchViewModel>()
    private val selectedSource by lazy {   PreferenceManager
        .getDefaultSharedPreferences(requireContext())
        .getString("source", "yugen")}
    private val rvAdapter by lazy {
        AnimeRecyclerAdapter(if(selectedSource == "kiss_kh") "landscape card" else "portrait card")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.GONE
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 4)
        } else {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        }
        binding.recyclerView.adapter = rvAdapter

        searchViewModel.searchedAnimeList.observe(viewLifecycleOwner) { animeList ->
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.setHasFixedSize(true)

            rvAdapter.submitList(animeList) {
                if (animeList.isNotEmpty())
                    binding.recyclerView.scrollToPosition(0)
            }
        }

        binding.textInputEditText.addTextChangedListener {
            val searchedText =
                it.toString().lowercase(Locale.ENGLISH).replace("[^A-Za-z0-9]".toRegex(), " ")
                    .trim().replace("\\s+".toRegex(), " ").replace(" ", "+")

            if (searchedText.length >= 3) {
                binding.progressbarInMain.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE
                searchViewModel.searchAnime(searchedText)
            }
        }

        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (activity != null) {
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
            } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 4)
            }
        }
    }
}