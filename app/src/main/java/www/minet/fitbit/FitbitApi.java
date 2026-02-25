package www.minet.fitbit;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface FitbitApi {

    @FormUrlEncoded
    @POST("oauth2/token")
    Call<TokenResponse> exchangeCode(
            @Header("Authorization") String authHeader,
            @Field("client_id") String clientId,
            @Field("grant_type") String grantType,
            @Field("redirect_uri") String redirectUri,
            @Field("code") String code
    );

    @FormUrlEncoded
    @POST("oauth2/token")
    Call<TokenResponse> refreshToken(
            @Header("Authorization") String authHeader,
            @Field("grant_type") String grantType,
            @Field("refresh_token") String refreshToken
    );

    @GET("1/user/-/activities/date/{date}.json")
    Call<DailyActivityResponse> getDailyActivity(
            @Header("Authorization") String authHeader,
            @retrofit2.http.Path("date") String date
    );

    @GET("1/user/-/activities/heart/date/today/1d.json")
    Call<HeartRateResponse> getHeartRate(
            @Header("Authorization") String authHeader
    );

}
