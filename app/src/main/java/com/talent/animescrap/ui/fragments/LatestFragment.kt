package com.talent.animescrap.ui.fragments

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentLatestBinding
import com.talent.animescrap.ui.viewmodels.LatestViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LatestFragment : Fragment() {

    private var _binding: FragmentLatestBinding? = null
    private val latestViewModel: LatestViewModel by viewModels()
    private var rvAdapter = RecyclerAdapter()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLatestBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.VISIBLE
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 4)
        } else {
            binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        }
        binding.recyclerView.adapter = rvAdapter
        binding.recyclerView.setHasFixedSize(true)

        latestViewModel.latestAnimeList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            if (it.isNotEmpty()) {
                binding.errorCard.visibility = View.GONE
            } else {
                binding.errorCard.visibility = View.VISIBLE
            }

            if (binding.swipeContainer.isRefreshing) {
                rvAdapter.submitList(it) {
                    binding.recyclerView.smoothScrollToPosition(0)
                }
                binding.swipeContainer.isRefreshing = false
            } else {
                rvAdapter.submitList(it)
            }
        }

        binding.swipeContainer.setOnRefreshListener { latestViewModel.getLatestAnimeList() }

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