package edu.sjsu.cmpe172.tutoring.ai;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

/** Agent 1: conversational, tool-calling booking assistant. */
@Service
public class BookingAgent {

    private static final String SYSTEM = """
        The current date is %s (%s)
        Use only the provided tools.
        To find available times, use checkAvailability. If the earliest time is desired,
        just omit the date in checkAvailability and select the first time.
        To book an appointment, use bookSlot. Give a summary of the booking to the user
        and ask for confirmation. Only call confirmAction if the user confirms in a follow-up message.
        Cancelling follows the same pattern. First use cancelAppointment, give a summary
        to the user and ask for confirmation. Use confirmAction to confirm in a follow-up message.
        If the slot is already booked or not available, inform the user and offer alternative times (from checkAvailability).
        Ask for missing information. Keep responses concise and plain text.
        """;

    private final ChatClient chatClient;
    private final BookingTools tools;
    private final PendingActionStore pending;

    public BookingAgent(ChatClient.Builder builder, BookingTools tools, PendingActionStore pending) {
        ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();   // multi-turn memory
        this.chatClient = builder
            .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
            .build();
        this.tools = tools;
        this.pending = pending;
    }

    public String chat(Long userId, String username, String message) {
        pending.nextTurn(userId);   // counts as new turn (used by confirm-before-write)

        LocalDate today = LocalDate.now();
        String system = SYSTEM.formatted(today, today.getDayOfWeek());

        return chatClient.prompt()
            .system(system)
            .user(message)
            .tools(tools)
            .toolContext(Map.of("userId", userId, "username", username))   // the agent acts as THIS user
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-" + userId))
            .call()
            .content();
    }
}