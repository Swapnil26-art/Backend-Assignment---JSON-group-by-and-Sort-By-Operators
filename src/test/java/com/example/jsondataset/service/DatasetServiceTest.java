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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetServiceTest {

    @Mock
    private DatasetRepository datasetRepository;

    private ObjectMapper objectMapper;

    private DatasetService datasetService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        datasetService = new DatasetService(datasetRepository, objectMapper);
    }

    private DatasetRecord record(String datasetName, long id, String json) {
        DatasetRecord r = new DatasetRecord(datasetName, json);
        r.setId(id);
        return r;
    }

    @Test
    void insertRecord_shouldPersistAndReturnResponse() {
        JsonNode data = objectMapper.createObjectNode()
                .put("id", 1)
                .put("name", "John Doe")
                .put("age", 30)
                .put("department", "Engineering");
        DatasetRecord saved = new DatasetRecord("employee_dataset", data.toString());
        saved.setId(1L);
        when(datasetRepository.save(any(DatasetRecord.class))).thenReturn(saved);

        InsertResponse response = datasetService.insertRecord("employee_dataset", data);

        assertEquals("Record added successfully", response.getMessage());
        assertEquals("employee_dataset", response.getDataset());
        assertEquals(1L, response.getRecordId());
    }

    @Test
    void insertRecord_withEmptyBody_shouldThrowInvalidRequest() {
        JsonNode empty = objectMapper.createObjectNode();
        assertThrows(InvalidRequestException.class,
                () -> datasetService.insertRecord("employee_dataset", empty));
    }

    @Test
    void groupBy_shouldGroupRecordsByField() throws Exception {
        List<DatasetRecord> records = List.of(
                record("employee_dataset", 1L,
                        "{\"id\":1,\"name\":\"John\",\"age\":30,\"department\":\"Engineering\"}"),
                record("employee_dataset", 2L,
                        "{\"id\":2,\"name\":\"Jane\",\"age\":25,\"department\":\"Engineering\"}"),
                record("employee_dataset", 3L,
                        "{\"id\":3,\"name\":\"Alice\",\"age\":28,\"department\":\"Marketing\"}")
        );
        when(datasetRepository.existsByDatasetName("employee_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("employee_dataset")).thenReturn(records);

        GroupByResponse response = datasetService.groupBy("employee_dataset", "department");

        Map<String, List<Object>> grouped = response.getGroupedRecords();
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("Engineering").size());
        assertEquals(1, grouped.get("Marketing").size());
    }

    @Test
    void groupBy_withMissingField_shouldThrowInvalidField() {
        List<DatasetRecord> records = List.of(
                record("employee_dataset", 1L,
                        "{\"id\":1,\"name\":\"John\",\"age\":30,\"department\":\"Engineering\"}")
        );
        when(datasetRepository.existsByDatasetName("employee_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("employee_dataset")).thenReturn(records);

        assertThrows(InvalidFieldException.class,
                () -> datasetService.groupBy("employee_dataset", "nonexistentField"));
    }

    @Test
    void sortBy_ascending_shouldSortByNumericField() {
        List<DatasetRecord> records = List.of(
                record("employee_dataset", 1L,
                        "{\"id\":1,\"name\":\"John\",\"age\":30,\"department\":\"Engineering\"}"),
                record("employee_dataset", 2L,
                        "{\"id\":2,\"name\":\"Jane\",\"age\":25,\"department\":\"Engineering\"}"),
                record("employee_dataset", 3L,
                        "{\"id\":3,\"name\":\"Alice\",\"age\":28,\"department\":\"Marketing\"}")
        );
        when(datasetRepository.existsByDatasetName("employee_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("employee_dataset")).thenReturn(records);

        SortByResponse response = datasetService.sortBy("employee_dataset", "age", "asc");

        assertNotNull(response.getSortedRecords());
        assertEquals(3, response.getSortedRecords().size());
        Map<String, Object> first = (Map<String, Object>) response.getSortedRecords().get(0);
        assertEquals(25, ((Number) first.get("age")).intValue());
    }

    @Test
    void sortBy_descending_shouldSortByNumericField() {
        List<DatasetRecord> records = List.of(
                record("employee_dataset", 1L,
                        "{\"id\":1,\"name\":\"John\",\"age\":30,\"department\":\"Engineering\"}"),
                record("employee_dataset", 2L,
                        "{\"id\":2,\"name\":\"Jane\",\"age\":25,\"department\":\"Engineering\"}"),
                record("employee_dataset", 3L,
                        "{\"id\":3,\"name\":\"Alice\",\"age\":28,\"department\":\"Marketing\"}")
        );
        when(datasetRepository.existsByDatasetName("employee_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("employee_dataset")).thenReturn(records);

        SortByResponse response = datasetService.sortBy("employee_dataset", "age", "desc");

        assertEquals(3, response.getSortedRecords().size());
        Map<String, Object> first = (Map<String, Object>) response.getSortedRecords().get(0);
        assertEquals(30, ((Number) first.get("age")).intValue());
    }

    @Test
    void sortBy_withMissingField_shouldThrowInvalidField() {
        List<DatasetRecord> records = List.of(
                record("employee_dataset", 1L,
                        "{\"id\":1,\"name\":\"John\",\"age\":30}")
        );
        when(datasetRepository.existsByDatasetName("employee_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("employee_dataset")).thenReturn(records);

        assertThrows(InvalidFieldException.class,
                () -> datasetService.sortBy("employee_dataset", "nonexistentField", "asc"));
    }

    @Test
    void sortBy_invalidOrder_shouldThrowInvalidRequest() {
        assertThrows(InvalidRequestException.class,
                () -> datasetService.sortBy("employee_dataset", "age", "sideways"));
    }

    @Test
    void query_withNonExistentDataset_shouldThrowNotFound() {
        when(datasetRepository.existsByDatasetName("ghost")).thenReturn(false);

        assertThrows(DatasetNotFoundException.class,
                () -> datasetService.groupBy("ghost", "department"));
    }

    @Test
    void query_withEmptyDataset_shouldThrowEmptyDataset() {
        when(datasetRepository.existsByDatasetName("empty_dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("empty_dataset")).thenReturn(List.of());

        assertThrows(EmptyDatasetException.class,
                () -> datasetService.sortBy("empty_dataset", "age", "asc"));
    }

    @Test
    void sortBy_shouldSupportStringFieldsAscending() {
        List<DatasetRecord> records = List.of(
                record("dataset", 1L, "{\"id\":1,\"name\":\"Zoe\"}"),
                record("dataset", 2L, "{\"id\":2,\"name\":\"Alice\"}")
        );
        when(datasetRepository.existsByDatasetName("dataset")).thenReturn(true);
        when(datasetRepository.findByDatasetNameOrderByIdAsc("dataset")).thenReturn(records);

        SortByResponse response = datasetService.sortBy("dataset", "name", "asc");

        Map<String, Object> first = (Map<String, Object>) response.getSortedRecords().get(0);
        assertEquals("Alice", first.get("name"));
    }
}
