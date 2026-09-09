package com.example.jsondataset.dto;

import java.util.List;
import java.util.Map;

public class GroupByResponse {

    private Map<String, List<Object>> groupedRecords;

    public GroupByResponse() {
    }

    public GroupByResponse(Map<String, List<Object>> groupedRecords) {
        this.groupedRecords = groupedRecords;
    }

    public Map<String, List<Object>> getGroupedRecords() {
        return groupedRecords;
    }

    public void setGroupedRecords(Map<String, List<Object>> groupedRecords) {
        this.groupedRecords = groupedRecords;
    }
}
