package com.talent.animescrap.ui.latest

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.talent.animescrap.R
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.databinding.FragmentLatestBinding
import com.talent.animescrap.model.Photos
import org.jsoup.Jsoup

class LatestFragment : Fragment() {

    private lateinit var latestViewModel: LatestViewModel
    private var _binding: FragmentLatestBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        latestViewModel = ViewModelProvider(this).get(LatestViewModel::class.java)

        _binding = FragmentLatestBinding.inflate(inflater, container, false)

        val root: View = binding.root
        val textView: TextView = binding.textLatest

        latestViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })

        _binding!!.progressbarInMain.visibility = View.VISIBLE
        _binding!!.recyclerView.layoutManager = GridLayoutManager(activity as Context, 2)

        Thread {

            val picInfo = arrayListOf<Photos>()
            val url = "https://yugenani.me/latest/"

            val doc = Jsoup.connect(url).get()
            val allInfo = doc.getElementsByClass("ep-card")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("ep-details").attr("alt")
                val itemLink = item.getElementsByClass("ep-details").attr("href")
                val picObject = Photos(itemName, itemImage, itemLink)
                picInfo.add(picObject)
            }

            activity?.runOnUiThread {
                _binding!!.progressbarInMain.visibility = View.GONE
                _binding!!.recyclerView.adapter = RecyclerAdapter(activity as Context, picInfo)
                _binding!!.recyclerView.setHasFixedSize(true)
            }
        }.start()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}