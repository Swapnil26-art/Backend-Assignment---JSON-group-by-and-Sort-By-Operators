package com.example.jsondataset.dto;

public class InsertResponse {

    private String message;
    private String dataset;
    private Long recordId;

    public InsertResponse() {
    }

    public InsertResponse(String message, String dataset, Long recordId) {
        this.message = message;
        this.dataset = dataset;
        this.recordId = recordId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }
}
