package net.ijoon.ijoonweatherstation.RestApi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static String URL = "http://1.246.219.189:20217/";

    static OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request newRequest  = chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .addHeader("x-access-token", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJfaWQiOiJhZG1pbkBpam9vbi5uZXQiLCJuYW1lIjoiYWRtaW4iLCJpYXQiOjE1ODI3MDM5NzcsImV4cCI6MTU4MzMwODc3NywiaXNzIjoiaWpvb24ubmV0Iiwic3ViIjoic2Vqb25nIn0.HOj6IlMQ1Wo-pos97UwMEBQL1G_pQyoSzHw1DB8FBpM")
                            .build();
                    return chain.proceed(newRequest);
                }
            }).build();

    public static ApiService getApiService(OkHttpClient header) {
        return getInstance().create(ApiService.class);
    }

    private static Retrofit getInstance() {
        Gson gson = new GsonBuilder().setLenient().create();
        return new Retrofit.Builder()
                .client(client)
                .baseUrl(URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

//
//    Retrofit retrofit = new Retrofit.Builder()
//            .client(client)
//            .baseUrl(URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build();
//
//    ApiService server = retrofit.create(ApiService.class);



}