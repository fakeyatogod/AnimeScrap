package com.talent.animescrap.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentLatestBinding
import com.talent.animescrap.ui.viewmodels.LatestViewModel

class LatestFragment : Fragment() {

    private var _binding: FragmentLatestBinding? = null
    private lateinit var latestViewModel: LatestViewModel
    private var rvAdapter = RecyclerAdapter()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        latestViewModel = ViewModelProvider(this)[LatestViewModel::class.java]

        _binding = FragmentLatestBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.VISIBLE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        binding.recyclerView.adapter = rvAdapter
        latestViewModel.latestAnimeList.observe(viewLifecycleOwner) {
            binding.progressbarInMain.visibility = View.GONE
            binding.recyclerView.setHasFixedSize(true)
            if (binding.swipeContainer.isRefreshing) {
                binding.swipeContainer.isRefreshing = false
            }
            rvAdapter.submitList(it)
        }

        binding.swipeContainer.setOnRefreshListener { latestViewModel.getLatestAnimeList() }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}