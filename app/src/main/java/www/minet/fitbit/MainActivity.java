package www.minet.fitbit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    Button btn;

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

        btn = findViewById(R.id.btn_request_permissions);

        btn.setOnClickListener(v -> {
            String CLIENT_ID = "23V33B";
            String REDIRECT_URI = "myapp://callback";

            Uri authUri = Uri.parse("https://www.fitbit.com/oauth2/authorize")
                    .buildUpon()
                    .appendQueryParameter("response_type", "code")
                    .appendQueryParameter("client_id", CLIENT_ID)
                    .appendQueryParameter("redirect_uri", REDIRECT_URI)
                    .appendQueryParameter("scope", "activity heartrate profile")
                    .build();

            Intent intent = new Intent(Intent.ACTION_VIEW, authUri);
            startActivity(intent);
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();

        if (uri != null && uri.getQueryParameter("code") != null) {

            String authCode = uri.getQueryParameter("code");
            Log.d("OAUTH", "Authorization Code: " + authCode);

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

                    String accessToken = response.body().accessToken;
                    fetchFitbitData(accessToken);

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

    private void fetchFitbitData(String accessToken) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        FitbitApi api = retrofit.create(FitbitApi.class);

        String authHeader = "Bearer " + accessToken;

        // <<< ADD THIS HERE >>>
        Calendar calendar = Calendar.getInstance();
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.getTime());

        // ===== DAILY ACTIVITY =====
        api.getDailyActivity(authHeader, todayDate).enqueue(new Callback<DailyActivityResponse>() {
            @Override
            public void onResponse(Call<DailyActivityResponse> call,
                                   Response<DailyActivityResponse> response) {

                Log.d("FITBIT", "HTTP Code: " + response.code());
                Log.d("FITBIT", "Response body: " + new Gson().toJson(response.body()));

                if (response.isSuccessful() && response.body() != null) {

                    Log.d("FITBIT", "Parsed JSON: " +
                            new com.google.gson.Gson().toJson(response.body()));

                    DailyActivityResponse data = response.body();

                    int steps = data.summary.steps;
                    int calories = data.summary.caloriesOut;
                    int moveMinutes = data.summary.veryActiveMinutes;

                    double distance = 0;

                    if (data.summary.distances != null) {
                        for (DailyActivityResponse.Distance d : data.summary.distances) {
                            if ("total".equals(d.activity)) {
                                distance = d.distance;
                                break;
                            }
                        }
                    }

                    double walkingSpeed;
                    if (moveMinutes > 0) {
                        walkingSpeed = distance / (moveMinutes / 60.0);
                    } else {
                        walkingSpeed = 0;
                    }

                    // ===== UPDATE UI =====
                    double finalDistance = distance;
                    runOnUiThread(() -> {
                        ((TextView) findViewById(R.id.steps))
                                .setText(String.valueOf(steps));

                        ((TextView) findViewById(R.id.distance))
                                .setText(finalDistance + " km");

                        ((TextView) findViewById(R.id.move_minutes))
                                .setText(String.valueOf(moveMinutes));

                        ((TextView) findViewById(R.id.calories_burned))
                                .setText(String.valueOf(calories));

                        ((TextView) findViewById(R.id.walking_speed))
                                .setText(String.format("%.2f km/h", walkingSpeed));
                    });
                }
            }

            @Override
            public void onFailure(Call<DailyActivityResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });

        // ===== HEART RATE =====
        api.getHeartRate(authHeader).enqueue(new Callback<HeartRateResponse>() {
            @Override
            public void onResponse(Call<HeartRateResponse> call,
                                   Response<HeartRateResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    int heartRate = response.body()
                            .activitiesHeart.get(0)
                            .value.restingHeartRate;

                    runOnUiThread(() -> {
                        ((TextView) findViewById(R.id.heart_rate))
                                .setText(String.valueOf(heartRate) + " bpm");
                    });
                }
            }

            @Override
            public void onFailure(Call<HeartRateResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

}