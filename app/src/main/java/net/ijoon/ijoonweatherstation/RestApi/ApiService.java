package net.ijoon.ijoonweatherstation.RestApi;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/auth/regPushToken")
    Call<RegPushTokenResponse> regPushToken(@Body RegPushTokenRequest request);
    @POST("/api/auth/unRegPushToken")
    Call<UnRegPushTokenResponse> regPushToken(@Body UnRegPushTokenRequest request);
}
