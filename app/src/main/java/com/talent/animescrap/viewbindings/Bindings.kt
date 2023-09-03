package com.talent.animescrap.viewbindings

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import coil.ImageLoader
import coil.load
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.talent.animescrap.R
import com.talent.animescrap.utils.Utils

private val shimmer =
    Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
        .setDuration(1200) // how long the shimmering animation takes to do one full sweep
        .setBaseAlpha(0.6f) //the alpha of the underlying children
        .setHighlightAlpha(0.9f) // the shimmer alpha amount
        .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
        .setAutoStart(true)
        .build()
/* A function that is called when the `image` attribute is used in the layout. */
@BindingAdapter("image")
fun ImageView.setImage(url: String?) {
    val imageLoader = ImageLoader.Builder(context)
        .okHttpClient { Utils.httpClient }
        .build()
    if (!url.isNullOrEmpty())
        load(url, imageLoader) {
            crossfade(true)
            placeholder(ShimmerDrawable().apply {
                setShimmer(shimmer)
            })
            error(R.drawable.ic_broken_image)
            build()
        }
}