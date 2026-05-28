package com.example.network;

import com.example.model.Event;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface EventApiService {

    @GET("api/events")
    Call<List<Event>> getEvents();

    @GET("api/events/{id}")
    Call<Event> getEventById(@Path("id") Long id);

    @POST("api/events")
    Call<Event> createEvent(@Body Event event);

    @PUT("api/events/{id}")
    Call<Event> updateEvent(@Path("id") Long id, @Body Event event);

    @DELETE("api/events/{id}")
    Call<Void> deleteEvent(@Path("id") Long id);
}
