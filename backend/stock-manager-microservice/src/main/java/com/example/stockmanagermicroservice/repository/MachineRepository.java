package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Machine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository extends MongoRepository<Machine, String> {

    Optional<Machine> findBySerial(String serial);

    List<Machine> findBySerialIn(List<String> serials);
}
