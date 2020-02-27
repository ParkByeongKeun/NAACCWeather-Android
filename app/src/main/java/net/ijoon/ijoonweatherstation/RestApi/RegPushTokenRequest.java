package net.ijoon.ijoonweatherstation.RestApi;

public class RegPushTokenRequest {
    private final String UUID;
    private final String pushToken;
    public RegPushTokenRequest(String UUID, String pushToken) {
        this.UUID = UUID;
        this.pushToken = pushToken;
    }
}
