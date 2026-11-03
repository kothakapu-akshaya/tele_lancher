package com.telelauncher.data

import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApi {
    @GET("trending/movie/week")
    suspend fun trendingMovies(@Query("api_key") key: String): TmdbMovieResponse

    @GET("trending/tv/week")
    suspend fun trendingTv(@Query("api_key") key: String): TmdbTvResponse

    @GET("movie/popular")
    suspend fun popularMovies(@Query("api_key") key: String, @Query("region") region: String): TmdbMovieResponse

    @GET("tv/popular")
    suspend fun popularTv(@Query("api_key") key: String, @Query("region") region: String): TmdbTvResponse

    @GET("movie/{id}")
    suspend fun movieDetails(@retrofit2.http.Path("id") id: Int, @Query("api_key") key: String): TmdbMovieDetail

    @GET("tv/{id}")
    suspend fun tvDetails(@retrofit2.http.Path("id") id: Int, @Query("api_key") key: String): TmdbTvDetail

    @GET("search/multi")
    suspend fun search(@Query("query") q: String, @Query("api_key") key: String): TmdbSearchResponse

    @GET("movie/{id}/watch/providers")
    suspend fun movieProviders(@retrofit2.http.Path("id") id: Int, @Query("api_key") key: String): TmdbWatchProvidersResponse

    @GET("tv/{id}/watch/providers")
    suspend fun tvProviders(@retrofit2.http.Path("id") id: Int, @Query("api_key") key: String): TmdbWatchProvidersResponse
}