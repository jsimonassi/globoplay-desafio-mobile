package com.simonassi.globoplay.ui.highlights

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.tabs.TabLayoutMediator
import com.simonassi.globoplay.BuildConfig
import com.simonassi.globoplay.R
import com.simonassi.globoplay.data.Genre
import com.simonassi.globoplay.data.favorite.entity.Favorite
import com.simonassi.globoplay.data.movie.Movie
import com.simonassi.globoplay.data.tv.Tv
import com.simonassi.globoplay.databinding.ActivityHighlightsBinding
import com.simonassi.globoplay.databinding.NetworkErrorLayoutBinding
import com.simonassi.globoplay.ui.videoplayer.VideoPlayerActivity
import com.simonassi.globoplay.utilities.Utils
import com.simonassi.globoplay.utilities.contants.ImageQualitySpec
import com.simonassi.globoplay.utilities.contants.ItemType
import com.simonassi.globoplay.viewmodels.HighlightsViewModel
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation

@AndroidEntryPoint
class HighlightsActivity : AppCompatActivity() {

    val args: HighlightsActivityArgs by navArgs()
    private val highlighthsViewModel: HighlightsViewModel by viewModels()
    private lateinit var movie: Movie
    private lateinit var tv: Tv
    private lateinit var binding: ActivityHighlightsBinding
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Globoplay)

        if(!Utils.isNetworkAvailable(this)){
            val errorBinding = NetworkErrorLayoutBinding.inflate(layoutInflater)
            setContentView(errorBinding.root)
            return
        }

        binding = ActivityHighlightsBinding.inflate(layoutInflater)
        val view = binding.root
        supportActionBar?.hide()
        setContentView(view)

        highlighthsViewModel.favoriteSearchLiveData.observe(this, Observer { favorite ->
            setupFavorite(favorite)
        })
        highlighthsViewModel.getFavoriteById(args.itemId)

        when (args.type) {
            ItemType.MOVIE -> {
                highlighthsViewModel.movieSearchLiveData.observe(this, Observer { fetchedMovie ->
                    movie = fetchedMovie
                    setupMovie(fetchedMovie)
                })
                highlighthsViewModel.getMovieById(args.itemId)
            }
            ItemType.TV -> {
                highlighthsViewModel.tvSearchLiveData.observe(this, Observer { fetchedTv ->
                    tv = fetchedTv
                    setupTv(fetchedTv)
                })
                highlighthsViewModel.getTvById(args.itemId)
            }
        }

        binding.moreContentViewPager.adapter = TabAdapter(this)
        TabLayoutMediator(binding.moreContentTabLayout, binding.moreContentViewPager){tab, position ->
            val titles = listOf(R.string.suggestion_title, R.string.details_title)
            tab.text = this.getString(titles[position])
        }.attach()
        setupListeners()
        setupVideoObserver(view)
    }

    private fun setupInfos(
        coverPath: String,
        backgroundImagePath: String,
        title: String,
        overview: String,
        genre: List<Genre>,
    ) {
        Glide.with(binding.coverImageView.context)
            .load(coverPath)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.coverImageView)

        Glide.with(this)
            .load(backgroundImagePath)
            .apply(bitmapTransform(BlurTransformation(18, 3)))
            .transition(DrawableTransitionOptions.withCrossFade(1500))
            .into(object : CustomTarget<Drawable?>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    binding.blurBackgroundLayout.background = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        binding.titleTextView.text = title
        binding.overviewTextView.text = overview
        if(genre.isNotEmpty()){
            binding.typeTextView.text = genre[0].name
            highlighthsViewModel.getRelatedMovies(genre[0].id)
        }

    }

    fun setupMovie(movie: Movie){
        movie.cover = BuildConfig.BUCKET_URL + ImageQualitySpec.LOW_QUALITY + movie.cover
        movie.backdropCover = BuildConfig.BUCKET_URL + ImageQualitySpec.LOW_QUALITY + movie.backdropCover
        setupInfos(
            movie.cover,
            movie.backdropCover,
            movie.title,
            movie.overview,
            movie.currentGenres,
        )
    }

    fun setupTv(tv: Tv){
        tv.cover = BuildConfig.BUCKET_URL + ImageQualitySpec.LOW_QUALITY + tv.cover
        tv.backdropCover = BuildConfig.BUCKET_URL + ImageQualitySpec.LOW_QUALITY + tv.backdropCover
        setupInfos(
            tv.cover,
            tv.backdropCover,
            tv.title,
            tv.overview,
            tv.currentGenres
        )
    }

    fun setupFavorite(favorite: Favorite?){
        if(favorite == null){
            isFavorite = false
            binding.favoriteTextView.text = this.getText(R.string.my_list)
            Glide.with(binding.favoriteIconImageView.context)
                .load(R.drawable.ic_favorite)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.favoriteIconImageView)
        }else{
            isFavorite = true
            binding.favoriteTextView.text = this.getText(R.string.added_into_my_list)
            Glide.with(binding.favoriteIconImageView.context)
                .load(R.drawable.ic_check)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.favoriteIconImageView)
        }
    }

    private fun setupListeners() {
        binding.favoriteButton.setOnClickListener(View.OnClickListener {
            if(!isFavorite){
                var newFavorite: Favorite?
                when(args.type){
                    ItemType.TV -> {
                        newFavorite = Favorite(args.itemId, tv.title, args.type, tv.cover)
                    }
                    else -> {
                        newFavorite = Favorite(args.itemId, movie.title, args.type, movie.cover)
                    }
                }
                highlighthsViewModel.saveFavorite(newFavorite)
                isFavorite = true
                setupFavorite(newFavorite)
            }else{
                highlighthsViewModel.deleteFavoriteById(args.itemId)
                isFavorite = false
                setupFavorite(null)
            }
        })

        binding.showContentButton.setOnClickListener(View.OnClickListener {
            binding.progress.visibility = View.VISIBLE
            when(args.type){
                ItemType.MOVIE -> {
                    highlighthsViewModel.getMovieVideoLink(args.itemId, baseContext)
                }
                else -> {
                    highlighthsViewModel.getTvVideoLink(args.itemId, baseContext)
                }
            }
        })

        binding.goBackButton.setOnClickListener(View.OnClickListener {
            finish()
        })
    }

    private fun setupVideoObserver(view: View){
        highlighthsViewModel.videoUrl.observe(this, Observer { videoUrl ->
            binding.progress.visibility = View.INVISIBLE
            if(videoUrl.isNotEmpty()){
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", videoUrl)
                startActivity(intent)
            }else{
                Toast.makeText(this, getString(R.string.video_unavailable), Toast.LENGTH_LONG).show()
            }
        })
    }

}