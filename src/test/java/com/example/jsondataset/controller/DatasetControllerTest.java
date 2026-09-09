package com.example.jsondataset.controller;

import com.example.jsondataset.dto.GroupByResponse;
import com.example.jsondataset.dto.InsertResponse;
import com.example.jsondataset.dto.SortByResponse;
import com.example.jsondataset.exception.DatasetNotFoundException;
import com.example.jsondataset.exception.InvalidFieldException;
import com.example.jsondataset.exception.InvalidRequestException;
import com.example.jsondataset.service.DatasetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DatasetService datasetService;

    @Test
    void insertRecord_shouldReturnCreated() throws Exception {
        InsertResponse response = new InsertResponse("Record added successfully",
                "employee_dataset", 1L);
        when(datasetService.insertRecord(anyString(), any())).thenReturn(response);

        String body = "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}";

        mockMvc.perform(post("/api/dataset/employee_dataset/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Record added successfully"))
                .andExpect(jsonPath("$.dataset").value("employee_dataset"))
                .andExpect(jsonPath("$.recordId").value(1));
    }

    @Test
    void query_withGroupByShouldReturnGroupedRecords() throws Exception {
        Map<String, List<Object>> grouped = Map.of(
                "Engineering", List.of(Map.of("id", 1, "department", "Engineering")),
                "Marketing", List.of(Map.of("id", 3, "department", "Marketing"))
        );
        GroupByResponse response = new GroupByResponse(grouped);
        when(datasetService.groupBy("employee_dataset", "department")).thenReturn(response);

        mockMvc.perform(get("/api/dataset/employee_dataset/query")
                        .param("groupBy", "department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords.Engineering").isArray())
                .andExpect(jsonPath("$.groupedRecords.Marketing").isArray());
    }

    @Test
    void query_withSortByShouldReturnSortedRecords() throws Exception {
        List<Object> sorted = List.of(
                Map.of("id", 2, "age", 25),
                Map.of("id", 3, "age", 28),
                Map.of("id", 1, "age", 30)
        );
        SortByResponse response = new SortByResponse(sorted);
        when(datasetService.sortBy("employee_dataset", "age", "asc")).thenReturn(response);

        mockMvc.perform(get("/api/dataset/employee_dataset/query")
                        .param("sortBy", "age")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[0].age").value(25))
                .andExpect(jsonPath("$.sortedRecords[2].age").value(30));
    }

    @Test
    void query_withoutParametersShouldReturnBadRequest() throws Exception {
        when(datasetService.groupBy(anyString(), any()))
                .thenThrow(new InvalidRequestException("Provide either 'groupBy' or 'sortBy' query parameter"));

        mockMvc.perform(get("/api/dataset/employee_dataset/query"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void query_withNonExistentDatasetShouldReturnNotFound() throws Exception {
        when(datasetService.groupBy("ghost", "department"))
                .thenThrow(new DatasetNotFoundException("Dataset 'ghost' does not exist"));

        mockMvc.perform(get("/api/dataset/ghost/query")
                        .param("groupBy", "department"))
                .andExpect(status().isNotFound());
    }

    @Test
    void query_withInvalidFieldShouldReturnBadRequest() throws Exception {
        when(datasetService.groupBy("employee_dataset", "nonexistent"))
                .thenThrow(new InvalidFieldException("Field 'nonexistent' does not exist in the dataset records"));

        mockMvc.perform(get("/api/dataset/employee_dataset/query")
                        .param("groupBy", "nonexistent"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insertRecord_withMalformedJsonShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/dataset/employee_dataset/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{malformed"))
                .andExpect(status().isBadRequest());
    }
}
