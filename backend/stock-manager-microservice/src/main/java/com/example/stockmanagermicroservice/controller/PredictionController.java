package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.EquipmentPrediction;
import com.example.stockmanagermicroservice.repository.EquipmentPredictionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring/predictions")
@CrossOrigin(origins = "*")
public class PredictionController {

    @Autowired
    private EquipmentPredictionRepository predictionRepository;

    @GetMapping
    public ResponseEntity<List<EquipmentPrediction>> getAllPredictions() {
        return ResponseEntity.ok(predictionRepository.findAll());
    }
}
