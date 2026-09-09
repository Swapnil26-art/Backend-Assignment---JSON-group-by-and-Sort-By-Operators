package com.example.jsondataset.service;

import com.example.jsondataset.dto.GroupByResponse;
import com.example.jsondataset.dto.InsertResponse;
import com.example.jsondataset.dto.SortByResponse;
import com.example.jsondataset.entity.DatasetRecord;
import com.example.jsondataset.exception.DatasetNotFoundException;
import com.example.jsondataset.exception.EmptyDatasetException;
import com.example.jsondataset.exception.InvalidFieldException;
import com.example.jsondataset.exception.InvalidRequestException;
import com.example.jsondataset.repository.DatasetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatasetService {

    private static final String VALID_ORDER_ASC = "asc";
    private static final String VALID_ORDER_DESC = "desc";

    private final DatasetRepository datasetRepository;
    private final ObjectMapper objectMapper;

    public DatasetService(DatasetRepository datasetRepository, ObjectMapper objectMapper) {
        this.datasetRepository = datasetRepository;
        this.objectMapper = objectMapper;
    }

    public InsertResponse insertRecord(String datasetName, JsonNode recordData) {
        if (recordData == null || !recordData.isObject() || recordData.isEmpty()) {
            throw new InvalidRequestException("Request body must be a non-empty JSON object");
        }

        DatasetRecord record = new DatasetRecord(datasetName, recordData.toString());
        DatasetRecord saved = datasetRepository.save(record);

        return new InsertResponse("Record added successfully", datasetName, saved.getId());
    }

    public GroupByResponse groupBy(String datasetName, String field) {
        List<DatasetRecord> records = fetchRecordsForDataset(datasetName);
        Map<String, List<Object>> grouped = new LinkedHashMap<>();

        for (DatasetRecord record : records) {
            JsonNode json = readRecordData(record);
            JsonNode fieldValue = json.get(field);

            if (fieldValue == null || fieldValue.isNull()) {
                throw new InvalidFieldException("Field '" + field + "' does not exist in the dataset records");
            }

            String key = fieldValue.asText();
            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(objectMapper.convertValue(json, Object.class));
        }

        return new GroupByResponse(grouped);
    }

    public SortByResponse sortBy(String datasetName, String field, String order) {
        if (!VALID_ORDER_ASC.equals(order) && !VALID_ORDER_DESC.equals(order)) {
            throw new InvalidRequestException("Order must be either 'asc' or 'desc'");
        }

        List<DatasetRecord> records = fetchRecordsForDataset(datasetName);
        List<JsonNode> jsonRecords = new ArrayList<>();

        for (DatasetRecord record : records) {
            JsonNode json = readRecordData(record);
            JsonNode fieldValue = json.get(field);

            if (fieldValue == null || fieldValue.isNull()) {
                throw new InvalidFieldException("Field '" + field + "' does not exist in the dataset records");
            }

            jsonRecords.add(json);
        }

        Comparator<JsonNode> comparator = createFieldComparator(field);
        if (VALID_ORDER_DESC.equals(order)) {
            comparator = comparator.reversed();
        }

        jsonRecords.sort(comparator);

        List<Object> sorted = jsonRecords.stream()
                .map(json -> objectMapper.convertValue(json, Object.class))
                .toList();

        return new SortByResponse(sorted);
    }

    private List<DatasetRecord> fetchRecordsForDataset(String datasetName) {
        if (!datasetRepository.existsByDatasetName(datasetName)) {
            throw new DatasetNotFoundException("Dataset '" + datasetName + "' does not exist");
        }

        List<DatasetRecord> records = datasetRepository.findByDatasetNameOrderByIdAsc(datasetName);
        if (records.isEmpty()) {
            throw new EmptyDatasetException("Dataset '" + datasetName + "' has no records");
        }

        return records;
    }

    private JsonNode readRecordData(DatasetRecord record) {
        try {
            return objectMapper.readTree(record.getRecordData());
        } catch (Exception e) {
            throw new InvalidRequestException("Stored record data is not valid JSON");
        }
    }

    private Comparator<JsonNode> createFieldComparator(String field) {
        return (a, b) -> {
            JsonNode valueA = a.get(field);
            JsonNode valueB = b.get(field);

            if (valueA.isNumber() && valueB.isNumber()) {
                return Double.compare(valueA.asDouble(), valueB.asDouble());
            }
            return valueA.asText().compareTo(valueB.asText());
        };
    }
}
