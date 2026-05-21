package com.example.usermicroservice.service;

import com.example.usermicroservice.model.*;
import com.example.usermicroservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AlertService alertService;

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public List<Ticket> getTicketsByUser(String userId) {
        return ticketRepository.findByUserId(userId);
    }

    public Optional<Ticket> getTicketById(String id) {
        return ticketRepository.findById(id);
    }

    public List<Ticket> getTicketsForTechnician(String techId) {
        return ticketRepository.findByAssignedToOrUserId(techId, techId);
    }

    private User findBestTechnician() {
        List<User> technicians = userRepository.findByRole(Role.TECHNICIAN);
        if (technicians.isEmpty()) return null;

        User bestTech = null;
        long minWorkload = Long.MAX_VALUE;

        List<String> activeTicketStatuses = List.of("Open", "In Progress", "Waiting", "Testing");
        List<String> activeTaskStatuses = List.of("Pending", "In Progress", "Paused");

        for (User tech : technicians) {
            long ticketCount = ticketRepository.countByAssignedToAndStatusIn(tech.getId(), activeTicketStatuses);
            long taskCount = taskRepository.countByAssignedToAndStatusIn(tech.getId(), activeTaskStatuses);
            
            // Workload score: prioritize fewer open tickets and fewer schedule tasks
            long workload = (ticketCount * 10) + taskCount;

            if (workload < minWorkload) {
                minWorkload = workload;
                bestTech = tech;
            }
        }
        return bestTech;
    }

    public Ticket createTicket(Ticket ticket) {
        ticket.prePersist();
        
        // Auto-assign best technician if not already assigned
        if (ticket.getAssignedTo() == null || ticket.getAssignedTo().isEmpty()) {
            User bestTech = findBestTechnician();
            if (bestTech != null) {
                ticket.setAssignedTo(bestTech.getId());
                ticket.setTechnicianName(bestTech.getFirstName() + " " + bestTech.getLastName());
            }
        } else if (ticket.getTechnicianName() == null || ticket.getTechnicianName().isEmpty()) {
            // If ID is provided but Name is missing, fetch it
            userRepository.findById(ticket.getAssignedTo()).ifPresent(u -> 
                ticket.setTechnicianName(u.getFirstName() + " " + u.getLastName()));
        }

        Ticket saved = ticketRepository.save(ticket);
        
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().isEmpty()) {
            notificationService.createNotification(
                "Ticket Assigned",
                "Assignment for: '" + saved.getTitle() + "'. Assigned Technician: " + saved.getTechnicianName(),
                "INFO", "TICKET", saved.getId(), saved.getAssignedTo(), null
            );
        } else {
            // Unassigned ticket: broadcast to all TECHNICIANs
            notificationService.createNotification(
                "New Unassigned Ticket",
                "A new ticket has been created and needs assignment: '" + saved.getTitle() + "'",
                "INFO", "TICKET", saved.getId(), null, "TECHNICIAN"
            );
        }

        // Notify Creator
        if (saved.getUserId() != null) {
            notificationService.createNotification(
                "Ticket Created Successfully",
                "Your ticket '" + saved.getTitle() + "' has been submitted and assigned to " + (saved.getTechnicianName() != null ? saved.getTechnicianName() : "a technician") + ".",
                "SUCCESS", "TICKET", saved.getId(), saved.getUserId(), null
            );
        }
        
        return saved;
    }

    public Ticket updateTicket(String id, Ticket ticketDetails) {
        return ticketRepository.findById(id).map(ticket -> {
            String oldStatus = ticket.getStatus();
            String oldAssignedTo = ticket.getAssignedTo();
            
            ticket.setTitle(ticketDetails.getTitle());
            ticket.setDescription(ticketDetails.getDescription());
            ticket.setCategory(ticketDetails.getCategory());
            ticket.setPriority(ticketDetails.getPriority());
            ticket.setStatus(ticketDetails.getStatus());
            
            // Handle assignment change
            if (ticketDetails.getAssignedTo() != null && !ticketDetails.getAssignedTo().equals(oldAssignedTo)) {
                ticket.setAssignedTo(ticketDetails.getAssignedTo());
                // Update technician name as well
                userRepository.findById(ticketDetails.getAssignedTo()).ifPresent(u -> 
                    ticket.setTechnicianName(u.getFirstName() + " " + u.getLastName()));
            } else if (ticketDetails.getAssignedTo() == null) {
                ticket.setAssignedTo(null);
                ticket.setTechnicianName(null);
            }
            
            ticket.setEquipmentName(ticketDetails.getEquipmentName());
            ticket.setDeadline(ticketDetails.getDeadline());
            ticket.setAttachments(ticketDetails.getAttachments());
            ticket.setUserName(ticketDetails.getUserName());
            ticket.setUserRole(ticketDetails.getUserRole());
            ticket.setWorkNote(ticketDetails.getWorkNote());
            ticket.setRepairTasks(ticketDetails.getRepairTasks());
            ticket.setPartsUsed(ticketDetails.getPartsUsed());
            ticket.setPartsInstalled(ticketDetails.getPartsInstalled());
            if (ticketDetails.getDiagnosisResult() != null) {
                ticket.setDiagnosisResult(ticketDetails.getDiagnosisResult());
            }
            if (ticketDetails.getValidationSummary() != null) {
                ticket.setValidationSummary(ticketDetails.getValidationSummary());
            }
            if (ticketDetails.getEquipmentId() != null && !ticketDetails.getEquipmentId().isEmpty()) {
                ticket.setEquipmentId(ticketDetails.getEquipmentId());
            }
            ticket.preUpdate();
            
            Ticket updated = ticketRepository.save(ticket);
            
            // Notify if assignment changed
            if (updated.getAssignedTo() != null && !updated.getAssignedTo().equals(oldAssignedTo)) {
                notificationService.createNotification(
                    "New Ticket Assignment",
                    "A ticket has been assigned to you: " + updated.getTitle(),
                    "INFO", "TICKET", updated.getId(), updated.getAssignedTo(), null
                );
            }
            
            // Notify if status changed
            if (updated.getStatus() != null && !updated.getStatus().equals(oldStatus)) {
                String statusMsg = "The status of ticket '" + updated.getTitle() + "' was updated to: " + updated.getStatus();
                
                // Notify assigned person
                if (updated.getAssignedTo() != null) {
                    notificationService.createNotification("Ticket Status Update", statusMsg, "INFO", "TICKET", updated.getId(), updated.getAssignedTo(), null);
                }
                
                // Notify creator if different from assigned
                if (updated.getUserId() != null && !updated.getUserId().equals(updated.getAssignedTo())) {
                    notificationService.createNotification("Ticket Status Update", statusMsg, "INFO", "TICKET", updated.getId(), updated.getUserId(), null);
                }

                // If cancelled, alert STOCK_MANAGER
                if ("Cancelled".equalsIgnoreCase(updated.getStatus())) {
                    alertService.createOrUpdateAlert(
                        "TICKET_CANCELLED_" + updated.getId(),
                        "SYSTEM",
                        "HIGH",
                        "ROLE",
                        "STOCK_MANAGER",
                        "Ticket Cancelled",
                        "The ticket '" + updated.getTitle() + "' for equipment '" + updated.getEquipmentName() + "' was cancelled."
                    );
                }

                // If overdue, trigger alert
                if ("Overdue".equalsIgnoreCase(updated.getStatus())) {
                    alertService.createOrUpdateAlert(
                        "TICKET_OVERDUE_" + updated.getId(),
                        "TICKET_OVERDUE",
                        "HIGH",
                        "ROLE",
                        "STOCK_MANAGER",
                        "Ticket Overdue: " + updated.getTitle(),
                        "Ticket ID " + updated.getId() + " has been marked as overdue."
                    );
                }

                // If resolved or closed, resolve the overdue alert if it exists
                if ("Resolved".equalsIgnoreCase(updated.getStatus()) || "Closed".equalsIgnoreCase(updated.getStatus())) {
                    alertService.resolveAlert("TICKET_OVERDUE_" + updated.getId());
                }
            }
            
            return updated;
        }).orElseThrow(() -> new RuntimeException("Ticket not found with id " + id));
    }

    public void deleteTicket(String id) {
        ticketRepository.findById(id).ifPresent(ticket -> {
            String msg = "Ticket '" + ticket.getTitle() + "' has been deleted.";
            if (ticket.getUserId() != null) {
                notificationService.createNotification("Ticket Deleted", msg, "INFO", "TICKET", id, ticket.getUserId(), null);
            }
            if (ticket.getAssignedTo() != null && !ticket.getAssignedTo().equals(ticket.getUserId())) {
                notificationService.createNotification("Ticket Deleted", msg, "INFO", "TICKET", id, ticket.getAssignedTo(), null);
            }
        });
        ticketRepository.deleteById(id);
    }
}
