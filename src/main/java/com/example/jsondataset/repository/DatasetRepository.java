package com.example.jsondataset.repository;

import com.example.jsondataset.entity.DatasetRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<DatasetRecord, Long> {

    List<DatasetRecord> findByDatasetNameOrderByIdAsc(String datasetName);

    boolean existsByDatasetName(String datasetName);
}
