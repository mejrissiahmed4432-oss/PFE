package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.Equipment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.mongodb.repository.Query;

@Repository
public interface EquipmentRepository extends MongoRepository<Equipment, String> {
    List<Equipment> findBySupplier(String supplier);
    List<Equipment> findBySupplierId(String supplierId);
    List<Equipment> findByShelfId(String shelfId);
    List<Equipment> findByCategory(String category);

    // Optimized queries for list views (exclude heavy base64 file data)
    @Query(value = "{}", fields = "{ 'invoiceFileData': 0, 'warrantyFileData': 0 }")
    List<Equipment> findAllExcludingFiles();

    @Query(value = "{ 'shelfId' : ?0 }", fields = "{ 'invoiceFileData': 0, 'warrantyFileData': 0 }")
    List<Equipment> findByShelfIdExcludingFiles(String shelfId);

    @Query(value = "{ '_id' : ?0 }", fields = "{ 'invoiceFileData': 0, 'warrantyFileData': 0 }")
    java.util.Optional<Equipment> findByIdExcludingFiles(String id);
    boolean existsByCategory(String category);
    boolean existsByTypeIgnoreCase(String type);
    boolean existsBySerialNumber(String serialNumber);
    boolean existsBySerialNumberAndIdNot(String serialNumber, String id);
    boolean existsByShelfId(String shelfId);
    boolean existsBySupplierId(String supplierId);
    List<Equipment> findByTypeIgnoreCase(String type);

    // Optimized query for type update — only fetches id + qrCode, skips heavy file data
    @Query(value = "{ 'type' : { $regex: ?0, $options: 'i' } }", fields = "{ 'invoiceFileData': 0, 'warrantyFileData': 0, 'icon': 0, 'specification': 0 }")
    List<Equipment> findByTypeIgnoreCaseExcludingFiles(String type);
}
