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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel by viewModels<SearchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        val inputEditText = binding.textInputEditText
        val recyclerView = binding.recyclerView
        binding.progressbarInMain.visibility = View.GONE

        recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
        inputEditText.addTextChangedListener {
            val newText2 = it.toString().lowercase(Locale.ENGLISH)

            if (newText2.length >= 3) {
                binding.progressbarInMain.visibility = View.VISIBLE
                recyclerView.visibility = View.VISIBLE
                recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)
                val searchUrl = "https://yugen.to/search/?q=${newText2.replace(" ", "+")}"
                println(searchUrl)
                CoroutineScope(Dispatchers.IO).launch {
                    searchViewModel.searchAnime(searchUrl)
                    withContext(Dispatchers.Main) {
                        searchViewModel.animeLatestList.observe(viewLifecycleOwner) { animeList ->
                            binding.progressbarInMain.visibility = View.GONE
                            binding.recyclerView.adapter =
                                RecyclerAdapter(activity as Context, animeList)
                            binding.recyclerView.setHasFixedSize(true)
                        }
                    }

                }
            }
        }

        return binding.root
    }

}