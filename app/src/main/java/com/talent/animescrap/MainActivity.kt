package com.talent.animescrap

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.talent.animescrap.adapter.RecyclerAdapter
import com.talent.animescrap.model.Photos
import org.jsoup.Jsoup

class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<ArrayList<Photos>>  {
    private lateinit var recyclerView: RecyclerView
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportLoaderManager.initLoader(56,null,this)
    }

    class AsyncScrap(context: Context) : AsyncTaskLoader<ArrayList<Photos>>(context) {

        override fun loadInBackground(): ArrayList<Photos> {
            var picInfo = arrayListOf<Photos>()
            val url = "https://yugenani.me"

            val doc = Jsoup.connect(url).get()
            val allInfo = doc.getElementsByClass("ep-card")
//            println(allInfo)
            for (item in allInfo) {
                val itemImage = item.getElementsByTag("img").attr("src")
                val itemName = item.getElementsByClass("ep-details").attr("alt")
                val itemLink = item.getElementsByClass("ep-details").attr("href")
                val picObject = Photos(itemName, itemImage, itemLink)
                picInfo.add(picObject)
            }
            return picInfo
        }

        override fun onStartLoading() {
            forceLoad() // Starts the loadInBackground method
        }

    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<ArrayList<Photos>> {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        println("Hi Loader")

        return AsyncScrap(this)
    }

    override fun onLoadFinished(loader: Loader<ArrayList<Photos>>, data: ArrayList<Photos>) {
        recyclerView.adapter = RecyclerAdapter(applicationContext, data)
        recyclerView.setHasFixedSize(true)
    }

    override fun onLoaderReset(loader: Loader<ArrayList<Photos>>) {
        TODO("Not yet implemented")
    }

}