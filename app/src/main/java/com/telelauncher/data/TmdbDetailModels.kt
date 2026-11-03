package com.telelauncher.data

data class TmdbMovieDetail(val id: Int, val title: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?, val release_date: String?, val genres: List<TmdbGenre> = emptyList())
data class TmdbTvDetail(val id: Int, val name: String, val poster_path: String?, val backdrop_path: String?, val vote_average: Double, val overview: String?, val first_air_date: String?, val genres: List<TmdbGenre> = emptyList())
data class TmdbGenre(val id: Int, val name: String)
data class TmdbSearchItem(val id: Int, val media_type: String?, val title: String?, val name: String?, val poster_path: String?, val vote_average: Double? = 0.0, val overview: String?)
data class TmdbSearchResponse(val results: List<TmdbSearchItem>)

data class TmdbWatchProvidersResponse(val results: Map<String, TmdbWatchRegionProviders> = emptyMap())
data class TmdbWatchRegionProviders(
    val link: String? = null,
    val flatrate: List<TmdbWatchProvider> = emptyList(),
    val rent: List<TmdbWatchProvider> = emptyList(),
    val buy: List<TmdbWatchProvider> = emptyList()
)
data class TmdbWatchProvider(val provider_id: Int, val provider_name: String, val logo_path: String? = null)