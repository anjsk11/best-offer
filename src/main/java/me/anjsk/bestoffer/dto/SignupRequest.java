package me.anjsk.bestoffer.dto;

public class SignupRequest {
    private String email;
    private String password;
    private String nickname;

    protected SignupRequest() {}

    public SignupRequest(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getNickname() { return nickname; }
}