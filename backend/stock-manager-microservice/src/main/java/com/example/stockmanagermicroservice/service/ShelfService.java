package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.ShelfRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfService {

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    public List<Shelf> getAllShelves() {
        return shelfRepository.findAll();
    }

    public Optional<Shelf> getShelfById(String id) {
        return shelfRepository.findById(id);
    }

    public List<Shelf> getShelvesByEquipmentType(String equipmentType) {
        return shelfRepository.findByEquipmentTypeIgnoreCase(equipmentType);
    }

    public Shelf createShelf(Shelf shelf) {
        updateShelfStatus(shelf);
        Shelf saved = shelfRepository.save(shelf);
        notificationService.createNotification("New Shelf Created: " + saved.getNb(),
                "Shelf " + saved.getNb() + " (" + saved.getEquipmentType() + ") has been added",
                "SUCCESS", "SHELF", saved.getId(), null, "STOCK_MANAGER");
        return saved;
    }

    public Shelf updateShelf(String id, Shelf shelfDetails) {
        return shelfRepository.findById(id).map(shelf -> {
            String oldNb = shelf.getNb();
            shelf.setNb(shelfDetails.getNb());
            shelf.setMaxQte(shelfDetails.getMaxQte());
            shelf.setMinQte(shelfDetails.getMinQte());
            shelf.setCurrentQte(shelfDetails.getCurrentQte());
            shelf.setEquipmentType(shelfDetails.getEquipmentType());
            updateShelfStatus(shelf);
            Shelf saved = shelfRepository.save(shelf);

            // Sync alerts if name changed (Deprecated - historical alerts act as point-in-time snapshot)

            notificationService.createNotification("Shelf Updated: " + saved.getNb(),
                    "Shelf " + saved.getNb() + " configuration has been modified",
                    "INFO", "SHELF", saved.getId(), null, "STOCK_MANAGER");
            return saved;
        }).orElseThrow(() -> new RuntimeException("Shelf not found with id: " + id));
    }

    @Autowired
    private EquipmentRepository equipmentRepository;

    public void deleteShelf(String id) {
        if (equipmentRepository.existsByShelfId(id)) {
            throw new IllegalStateException("Cannot delete shelf: Equipment is still placed in it.");
        }

        shelfRepository.findById(id).ifPresent(shelf -> {
            notificationService.createNotification("Shelf Deleted: " + shelf.getNb(),
                    "Shelf " + shelf.getNb() + " has been removed",
                    "INFO", "SHELF", id, null, "STOCK_MANAGER");
        });
        shelfRepository.deleteById(id);
    }

    public void updateShelfStatus(Shelf shelf) {
        if (shelf.getCurrentQte() == null)
            shelf.setCurrentQte(0);
        if (shelf.getMaxQte() == null)
            shelf.setMaxQte(0);
        if (shelf.getMinQte() == null)
            shelf.setMinQte(0);

        String oldStatus = shelf.getStatus();

        if (shelf.getCurrentQte() == 0) {
            shelf.setStatus("EMPTY");
        } else if (shelf.getCurrentQte() < shelf.getMinQte()) {
            shelf.setStatus("LOW");
        } else if (shelf.getCurrentQte() >= shelf.getMaxQte()) {
            shelf.setStatus("FULL");
        } else {
            shelf.setStatus("NORMAL");
        }

        // Generate System Alerts for stock issues
        if (shelf.getStatus() != null && !shelf.getStatus().equals(oldStatus)) {
            if ("LOW".equals(shelf.getStatus())) {
                alertService.createAlert("Low Stock Alert: Shelf " + shelf.getNb(),
                        "Shelf " + shelf.getNb() + " (" + shelf.getEquipmentType() + ") is running low on stock.",
                        "ERROR", "STOCK", shelf.getId(), "STOCK_MANAGER");
            } else if ("FULL".equals(shelf.getStatus())) {
                alertService.createAlert("Shelf Full: Shelf " + shelf.getNb(),
                        "Shelf " + shelf.getNb() + " (" + shelf.getEquipmentType() + ") has reached maximum capacity.",
                        "ERROR", "STOCK", shelf.getId(), "STOCK_MANAGER");
            } else if ("EMPTY".equals(shelf.getStatus())) {
                alertService.createAlert("Stock Empty Alert: Shelf " + shelf.getNb(),
                        "Shelf " + shelf.getNb() + " (" + shelf.getEquipmentType()
                                + ") is completely empty. restocking is needed.",
                        "ERROR", "STOCK", shelf.getId(), "STOCK_MANAGER");
            }
        }
    }
}
