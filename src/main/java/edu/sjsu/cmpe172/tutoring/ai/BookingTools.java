package edu.sjsu.cmpe172.tutoring.ai;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import edu.sjsu.cmpe172.tutoring.dto.AppointmentSummary;
import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.service.AppointmentService;
import edu.sjsu.cmpe172.tutoring.service.BookingFacade;
import edu.sjsu.cmpe172.tutoring.service.SlotService;

// Tools the conversation agent can call
@Component
public class BookingTools {

    private final SlotService slotService;
    private final AppointmentService appointmentService;
    private final BookingFacade bookingFacade;
    private final PendingActionStore pending;

    public BookingTools(SlotService slotService, AppointmentService appointmentService,
                        BookingFacade bookingFacade, PendingActionStore pending) {
        this.slotService = slotService;
        this.appointmentService = appointmentService;
        this.bookingFacade = bookingFacade;
        this.pending = pending;
    }

    //read only tools

    @Tool(description = "List available tutoring slots, earliest first (maximum 10). "
            + "Optionally filter to one day. Use this to find the earliest slot or to offer alternatives.")
    public String checkAvailability(
            @ToolParam(description = "Day in yyyy-MM-dd format, or empty for all upcoming days", required = false) String date) {
        LocalDate day = null;
        if (date != null && !date.isBlank()) {
            try {
                day = LocalDate.parse(date.trim());
            } catch (DateTimeParseException e) {
                return "Invalid date. Use the format yyyy-MM-dd.";
            }
        }
        List<SlotDto> slots = slotService.getAvailableSlots(null, null, day, 10, 0);
        if (slots.isEmpty()) {
            return day == null ? "No available slots." : "No available slots on " + day + ".";
        }
        StringBuilder sb = new StringBuilder();
        for (SlotDto s : slots) {
            sb.append("slotId=").append(s.id())
              .append(" | tutor=").append(s.providerName())
              .append(" | service=").append(s.serviceName())
              .append(" | price=$").append(String.format("%.2f", s.price()))
              .append(" | start=").append(s.startAt())
              .append(" | end=").append(s.endAt())
              .append("\n");
        }
        return sb.toString();
    }

    @Tool(description = "List the logged in customer's own appointments. view is 'upcoming' or 'history'.")
    public String listMyAppointments(
            @ToolParam(description = "upcoming or history", required = false) String view,
            ToolContext context) {
        Long userId = userId(context);
        boolean upcoming = !"history".equalsIgnoreCase(view);
        List<AppointmentSummary> list = appointmentService.getCustomerAppointments(userId, upcoming);
        if (list.isEmpty()) {
            return upcoming ? "You have no upcoming appointments." : "You have no past appointments.";
        }
        StringBuilder sb = new StringBuilder();
        for (AppointmentSummary a : list) {
            sb.append("appointmentId=").append(a.id())
              .append(" | tutor=").append(a.providerName())
              .append(" | service=").append(a.serviceName())
              .append(" | start=").append(a.startAt())
              .append(" | status=").append(a.status())
              .append("\n");
        }
        return sb.toString();
    }

    // write tools PREPARES, nothing is saved

    @Tool(description = "Step 1 of booking. Prepares a booking for the slot but does NOT book it. "
            + "Show the returned summary to the user and ask them to confirm.")
    public String bookSlot(
            @ToolParam(description = "slotId returned by checkAvailability") Long slotId,
            ToolContext context) {
        Long userId = userId(context);
        try {
            SlotDto s = slotService.getSlotById(slotId);
            String summary = "Book slot " + s.id() + ": tutor " + s.providerName() + ", " + s.serviceName()
                    + ", $" + String.format("%.2f", s.price()) + ", " + s.startAt() + " to " + s.endAt();
            pending.stage(userId, "BOOK", slotId, summary);
            return "PREPARED, NOT BOOKED YET. Show this to the user and ask them to confirm: " + summary
                    + ". Only after the user confirms in their NEXT message may you call confirmAction.";
        } catch (ResourceNotFoundException e) {
            return "That slot does not exist. Use checkAvailability to find a valid slot.";
        }
    }

    @Tool(description = "Step 1 of cancelling. Prepares the cancellation but does NOT cancel. "
            + "Show the returned summary to the user and ask them to confirm.")
    public String cancelAppointment(
            @ToolParam(description = "appointmentId returned by listMyAppointments") Long appointmentId,
            ToolContext context) {
        Long userId = userId(context);
        try {
            Appointment a = appointmentService.getAppointment(userId, appointmentId);   // also checks ownership
            if (!"BOOKED".equals(a.status())) {
                return "That appointment is " + a.status() + " and cannot be cancelled.";
            }
            String summary = "Cancel appointment " + a.id() + " (slot " + a.slotId() + ")";
            pending.stage(userId, "CANCEL", appointmentId, summary);
            return "PREPARED, NOT CANCELLED YET. Show this to the user and ask them to confirm: " + summary
                    + ". Only after the user confirms in their NEXT message may you call confirmAction.";
        } catch (ResourceNotFoundException e) {
            return "You have no appointment with that id. Use listMyAppointments to see yours.";
        }
    }

    // write tool

    @Tool(description = "Step 2. Commits the prepared booking or cancellation. "
            + "Call this ONLY after the user has explicitly confirmed in a message AFTER the summary was shown.")
    public String confirmAction(ToolContext context) {
        Long userId = userId(context);
        String username = (String) context.getContext().get("username");

        PendingActionStore.Pending p = pending.get(userId);
        if (p == null) {
            return "Nothing is waiting for confirmation.";
        }
        // the confirmation must come in a later user message than the one that prepared the action
        if (pending.currentTurn(userId) <= p.turn()) {
            return "REFUSED: the user has not confirmed yet. Show the summary, ask for confirmation, and wait for reply.";
        }
        pending.clear(userId);

        try {
            if ("BOOK".equals(p.type())) {
                // BookingFacade -> AppointmentService.bookSlot (@Transactional): version check + unique index
                Appointment a = bookingFacade.book(userId, username, p.targetId());
                return "SUCCESS: booked. appointmentId=" + a.id() + ", slotId=" + a.slotId() + ".";
            } else {
                bookingFacade.cancel(userId, username, p.targetId());
                return "SUCCESS: appointment " + p.targetId() + " was cancelled.";
            }
        } catch (SlotAlreadyBookedException e) {
            return "FAILED: that slot was just taken by someone else. Apologise and offer alternatives using checkAvailability.";
        } catch (ResourceNotFoundException | IllegalArgumentException e) {
            return "FAILED: " + e.getMessage();
        }
    }

    private static Long userId(ToolContext context) {
        return (Long) context.getContext().get("userId");
    }
}