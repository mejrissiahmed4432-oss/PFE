package com.medina.app.api;

import com.medina.app.model.ForgotPasswordRequest;
import com.medina.app.model.LoginRequest;
import com.medina.app.model.LoginResponse;
import com.medina.app.model.ResetPasswordRequest;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("api/users/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/users/forgot-password")
    Call<Map<String, String>> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/users/reset-password")
    Call<Map<String, String>> resetPassword(@Body ResetPasswordRequest request);
}
