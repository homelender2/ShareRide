package com.companyname.shareride;

public class ChatMessage {
    private String sender;
    private String message;
    private boolean isOwnMessage;

    public ChatMessage(String sender, String message, boolean isOwnMessage) {
        this.sender = sender;
        this.message = message;
        this.isOwnMessage = isOwnMessage;
    }

    // Getters
    public String getSender() { return sender; }
    public String getMessage() { return message; }
    public boolean isOwnMessage() { return isOwnMessage; }
}