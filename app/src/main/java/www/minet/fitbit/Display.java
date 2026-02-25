package www.minet.fitbit;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class Display extends AppCompatActivity {

    private TextView steps, distance, moveMinutes, walkingSpeed, caloriesBurned, heartRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_display);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        steps = findViewById(R.id.steps);
        distance = findViewById(R.id.distance);
        moveMinutes = findViewById(R.id.move_minutes);
        walkingSpeed = findViewById(R.id.walking_speed);
        caloriesBurned = findViewById(R.id.calories_burned);
        heartRate = findViewById(R.id.heart_rate);

        loadFitbitData();

    }

    private void loadFitbitData() {

        TokenManager tokenManager = new TokenManager(this);

        tokenManager.getValidAccessToken(new TokenManager.TokenCallback() {
            @Override
            public void onTokenReady(String accessToken) {
                fetchFitbitData(accessToken);
            }

            @Override
            public void onFailure() {
                Log.e("DISPLAY", "No valid token found");
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

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        // daily activity
        api.getDailyActivity(authHeader, todayDate).enqueue(new Callback<DailyActivityResponse>() {
            @Override
            public void onResponse(Call<DailyActivityResponse> call,
                                   Response<DailyActivityResponse> response) {

                if (response.code() == 401) {
                    handle401();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    DailyActivityResponse data = response.body();

                    int stepCount = data.summary.steps;
                    int calories = data.summary.caloriesOut;
                    int activeMinutes = data.summary.veryActiveMinutes;

                    double totalDistance = 0;
                    if (data.summary.distances != null) {
                        for (DailyActivityResponse.Distance d : data.summary.distances) {
                            if ("total".equals(d.activity)) {
                                totalDistance = d.distance;
                                break;
                            }
                        }
                    }

                    double speed = activeMinutes > 0
                            ? totalDistance / (activeMinutes / 60.0)
                            : 0;

                    steps.setText(String.valueOf(stepCount));
                    distance.setText(totalDistance + " km");
                    moveMinutes.setText(String.valueOf(activeMinutes));
                    caloriesBurned.setText(String.valueOf(calories));
                    walkingSpeed.setText(String.format("%.2f km/h", speed));
                }
            }

            @Override
            public void onFailure(Call<DailyActivityResponse> call, Throwable t) {
                Log.e("DISPLAY", "Activity fetch failed", t);
            }
        });

        // heart rate
        api.getHeartRate(authHeader).enqueue(new Callback<HeartRateResponse>() {
            @Override
            public void onResponse(Call<HeartRateResponse> call,
                                   Response<HeartRateResponse> response) {

                if (response.code() == 401) {
                    handle401();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {

                    int hr = response.body()
                            .activitiesHeart.get(0)
                            .value.restingHeartRate;

                    heartRate.setText(hr + " bpm");
                }
            }

            @Override
            public void onFailure(Call<HeartRateResponse> call, Throwable t) {
                Log.e("DISPLAY", "Heart rate fetch failed", t);
            }
        });
    }

    private void handle401() {

        TokenManager tokenManager = new TokenManager(this);

        tokenManager.refreshAccessToken(new TokenManager.TokenCallback() {
            @Override
            public void onTokenReady(String newAccessToken) {
                fetchFitbitData(newAccessToken);
            }

            @Override
            public void onFailure() {
                Log.e("DISPLAY", "Token refresh failed");
            }
        });
    }

}