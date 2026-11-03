package com.telelauncher.data

data class TmdbMovie(val id: Int, val title: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?)
data class TmdbMovieResponse(val results: List<TmdbMovie>)

data class TmdbTv(val id: Int, val name: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?)
data class TmdbTvResponse(val results: List<TmdbTv>)