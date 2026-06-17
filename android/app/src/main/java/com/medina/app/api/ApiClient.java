package com.medina.app.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    // private static final String BASE_URL = "http://10.0.2.2:8000/";

    // ▶ FOR REAL DEVICE: Replace 192.168.X.X with your PC's IP from ipconfig
    private static final String BASE_URL = "http://192.168.0.194:8000/";

    private static Retrofit retrofit = null;
    
    // Static token variable loaded on launch/login
    public static String authToken = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS);

            // Add dynamic authorization header interceptor
            clientBuilder.addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                if (authToken != null && !authToken.isEmpty()) {
                    okhttp3.Request request = original.newBuilder()
                            .header("Authorization", "Bearer " + authToken)
                            .build();
                    return chain.proceed(request);
                }
                return chain.proceed(original);
            });

            OkHttpClient client = clientBuilder.build();

            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }
}
