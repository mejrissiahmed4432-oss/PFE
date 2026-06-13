package com.example.usermicroservice.controller;

import com.example.usermicroservice.config.BlockchainTraceable;
import com.example.usermicroservice.model.Ticket;
import com.example.usermicroservice.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketService.getAllTickets();
    }

    @GetMapping("/user/{userId}")
    public List<Ticket> getTicketsByUser(@PathVariable String userId) {
        return ticketService.getTicketsByUser(userId);
    }

    @GetMapping("/technician/{techId}")
    public List<Ticket> getTicketsForTechnician(@PathVariable String techId) {
        return ticketService.getTicketsForTechnician(techId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable String id) {
        return ticketService.getTicketById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @BlockchainTraceable(action = "Create Ticket")
    @PostMapping
    public Ticket createTicket(@RequestBody Ticket ticket) {
        return ticketService.createTicket(ticket);
    }

    @BlockchainTraceable(action = "Update Ticket")
    @PutMapping("/{id}")
    public ResponseEntity<Ticket> updateTicket(@PathVariable String id, @RequestBody Ticket ticketDetails) {
        try {
            return ResponseEntity.ok(ticketService.updateTicket(id, ticketDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @BlockchainTraceable(action = "Delete Ticket")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable String id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }
}

