package edu.sjsu.cmpe172.tutoring.service;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.sjsu.cmpe172.tutoring.dto.CreateSlotRequest;
import edu.sjsu.cmpe172.tutoring.dto.SlotResponse;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock SlotRepository slotRepository;
    @InjectMocks SlotService service;

    private static final LocalDateTime START = LocalDateTime.now().plusDays(3);
    private static final LocalDateTime END = START.plusHours(1);

    // adjust the argument order here if your CreateSlotRequest record differs
    private static CreateSlotRequest request(Long serviceId, LocalDateTime start, LocalDateTime end) {
        return new CreateSlotRequest(serviceId, start, end);
    }

    // ---- createSlot validation ----

    @Test
    void createSlot_missingTimes_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createSlot(2L, request(1L, null, null)));
    }

    @Test
    void createSlot_startNotBeforeEnd_isRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.createSlot(2L, request(1L, START, START)));
        assertThrows(IllegalArgumentException.class, () -> service.createSlot(2L, request(1L, END, START)));
    }

    @Test
    void createSlot_inThePast_isRejected() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        assertThrows(IllegalArgumentException.class,
            () -> service.createSlot(2L, request(1L, past, past.plusHours(1))));
    }

    @Test
    void createSlot_userIsNotAProvider_throwsNotFound() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.createSlot(2L, request(1L, START, END)));
    }

    @Test
    void createSlot_overlappingSlot_isRejectedAndNothingSaved() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(7L);
        when(slotRepository.existsOverlappingSlot(7L, START, END)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createSlot(2L, request(1L, START, END)));
        verify(slotRepository, never()).createSlot(any(), any(), any(), any());
    }

    @Test
    void createSlot_validRequest_isSavedForTheProvider() {
        SlotResponse saved = new SlotResponse(9L, 7L, 1L, START, END, 0);
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(7L);
        when(slotRepository.existsOverlappingSlot(7L, START, END)).thenReturn(false);
        when(slotRepository.createSlot(7L, 1L, START, END)).thenReturn(saved);

        assertSame(saved, service.createSlot(2L, request(1L, START, END)));
    }

    // ---- lookup ----

    @Test
    void getSlotById_unknownSlot_throwsNotFound() {
        when(slotRepository.findSlotById(1L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.getSlotById(1L));
    }

    // ---- removal: ownership and booked rules ----

    @Test
    void removeSlot_userIsNotAProvider_throwsNotFound() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.removeSlot(2L, 1L));
    }

    @Test
    void removeSlot_slotOwnedByAnotherProvider_isRejected() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(7L);
        when(slotRepository.findSlotProviderId(1L)).thenReturn(8L);

        assertThrows(ResourceNotFoundException.class, () -> service.removeSlot(2L, 1L));
        verify(slotRepository, never()).deleteSlot(any(), any());
    }

    @Test
    void removeSlot_slotWithAppointments_isRejected() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(7L);
        when(slotRepository.findSlotProviderId(1L)).thenReturn(7L);
        when(slotRepository.countAppointmentsForSlot(1L)).thenReturn(1);

        assertThrows(SlotAlreadyBookedException.class, () -> service.removeSlot(2L, 1L));
        verify(slotRepository, never()).deleteSlot(any(), any());
    }

    @Test
    void removeSlot_ownedAndUnbooked_isDeleted() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(7L);
        when(slotRepository.findSlotProviderId(1L)).thenReturn(7L);
        when(slotRepository.countAppointmentsForSlot(1L)).thenReturn(0);

        service.removeSlot(2L, 1L);

        verify(slotRepository).deleteSlot(1L, 7L);
    }
}