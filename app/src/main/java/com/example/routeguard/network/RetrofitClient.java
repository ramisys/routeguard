package com.example.routeguard.network;

import com.example.routeguard.BuildConfig;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = BuildConfig.BASE_URL;
    private static final String OSRM_URL = "https://router.project-osrm.org/";
    private static RetrofitClient instance;
    private final ApiService apiService;
    private final RoutingService routingService;

    private RetrofitClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    HttpUrl url = original.url();
                    
                    // Only add auth to our own API, not OSRM
                    if (url.toString().startsWith(BASE_URL)) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            try {
                                GetTokenResult tokenResult = Tasks.await(user.getIdToken(false));
                                String token = tokenResult.getToken();
                                if (token != null) {
                                    Request authenticated = original.newBuilder()
                                            .header("Authorization", "Bearer " + token)
                                            .build();
                                    return chain.proceed(authenticated);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return chain.proceed(original);
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        Retrofit osrmRetrofit = new Retrofit.Builder()
                .baseUrl(OSRM_URL)
                .client(new OkHttpClient.Builder().addInterceptor(logging).build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        routingService = osrmRetrofit.create(RoutingService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public RoutingService getRoutingService() {
        return routingService;
    }
}