package com.avi.sensorproject;

/**
 * Created by Avi on 6/27/2016.
 */
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface SensorSpreadsheetWebService {

    @POST("1tj_HHO4wpW-93rl4xZqHwFcyK_hDPkV0CUOJrEBnX28/formResponse")
    @FormUrlEncoded
    Call<Void> completeQuestionnaire(
            @Field("entry.1309128794") String Latitude,
            @Field("entry.1637727623") String Longitude,
            @Field("entry.880509778") String CarbonMonoxide,
            @Field("entry.875272081") String NitrogenDioxide,
            @Field("entry.1834705411") String Ammonia
    );
}

/* URL : https://docs.google.com/forms/d/1tj_HHO4wpW-93rl4xZqHwFcyK_hDPkV0CUOJrEBnX28/formResponse
    Latitude : entry.1309128794
    Longitude : entry.1637727623
    Carbon Monoxide : entry.880509778
    Nitrogen Dioxide : entry.875272081
    Ammonia : entry.1834705411

 */