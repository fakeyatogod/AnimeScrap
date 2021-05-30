package com.talent.animescrap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import com.talent.animescrap.model.AnimeDetails
import org.jsoup.Jsoup


class PageActivity : AppCompatActivity() {

    private var contentLink: String? = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page)

        if (intent != null) {
            contentLink = intent.getStringExtra("content_link")
            if (contentLink == "null") {
                finish()
                Toast.makeText(this, "Some Unexpected error occurred", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            finish()
            Toast.makeText(this, "Some Unexpected error occurred", Toast.LENGTH_SHORT).show()
        }
        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        val progressBar = findViewById<ProgressBar>(R.id.progressbarInPage)
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val episodeButtonForSpinner = findViewById<Button>(R.id.episodeButtonForSpinner)

        textView.visibility = View.GONE
        textView2.visibility = View.GONE
        coverImage.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        spinner.visibility = View.GONE
        episodeButtonForSpinner.visibility = View.GONE

        Thread {

            val url = "https://yugenani.me${contentLink}watch/?sort=episode"
            val doc = Jsoup.connect(url).get()
            val animeContent = doc.getElementsByClass("p-10-t")
            val animeEpContent = doc.getElementsByClass("box p-10 p-15 m-15-b anime-metadetails")
                .select("div:nth-child(6)").select("span").text()
            val animeCover = doc.getElementsByClass("cover").attr("src")
            val animeDetails = arrayListOf<String>()
            for (element in animeContent) {
                animeDetails.add(element.text())
            }

            val animeModel =
                AnimeDetails(animeDetails[0], animeDetails[1], animeCover, animeEpContent)

            runOnUiThread {

                textView.text = animeModel.animeName
                textView2.text = animeModel.animeDesc
                Picasso.get().load(animeModel.animeCover).error(R.drawable.ic_broken_image)
                    .placeholder(R.drawable.pgi2).into(coverImage)
                progressBar.visibility = View.GONE
                textView.visibility = View.VISIBLE
                textView2.visibility = View.VISIBLE
                coverImage.visibility = View.VISIBLE
                setupSpinner(animeModel.animeEpisodes)

            }
        }.start()
    }

    private fun setupSpinner(num: String) {

        val epList = arrayListOf<String>()

        for (i in num.toInt() downTo 1) {
            epList.add(i.toString())
        }
        val textView = findViewById<TextView>(R.id.content_link)
        val textView2 = findViewById<TextView>(R.id.content_link_2)
        val coverImage = findViewById<ImageView>(R.id.coverAnime)
        val progressBar = findViewById<ProgressBar>(R.id.progressbarInPage)
        val spinner = findViewById<Spinner>(R.id.episodeSpinner)
        val episodeButtonForSpinner = findViewById<Button>(R.id.episodeButtonForSpinner)
        spinner.visibility = View.VISIBLE
        episodeButtonForSpinner.visibility = View.VISIBLE
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                Toast.makeText(parent.context, parent.selectedItem.toString(), Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val arrayAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item, epList
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = arrayAdapter

        episodeButtonForSpinner.setOnClickListener {
            textView.visibility = View.GONE
            textView2.visibility = View.GONE
            coverImage.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            episodeButtonForSpinner.visibility = View.GONE
            spinner.visibility = View.GONE
            var watchLink = contentLink
            watchLink = watchLink?.replace("anime", "watch")
            val animeEpUrl = "https://yugenani.me${watchLink}${spinner.selectedItem}"

            Thread {
                try {

                    val arrayLinks: ArrayList<String> = ArrayList()
                    val arrayLinksNames: ArrayList<String> = ArrayList()

                    var streamAniLink = "Null"
                    var tries = 1
                    do {
                        streamAniLink = Jsoup.connect(animeEpUrl)
                            .get().getElementsByClass("anime-download").attr("href")
                        println("Try $tries")
                        tries += 1
                    } while (streamAniLink == "Null")
                    
                    val goGoStreamLink = streamAniLink.replaceBefore(
                        "?id=",
                        "https://gogo-stream.com/download"
                    )
                    println(streamAniLink)
                    println(goGoStreamLink)

                    val gogoLink = Jsoup.connect(goGoStreamLink).get()
                    val downloadLinks = gogoLink.getElementsByClass("dowload")

                    println(gogoLink)
                    println(downloadLinks)

                    for (i in downloadLinks) {
                        arrayLinks.add(
                            i.getElementsByTag("a").text().toString()
                                .replace("Download", "")
                        )
                        arrayLinksNames.add(i.getElementsByTag("a").attr("href").toString())

                    }

                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        textView.visibility = View.VISIBLE
                        textView2.visibility = View.VISIBLE
                        coverImage.visibility = View.VISIBLE
                        episodeButtonForSpinner.visibility = View.VISIBLE
                        spinner.visibility = View.VISIBLE

                        val intent = Intent(this, LinksActivity::class.java)
                        intent.putExtra("nameOfLinks", arrayLinks)
                        intent.putExtra("theLinks", arrayLinksNames)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()


        }
    }


}