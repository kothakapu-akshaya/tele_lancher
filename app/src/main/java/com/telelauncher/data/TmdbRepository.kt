package com.telelauncher.data

import com.telelauncher.BuildConfig

class TmdbRepository(private val api: TmdbApi = TmdbClient.api) {
    suspend fun trendingMovies() = api.trendingMovies(BuildConfig.TMDB_API_KEY).results
    suspend fun trendingTv() = api.trendingTv(BuildConfig.TMDB_API_KEY).results
    suspend fun popularMovies(region: String = "IN") = api.popularMovies(BuildConfig.TMDB_API_KEY, region).results
    suspend fun popularTv(region: String = "IN") = api.popularTv(BuildConfig.TMDB_API_KEY, region).results
    suspend fun movieDetails(id: Int) = api.movieDetails(id, BuildConfig.TMDB_API_KEY)
    suspend fun tvDetails(id: Int) = api.tvDetails(id, BuildConfig.TMDB_API_KEY)
    suspend fun search(q: String) = api.search(q, BuildConfig.TMDB_API_KEY).results
    suspend fun movieProviders(id: Int, region: String = "IN") = api.movieProviders(id, BuildConfig.TMDB_API_KEY).results[region]
    suspend fun tvProviders(id: Int, region: String = "IN") = api.tvProviders(id, BuildConfig.TMDB_API_KEY).results[region]
}