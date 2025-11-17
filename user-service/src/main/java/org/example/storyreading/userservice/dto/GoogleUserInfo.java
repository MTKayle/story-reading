package org.example.storyreading.userservice.dto;

public class GoogleUserInfo {
    private String googleId;
    private String email;
    private String name;
    private String picture;
    private boolean emailVerified;

    public GoogleUserInfo() {
    }

    public GoogleUserInfo(String googleId, String email, String name, String picture, boolean emailVerified) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.picture = picture;
        this.emailVerified = emailVerified;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}

