package www.minet.fitbit;

import java.util.List;

public class DailyActivityResponse {

    public Summary summary;

    public static class Summary {
        public int steps;
        public int caloriesOut;
        public int veryActiveMinutes;
        public List<Distance> distances;
    }

    public static class Distance {
        public String activity;
        public double distance;
    }

}
