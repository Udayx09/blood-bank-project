package com.bloodbank.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper matching Node.js format
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private String error;
    private T data;
    private Integer count;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, String message, String error, T data, Integer count) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.data = data;
        this.count = count;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    // Builder
    public static <T> ApiResponseBuilder<T> builder() {
        return new ApiResponseBuilder<>();
    }

    public static class ApiResponseBuilder<T> {
        private boolean success;
        private String message;
        private String error;
        private T data;
        private Integer count;

        public ApiResponseBuilder<T> success(boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder<T> message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponseBuilder<T> error(String error) {
            this.error = error;
            return this;
        }

        public ApiResponseBuilder<T> data(T data) {
            this.data = data;
            return this;
        }

        public ApiResponseBuilder<T> count(Integer count) {
            this.count = count;
            return this;
        }

        public ApiResponse<T> build() {
            return new ApiResponse<>(success, message, error, data, count);
        }
    }

    // Static factory methods
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, int count) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .count(count)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(errorMessage)
                .build();
    }

    public static ApiResponse<String> message(String message) {
        return ApiResponse.<String>builder()
                .success(true)
                .message(message)
                .build();
    }
}
