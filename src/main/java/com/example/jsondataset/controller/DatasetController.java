package com.example.jsondataset.controller;

import com.example.jsondataset.dto.GroupByResponse;
import com.example.jsondataset.dto.InsertResponse;
import com.example.jsondataset.dto.SortByResponse;
import com.example.jsondataset.exception.InvalidRequestException;
import com.example.jsondataset.service.DatasetService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dataset/{datasetName}")
public class DatasetController {

    private final DatasetService datasetService;

    public DatasetController(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @PostMapping("/record")
    @ResponseStatus(HttpStatus.CREATED)
    public InsertResponse insertRecord(@PathVariable String datasetName,
                                       @RequestBody JsonNode recordData) {
        return datasetService.insertRecord(datasetName, recordData);
    }

    @GetMapping("/query")
    public Object query(@PathVariable String datasetName,
                        @RequestParam(required = false) String groupBy,
                        @RequestParam(required = false) String sortBy,
                        @RequestParam(required = false, defaultValue = "asc") String order) {

        if (groupBy != null && !groupBy.isBlank()) {
            return datasetService.groupBy(datasetName, groupBy);
        }

        if (sortBy != null && !sortBy.isBlank()) {
            return datasetService.sortBy(datasetName, sortBy, order);
        }

        throw new InvalidRequestException(
                "Provide either 'groupBy' or 'sortBy' query parameter");
    }
}
