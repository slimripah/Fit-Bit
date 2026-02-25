package www.minet.fitbit;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TokenManager {

    private static final String PREF_NAME = "fitbit_prefs";
    private Context context;
    private SharedPreferences prefs;
    private static final String CLIENT_ID = "23V33B";
    private static final String CLIENT_SECRET = "9790ce8db15fbc40d19a5c7b62551902";
    private boolean isRefreshing = false;
    private java.util.List<TokenCallback> pendingCallbacks = new java.util.ArrayList<>();

    public TokenManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // save tokens
    public void saveTokens(String accessToken, String refreshToken, int expiresIn) {
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .putLong("expiry_time",
                        System.currentTimeMillis() + (expiresIn * 1000L))
                .apply();
    }

    // get saved access token
    public String getAccessToken() {
        return prefs.getString("access_token", null);
    }

    // check if token expired
    public boolean isTokenExpired() {
        long expiryTime = prefs.getLong("expiry_time", 0);
        return System.currentTimeMillis() >= expiryTime;
    }

    // refresh token
    public synchronized void refreshAccessToken(TokenCallback callback) {

        pendingCallbacks.add(callback);

        if (isRefreshing) {
            return; // Already refreshing — just wait
        }

        isRefreshing = true;

        String refreshToken = prefs.getString("refresh_token", null);

        if (refreshToken == null) {
            notifyFailure();
            return;
        }

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basicAuth = "Basic " + Base64.encodeToString(
                credentials.getBytes(),
                Base64.NO_WRAP
        );

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FitbitApi api = retrofit.create(FitbitApi.class);

        api.refreshToken(
                basicAuth,
                "refresh_token",
                refreshToken
        ).enqueue(new Callback<TokenResponse>() {

            @Override
            public void onResponse(Call<TokenResponse> call,
                                   Response<TokenResponse> response) {

                isRefreshing = false;

                if (response.isSuccessful() && response.body() != null) {

                    TokenResponse token = response.body();

                    saveTokens(
                            token.accessToken,
                            token.refreshToken,
                            token.expiresIn
                    );

                    notifySuccess(token.accessToken);
                } else {
                    notifyFailure();
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                isRefreshing = false;
                notifyFailure();
            }
        });
    }

    private void notifySuccess(String accessToken) {
        for (TokenCallback cb : pendingCallbacks) {
            cb.onTokenReady(accessToken);
        }
        pendingCallbacks.clear();
    }

    private void notifyFailure() {
        for (TokenCallback cb : pendingCallbacks) {
            cb.onFailure();
        }
        pendingCallbacks.clear();
    }

    // get valid token (auto refresh if needed)
    public void getValidAccessToken(TokenCallback callback) {

        String token = getAccessToken();

        if (token == null) {
            callback.onFailure();
            return;
        }

        if (isTokenExpired()) {
            refreshAccessToken(callback);
        } else {
            callback.onTokenReady(token);
        }
    }

    // callback interface
    public interface TokenCallback {
        void onTokenReady(String accessToken);
        void onFailure();
    }
}
