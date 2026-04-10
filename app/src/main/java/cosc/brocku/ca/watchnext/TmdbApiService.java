package cosc.brocku.ca.watchnext;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("movie/popular")
    Call<TmdbResponse> getPopularMovies();

    @GET("movie/popular")
    Call<TmdbResponse> getPopularMoviesPage(@Query("page") int page);

    @GET("tv/popular")
    Call<TmdbResponse> getPopularTvShows();

    @GET("discover/movie")
    Call<TmdbResponse> discoverMoviesFull(
            @Query("with_genres") Integer genreId,
            @Query("primary_release_year") Integer year,
            @Query("primary_release_date.gte") String dateGte,
            @Query("primary_release_date.lte") String dateLte,
            @Query("vote_average.gte") Float minRating,
            @Query("sort_by") String sortBy,
            @Query("page") int page
    );

    @GET("discover/tv")
    Call<TmdbResponse> discoverTvFull(
            @Query("with_genres") Integer genreId,
            @Query("first_air_date_year") Integer year,
            @Query("first_air_date.gte") String dateGte,
            @Query("first_air_date.lte") String dateLte,
            @Query("vote_average.gte") Float minRating,
            @Query("sort_by") String sortBy,
            @Query("page") int page
    );

    @GET("search/multi")
    Call<TmdbResponse> searchMulti(@Query("query") String query);

    @GET("movie/{movie_id}")
    Call<TmdbMovieDetail> getMovieDetails(
            @Path("movie_id") int movieId,
            @Query("append_to_response") String appendToResponse
    );

    @GET("tv/{tv_id}")
    Call<TmdbMovieDetail> getTvDetails(
            @Path("tv_id") int tvId,
            @Query("append_to_response") String appendToResponse
    );

    @GET("discover/movie")
    Call<TmdbResponse> discoverMoviesByGenre(
            @Query("with_genres") int genreId,
            @Query("page") int page
    );

    @GET("discover/tv")
    Call<TmdbResponse> discoverTvByGenre(
            @Query("with_genres") int genreId,
            @Query("page") int page
    );

    @GET("movie/{movie_id}/similar")
    Call<TmdbResponse> getSimilarMovies(@Path("movie_id") int movieId);

    @GET("tv/{tv_id}/similar")
    Call<TmdbResponse> getSimilarTv(@Path("tv_id") int tvId);
}