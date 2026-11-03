package com.telelauncher.data

data class TmdbMovieDetail(val id: Int, val title: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?, val release_date: String?, val genres: List<TmdbGenre> = emptyList())
data class TmdbTvDetail(val id: Int, val name: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?, val first_air_date: String?, val genres: List<TmdbGenre> = emptyList())
data class TmdbGenre(val id: Int, val name: String)
data class TmdbSearchItem(val id: Int, val media_type: String?, val title: String?, val name: String?, val poster_path: String?, val vote_average: Double? = 0.0, val overview: String?)
data class TmdbSearchResponse(val results: List<TmdbSearchItem>)