package com.medina.app.api;

import com.medina.app.model.ForgotPasswordRequest;
import com.medina.app.model.LoginRequest;
import com.medina.app.model.LoginResponse;
import com.medina.app.model.ResetPasswordRequest;
import com.medina.app.model.User;
import com.medina.app.model.Notification;
import com.medina.app.model.Alert;
import com.medina.app.model.Task;
import com.medina.app.model.PartRequest;
import com.medina.app.model.Shelf;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.DELETE;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.PATCH;

public interface ApiService {

    @POST("api/users/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/users/forgot-password")
    Call<Map<String, String>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/users/reset-password")
    Call<Map<String, String>> resetPassword(@Body ResetPasswordRequest request);

    @GET("api/users/me")
    Call<User> getCurrentUser();

    @PUT("api/users/profile")
    Call<Map<String, Object>> updateProfile(@Body Map<String, String> request);

    @POST("api/users/change-password")
    Call<Map<String, Object>> changePassword(@Body Map<String, String> request);

    @POST("api/users/change-email")
    Call<Map<String, Object>> changeEmail(@Body Map<String, String> request);

    @GET("api/notifications")
    Call<List<Notification>> getNotifications(@Query("userId") String userId, @Query("role") String role);

    @PUT("api/notifications/{id}/read")
    Call<Notification> markNotificationAsRead(@Path("id") String id);

    @PUT("api/notifications/read-all")
    Call<Void> markAllNotificationsAsRead(@Query("userId") String userId, @Query("role") String role);

    @DELETE("api/notifications/{id}")
    Call<Void> deleteNotification(@Path("id") String id);

    @DELETE("api/notifications/all")
    Call<Void> deleteAllNotifications();

    @GET("api/alerts")
    Call<List<Alert>> getAlerts(@Query("userId") String userId, @Query("role") String role);

    @PUT("api/alerts/{id}/resolve")
    Call<Alert> resolveAlert(@Path("id") String id);

    @PUT("api/alerts/resolve-all")
    Call<Void> resolveAllAlerts(@Query("userId") String userId, @Query("role") String role);

    @DELETE("api/alerts/{id}")
    Call<Void> deleteAlert(@Path("id") String id);

    @DELETE("api/alerts/all")
    Call<Void> deleteAllAlerts();

    // --- Task management endpoints ---
    @GET("api/users/tasks/user/{userId}")
    Call<List<Task>> getTasksByUser(@Path("userId") String userId);

    @POST("api/users/tasks")
    Call<Task> createTask(@Body Task task);

    @PUT("api/users/tasks/{id}")
    Call<Task> updateTask(@Path("id") String id, @Body Task task);

    @PATCH("api/users/tasks/{id}/status")
    Call<Task> updateTaskStatus(@Path("id") String id, @Query("status") String status);

    @DELETE("api/users/tasks/{id}")
    Call<Void> deleteTask(@Path("id") String id);

    // --- Part Request & Shelf endpoints ---
    @GET("api/part-requests/my/{requesterId}")
    Call<List<PartRequest>> getMyPartRequests(@Path("requesterId") String requesterId);

    @POST("api/part-requests")
    Call<PartRequest> createPartRequest(@Body PartRequest request);

    @POST("api/equipment/{id}/return")
    Call<Void> returnPart(@Path("id") String id);

    @PUT("api/part-requests/{id}")
    Call<PartRequest> updatePartRequest(@Path("id") String id, @Body PartRequest request);

    @GET("api/shelves")
    Call<List<Shelf>> getAllShelves();
}
