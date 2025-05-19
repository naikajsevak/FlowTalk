package com.example.flowtalk.models;

public class MessagesModel {
    private String uid, message, messageId, msgUrl, messageType="", title, thumbnail;
    private long timestamp;
    private int feeling = -1;
    private String isSeen;
    private String delivered;
    private String fileSize;
    private String pageCount;
    public String getFileSize() {
        return String.valueOf(fileSize);
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

    public String getPageCount() {
        return pageCount;
    }

    public void setPageCount(String pageCount) {
        this.pageCount = pageCount;
    }


    // Constructor for general messages
    public MessagesModel(String uid, String message, long timestamp, String isSeen,String delivered) {
        this.uid = uid;
        this.message = message;
        this.timestamp = timestamp;
        this.isSeen = isSeen;
        this.delivered=delivered;
    }

    // Empty constructor for Firebase
    public MessagesModel() {}

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMsgUrl() {
        return msgUrl;
    }

    public void setMsgUrl(String msgUrl) {
        this.msgUrl = msgUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFeeling() {
        return feeling;
    }

    public void setFeeling(int feeling) {
        this.feeling = feeling;
    }

    public String isSeen() {
        return isSeen;
    }

    public void setSeen(String seen) {
        isSeen = seen;
    }


    // Fields for handling YouTube messages
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getDelivered() {
        return delivered;
    }

    public void setDelivered(String delivered) {
        this.delivered = delivered;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }
}
