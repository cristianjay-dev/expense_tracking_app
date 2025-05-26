package com.shop.expensestrackingapp;

public class UserModel {
    private int userId;
    private String firstName;
    private String email;
    private byte[] profileImage;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    public UserModel(int userId, String firstName, String email, byte[] profileImage) {
        this.userId = userId;
        this.firstName = firstName;
        this.email = email;
        this.profileImage = profileImage;
    }

}
