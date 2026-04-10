package me.anjsk.bestoffer.dto;

public class AuctionUpdateRequest {
    private String title;
    private String description;

    protected AuctionUpdateRequest() {}

    public AuctionUpdateRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}