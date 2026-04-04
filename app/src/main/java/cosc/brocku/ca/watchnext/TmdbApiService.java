package cosc.brocku.ca.watchnext;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TmdbApiService {

    @GET("movie/popular")
    Call<TmdbResponse> getPopularMovies();

    @GET("tv/popular")
    Call<TmdbResponse> getPopularTvShows();

    @GET("discover/movie")
    Call<TmdbResponse> discoverMovies(@Query("with_genres") int genreId);

    @GET("discover/tv")
    Call<TmdbResponse> discoverTv(@Query("with_genres") int genreId);

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
}