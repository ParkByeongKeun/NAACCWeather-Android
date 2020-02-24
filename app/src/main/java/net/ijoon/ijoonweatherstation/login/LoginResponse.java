package net.ijoon.ijoonweatherstation.login;

public class LoginResponse {

    private boolean success;
    private String accessToken;

    public void setSuccess(boolean success) {
        this.success = success;
    }
    public boolean getSuccess() {
        return success;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public String getAccessToken() {
        return this.accessToken;
    }
}