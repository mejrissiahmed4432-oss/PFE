package com.medina.app.api;

import com.medina.app.model.ForgotPasswordRequest;
import com.medina.app.model.LoginRequest;
import com.medina.app.model.LoginResponse;
import com.medina.app.model.ResetPasswordRequest;
import com.medina.app.model.User;
import com.medina.app.model.Notification;
import com.medina.app.model.Alert;
import com.medina.app.model.Task;

import com.medina.app.model.Shelf;
import com.medina.app.model.Message;
import com.medina.app.model.ConversationSummary;
import com.medina.app.model.Ticket;
import com.medina.app.model.Equipment;
import com.medina.app.model.AiRequest;
import com.medina.app.model.AiResponse;
import com.medina.app.model.AiConversation;

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
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import okhttp3.MultipartBody;

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

    @POST("api/equipment/{id}/return")
    Call<Void> returnPart(@Path("id") String id);

    @POST("api/part-requests/consume-parts/{requesterId}")
    Call<Void> consumeParts(@Path("requesterId") String requesterId, @Body java.util.List<java.util.Map<String, Object>> parts);


    @GET("api/shelves")
    Call<List<Shelf>> getAllShelves();

    @GET("api/messages/conversations")
    Call<List<ConversationSummary>> getConversations();

    @GET("api/messages/history/{otherUserId}")
    Call<List<Message>> getChatHistory(@Path("otherUserId") String otherUserId);

    @POST("api/messages")
    Call<Message> sendMessage(@Body Message message);

    @PUT("api/messages/read/{senderId}")
    Call<Map<String, String>> markAsRead(@Path("senderId") String senderId);

    @PUT("api/messages/{id}")
    Call<Message> editMessage(@Path("id") String id, @Body Map<String, String> body);

    @DELETE("api/messages/{id}")
    Call<Void> deleteMessage(@Path("id") String id, @Query("forEveryone") boolean forEveryone);

    @Multipart
    @POST("api/messages/upload")
    Call<Map<String, String>> uploadAttachment(@Part MultipartBody.Part file);

    @GET("api/users")
    Call<List<User>> getAllUsers();

    // --- Ticket endpoints ---
    @GET("api/tickets")
    Call<List<Ticket>> getAllTickets();

    @GET("api/tickets/user/{userId}")
    Call<List<Ticket>> getTicketsByUser(@Path("userId") String userId);

    @GET("api/tickets/technician/{techId}")
    Call<List<Ticket>> getTicketsForTechnician(@Path("techId") String techId);

    @GET("api/tickets/{id}")
    Call<Ticket> getTicketById(@Path("id") String id);

    @POST("api/tickets")
    Call<Ticket> createTicket(@Body Ticket ticket);

    @PUT("api/tickets/{id}")
    Call<Ticket> updateTicket(@Path("id") String id, @Body Ticket ticket);

    @DELETE("api/tickets/{id}")
    Call<Void> deleteTicket(@Path("id") String id);

    // --- Equipment endpoints ---
    @GET("api/equipment")
    Call<List<Equipment>> getAllEquipment();

    @GET("api/equipment/{id}")
    Call<Equipment> getEquipmentById(@Path("id") String id);

    @PUT("api/equipment/{id}")
    Call<Equipment> updateEquipment(@Path("id") String id, @Body Equipment equipment);

    // --- AI Assistant endpoints ---
    @POST("api/ai/query")
    Call<AiResponse> queryAi(@Body AiRequest request);

    @POST("api/ai/action/execute")
    Call<AiResponse> executeAiAction(@Body Map<String, Object> body);

    @GET("api/aiconversations")
    Call<List<AiConversation>> getAllAiConversations();

    @POST("api/aiconversations")
    Call<AiConversation> saveAiConversation(@Body AiConversation conversation);

    @DELETE("api/aiconversations/{id}")
    Call<Void> deleteAiConversation(@Path("id") String id);
}
