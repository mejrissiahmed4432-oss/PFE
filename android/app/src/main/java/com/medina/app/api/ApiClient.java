package com.medina.app.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    // ╔══════════════════════════════════════════════════════════════╗
    // ║  BASE URL CONFIGURATION                                      ║
    // ║                                                              ║
    // ║  EMULATOR:  Use 10.0.2.2 (maps to your PC's localhost)      ║
    // ║  REAL DEVICE: Use your PC's Wi-Fi IP from `ipconfig`        ║
    // ║               Both devices must be on the SAME Wi-Fi        ║
    // ╚══════════════════════════════════════════════════════════════╝

    // ▶ FOR EMULATOR:
    // private static final String BASE_URL = "http://10.0.2.2:8080/";

    // ▶ FOR REAL DEVICE: Replace 192.168.X.X with your PC's IP from ipconfig
    private static final String BASE_URL = "http://10.16.100.72:8080/";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
