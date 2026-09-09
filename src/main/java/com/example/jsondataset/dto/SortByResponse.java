package com.example.jsondataset.dto;

import java.util.List;

public class SortByResponse {

    private List<Object> sortedRecords;

    public SortByResponse() {
    }

    public SortByResponse(List<Object> sortedRecords) {
        this.sortedRecords = sortedRecords;
    }

    public List<Object> getSortedRecords() {
        return sortedRecords;
    }

    public void setSortedRecords(List<Object> sortedRecords) {
        this.sortedRecords = sortedRecords;
    }
}
