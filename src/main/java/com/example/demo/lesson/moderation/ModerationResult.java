package com.example.demo.lesson.moderation;

public class ModerationResult {

    private String status;
    private String reason;

    public ModerationResult() {}

    public ModerationResult(String status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}