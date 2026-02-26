package www.minet.fitbit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    Button btnLogin;
    private Button btnOpenDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnLogin = findViewById(R.id.btn_request_permissions);
        btnOpenDisplay = findViewById(R.id.btn_open_display);

        TokenManager tokenManager = new TokenManager(this);

        // if already logged in hide login
        if (tokenManager.getAccessToken() != null) {
            btnLogin.setVisibility(View.GONE);
            btnOpenDisplay.setVisibility(View.VISIBLE);
        } else {
            btnLogin.setVisibility(View.VISIBLE);
            btnOpenDisplay.setVisibility(View.GONE);
        }

        // login button
        btnLogin.setOnClickListener(v -> {

            String CLIENT_ID = "23V33B";
            String REDIRECT_URI = "myapp://callback";

            Uri authUri = Uri.parse("https://www.fitbit.com/oauth2/authorize")
                    .buildUpon()
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("client_id", CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("scope", "activity heartrate profile")
                    .build();

            startActivity(new Intent(Intent.ACTION_VIEW, authUri));
        });

        // open display button
        btnOpenDisplay.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, Display.class));
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();

        if (uri != null && uri.getQueryParameter("code") != null) {

            String authCode = uri.getQueryParameter("code");
            exchangeCodeForToken(authCode);
        }
    }

    private void exchangeCodeForToken(String code) {

        String CLIENT_ID = "23V33B";
        String CLIENT_SECRET = "9790ce8db15fbc40d19a5c7b62551902";
        String REDIRECT_URI = "myapp://callback";

        String credentials = CLIENT_ID + ":" + CLIENT_SECRET;
        String basicAuth = "Basic " + android.util.Base64.encodeToString(
                credentials.getBytes(),
                android.util.Base64.NO_WRAP
        );

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FitbitApi api = retrofit.create(FitbitApi.class);

        api.exchangeCode(
                basicAuth,
                CLIENT_ID,
                "authorization_code",
                REDIRECT_URI,
                code
        ).enqueue(new Callback<TokenResponse>() {

            @Override
            public void onResponse(Call<TokenResponse> call,
                                   Response<TokenResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    TokenResponse token = response.body();

                    // Use TokenManager to save tokens
                    TokenManager tokenManager = new TokenManager(MainActivity.this);
                    tokenManager.saveTokens(
                            token.accessToken,
                            token.refreshToken,
                            token.expiresIn
                    );

                    // Update UI
                    btnLogin.setVisibility(View.GONE);
                    btnOpenDisplay.setVisibility(View.VISIBLE);

                } else {
                    Log.e("OAUTH", "Token exchange failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                Log.e("OAUTH", "Token request failed", t);
            }
        });
    }

}