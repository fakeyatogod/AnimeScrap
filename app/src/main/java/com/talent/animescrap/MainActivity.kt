package com.talent.animescrap

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.model.Photos
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val progressBar = findViewById<ProgressBar>(R.id.progressbarInMain)
        progressBar.visibility = View.VISIBLE
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        Thread {

            val picInfo = arrayListOf<Photos>()
            val url = "https://yugenani.me"

            val doc = Jsoup.connect(url).get()
            val allInfo = doc.getElementsByClass("ep-card")
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("ep-details").attr("alt")
                val itemLink = item.getElementsByClass("ep-details").attr("href")
                val picObject = Photos(itemName, itemImage, itemLink)
                picInfo.add(picObject)
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                recyclerView.adapter = RecyclerAdapter(applicationContext, picInfo)
                recyclerView.setHasFixedSize(true)
            }
        }.start()
    }

}