package com.example.network;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "https://developer.example.com/";
    private static Retrofit retrofit = null;
    private static EventApiService apiService = null;

    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new MockEventInterceptor())
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static synchronized EventApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(EventApiService.class);
        }
        return apiService;
    }
}
