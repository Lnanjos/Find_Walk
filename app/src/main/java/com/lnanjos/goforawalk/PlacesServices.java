package com.lnanjos.goforawalk;

import com.lnanjos.models.NearbyPlaces;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PlacesServices {

    @GET("/maps/api/place/nearbysearch/json")
    Call<NearbyPlaces> listRandomPlaces(@Query("location") String location,
                                        @Query("radius") int radius,
                                        @Query("key") String key);

    @GET("/maps/api/place/nearbysearch/json")
    Call<NearbyPlaces> listRandomPlaces(@Query("location") String location,
                                        @Query("radius") int radius,
                                        @Query("key") String key,
                                        @Query("type") String type);
}
