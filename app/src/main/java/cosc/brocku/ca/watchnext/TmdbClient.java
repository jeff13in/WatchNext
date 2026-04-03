package cosc.brocku.ca.watchnext;

import cosc.brocku.ca.watchnext.BuildConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TmdbClient {

    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static TmdbApiService service;

    public static TmdbApiService getService() {
        if (service == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request().newBuilder()
                                .addHeader("Authorization",
                                        "Bearer " + BuildConfig.TMDB_ACCESS_TOKEN)
                                .addHeader("accept", "application/json")
                                .build();
                        return chain.proceed(request);
                    })
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(TmdbApiService.class);
        }
        return service;
    }
}
