package com.talent.animescrap.ui.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentSearchBinding
import com.talent.animescrap.ui.viewmodels.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel by viewModels<SearchViewModel>()
    private val rvAdapter = RecyclerAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        binding.progressbarInMain.visibility = View.GONE
        binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        binding.recyclerView.adapter = rvAdapter

        searchViewModel.searchedAnimeList.observe(viewLifecycleOwner) { animeList ->
            binding.progressbarInMain.visibility = View.GONE

            rvAdapter.submitList(animeList)

            binding.recyclerView.setHasFixedSize(true)
        }

        binding.textInputEditText.addTextChangedListener {
            val newText2 = it.toString().lowercase(Locale.ENGLISH)

            if (newText2.length >= 3) {
                binding.progressbarInMain.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE
                binding.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
                val searchUrl = "https://yugen.to/search/?q=${newText2.replace(" ", "+")}"
                println(searchUrl)
                searchViewModel.searchAnime(searchUrl)
            }
        }

        return binding.root
    }

}