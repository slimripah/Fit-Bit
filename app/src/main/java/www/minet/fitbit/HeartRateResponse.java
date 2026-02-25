package www.minet.fitbit;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class HeartRateResponse {

    @SerializedName("activities-heart")
    public List<ActivityHeart> activitiesHeart;

    public class ActivityHeart {
        public Value value;
    }

    public class Value {
        public int restingHeartRate;
    }

}
