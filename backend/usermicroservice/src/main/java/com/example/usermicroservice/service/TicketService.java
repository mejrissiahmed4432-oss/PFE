package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Ticket;
import com.example.usermicroservice.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsByUser(String userId) {
        return ticketRepository.findByUserId(userId);
    }

    public Optional<Ticket> getTicketById(String id) {
        return ticketRepository.findById(id);
    }

    public Ticket createTicket(Ticket ticket) {
        ticket.prePersist();
        return ticketRepository.save(ticket);
    }

    public Ticket updateTicket(String id, Ticket ticketDetails) {
        return ticketRepository.findById(id).map(ticket -> {
            ticket.setTitle(ticketDetails.getTitle());
            ticket.setDescription(ticketDetails.getDescription());
            ticket.setCategory(ticketDetails.getCategory());
            ticket.setPriority(ticketDetails.getPriority());
            ticket.setStatus(ticketDetails.getStatus());
            ticket.preUpdate();
            return ticketRepository.save(ticket);
        }).orElseThrow(() -> new RuntimeException("Ticket not found with id " + id));
    }

    public void deleteTicket(String id) {
        ticketRepository.deleteById(id);
    }
}
