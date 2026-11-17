package edu.sjsu.cmpe172.tutoring.ai;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import edu.sjsu.cmpe172.tutoring.dto.AppointmentSummary;
import edu.sjsu.cmpe172.tutoring.dto.NotificationRequest;
import edu.sjsu.cmpe172.tutoring.gateway.NotificationGateway;
import edu.sjsu.cmpe172.tutoring.service.AppointmentService;

/**
 * Agent 2: autonomous. it finds upcoming appointments that have not been reminded,
 * asks the LLM to write reminder, and sends through notification service.
 */
@Component
public class ReminderAgent {

    private static final Logger log = LoggerFactory.getLogger(ReminderAgent.class);

    private final AppointmentService appointmentService;
    private final NotificationGateway gateway;
    private final ChatClient chatClient;
    private final int windowHours;

    public ReminderAgent(AppointmentService appointmentService,
                         NotificationGateway gateway,
                         ChatClient.Builder builder,
                         @Value("${reminder.window-hours:24}") int windowHours) {
        this.appointmentService = appointmentService;
        this.gateway = gateway;
        this.chatClient = builder.build();
        this.windowHours = windowHours;
    }

    // no user prompt needed. "-" disables it (used in tests).
    @Scheduled(cron = "${reminder.cron:0 0 * * * *}")
    public void scheduledRun() {
        int sent = runOnce();
        log.info("Scheduled reminder run finished: {} reminders sent", sent);
    }

    /** Also called by the manual trigger endpoint. Returns how many reminders were sent. */
    public int runOnce() {
        List<AppointmentSummary> due = appointmentService.getDueReminders(windowHours);
        log.info("Reminder agent found {} appointments in the next {} hours", due.size(), windowHours);

        int sent = 0;
        for (AppointmentSummary a : due) {
            String text = writeReminder(a);
            gateway.send(new NotificationRequest(a.customerName(), "APPOINTMENT_REMINDER", text, "reminder-" + a.id()));
            appointmentService.markReminderSent(a.id());   // same appointment is not reminded twice
            sent++;
        }
        return sent;
    }

    private String writeReminder(AppointmentSummary a) {
        String prompt = ("Write a reminder of at most 40 words for the student %s about their tutoring "
                + "session: service %s with tutor %s, starting at %s. Plain text only, no markdown.")
                .formatted(a.customerName(), a.serviceName(), a.providerName(), a.startAt());
        try {
            return chatClient.prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.warn("LLM unavailable, using a template reminder for appointment {}: {}", a.id(), e.getMessage());
            return "Reminder: your " + a.serviceName() + " session with " + a.providerName()
                    + " starts at " + a.startAt() + ".";
        }
    }
}