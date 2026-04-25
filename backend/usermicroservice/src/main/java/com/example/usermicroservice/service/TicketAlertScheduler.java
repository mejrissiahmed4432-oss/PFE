package com.example.usermicroservice.service;

import com.example.usermicroservice.model.AlertPriority;
import com.example.usermicroservice.model.AlertType;
import com.example.usermicroservice.model.TargetType;
import com.example.usermicroservice.model.Ticket;
import com.example.usermicroservice.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TicketAlertScheduler {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AlertService alertService;

    // Run every hour
    @Scheduled(cron = "0 0 * * * *")
    public void checkOverdueTickets() {
        System.out.println("[TicketAlertScheduler] Checking for overdue tickets...");
        List<Ticket> allTickets = ticketRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Ticket ticket : allTickets) {
            String status = ticket.getStatus();
            String key = "TICKET_OVERDUE_" + ticket.getId();

            if (status != null && (status.equalsIgnoreCase("Resolved") || status.equalsIgnoreCase("Closed"))) {
                // If it was overdue but is now resolved, resolve the alert
                alertService.resolveAlert(key);
                continue;
            }

            String deadlineStr = ticket.getDeadline();
            if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
                try {
                    // Assuming deadline is ISO format (YYYY-MM-DD) or similar parseable by LocalDate.
                    // Adjust if it includes time. We'll take the first 10 chars.
                    String datePart = deadlineStr.length() >= 10 ? deadlineStr.substring(0, 10) : deadlineStr;
                    LocalDate deadline = LocalDate.parse(datePart);

                    if (deadline.isBefore(today)) {
                        // Ticket is overdue
                        String title = "Overdue Ticket: " + ticket.getTitle();
                        String message = "Ticket ID " + ticket.getId() + " is overdue since " + deadline + ".";
                        
                        alertService.createOrUpdateAlert(
                            key,
                            "TICKET_OVERDUE",
                            "HIGH",
                            "ROLE",
                            "STOCK_MANAGER", // Or whatever role handles tickets
                            title,
                            message
                        );
                    } else {
                        // Not overdue, resolve if there was an active alert
                        alertService.resolveAlert(key);
                    }
                } catch (DateTimeParseException e) {
                    System.err.println("Failed to parse deadline for ticket " + ticket.getId() + ": " + deadlineStr);
                }
            }
        }
    }
}
