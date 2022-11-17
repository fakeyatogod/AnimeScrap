package com.talent.animescrap.ui.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.AnimeRecyclerAdapter
import com.talent.animescrap.databinding.FragmentTrendingBinding
import com.talent.animescrap.ui.viewmodels.TrendingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private val trendingViewModel: TrendingViewModel by viewModels()
    private val selectedSource by lazy {   PreferenceManager
        .getDefaultSharedPreferences(requireContext())
        .getString("source", "yugen")}
    private val rvAdapter by lazy {
        AnimeRecyclerAdapter(if(selectedSource == "animepahe" || selectedSource == "kiss_kh") "landscape card" else "portrait card")
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrendingBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.VISIBLE
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 4)
        } else {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        }
        binding.recyclerView.adapter = rvAdapter
        binding.recyclerView.setHasFixedSize(true)

        trendingViewModel.trendingAnimeList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            if (it.isNotEmpty()) {
                binding.errorCard.visibility = View.GONE
            } else {
                binding.errorCard.visibility = View.VISIBLE
            }

            rvAdapter.submitList(it)

            if (binding.swipeContainer.isRefreshing) {
                binding.swipeContainer.isRefreshing = false
            }
        }

        binding.swipeContainer.setOnRefreshListener { trendingViewModel.getTrendingAnimeList() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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