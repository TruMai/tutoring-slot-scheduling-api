package edu.sjsu.cmpe172.tutoring.service;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import edu.sjsu.cmpe172.tutoring.dto.AppointmentSummary;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.repository.AppointmentRepository;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock SlotRepository slotRepository;
    @Mock AppointmentRepository appointmentRepository;
    @InjectMocks AppointmentService service;

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(7);

    // ---- booking rules ----

    @Test
    void bookSlot_unknownSlot_throwsNotFound() {
        when(slotRepository.getSlotStartAt(1L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.bookSlot(10L, 1L));
    }

    @Test
    void bookSlot_slotAlreadyStarted_isRejectedBeforeAnyWrite() {
        when(slotRepository.getSlotStartAt(1L)).thenReturn(LocalDateTime.now().minusHours(1));

        assertThrows(IllegalArgumentException.class, () -> service.bookSlot(10L, 1L));

        verify(slotRepository, never()).updateSlotVersion(anyLong(), anyInt());
        verify(appointmentRepository, never()).createAppointment(anyLong(), anyLong());
    }

    @Test
    void bookSlot_versionConflict_throwsSlotAlreadyBooked() {
        when(slotRepository.getSlotStartAt(1L)).thenReturn(FUTURE);
        when(slotRepository.getSlotVersion(1L)).thenReturn(0);
        when(slotRepository.updateSlotVersion(1L, 0)).thenReturn(0);   // lost the optimistic lock

        assertThrows(SlotAlreadyBookedException.class, () -> service.bookSlot(10L, 1L));
        verify(appointmentRepository, never()).createAppointment(anyLong(), anyLong());
    }

    @Test
    void bookSlot_uniqueIndexViolation_throwsSlotAlreadyBooked() {
        when(slotRepository.getSlotStartAt(1L)).thenReturn(FUTURE);
        when(slotRepository.getSlotVersion(1L)).thenReturn(0);
        when(slotRepository.updateSlotVersion(1L, 0)).thenReturn(1);
        when(appointmentRepository.createAppointment(10L, 1L)).thenThrow(new DuplicateKeyException("unique"));

        assertThrows(SlotAlreadyBookedException.class, () -> service.bookSlot(10L, 1L));
    }

    @Test
    void bookSlot_validRequest_returnsBookedAppointment() {
        Appointment expected = new Appointment(5L, 10L, 1L, "BOOKED");
        when(slotRepository.getSlotStartAt(1L)).thenReturn(FUTURE);
        when(slotRepository.getSlotVersion(1L)).thenReturn(0);
        when(slotRepository.updateSlotVersion(1L, 0)).thenReturn(1);
        when(appointmentRepository.createAppointment(10L, 1L)).thenReturn(expected);

        assertEquals(expected, service.bookSlot(10L, 1L));
    }

    // ---- ownership ----

    @Test
    void cancelAppointment_notOwnerOrAlreadyCancelled_throwsNotFound() {
        when(appointmentRepository.cancelAppointment(10L, 5L)).thenReturn(0);
        assertThrows(ResourceNotFoundException.class, () -> service.cancelAppointment(10L, 5L));
    }

    @Test
    void cancelAppointment_owner_succeeds() {
        when(appointmentRepository.cancelAppointment(10L, 5L)).thenReturn(1);
        assertDoesNotThrow(() -> service.cancelAppointment(10L, 5L));
    }

    @Test
    void getAppointment_otherCustomersAppointment_throwsNotFound() {
        when(appointmentRepository.findByIdAndCustomer(5L, 99L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.getAppointment(99L, 5L));
    }

    @Test
    void getAppointment_owner_returnsAppointment() {
        Appointment a = new Appointment(5L, 10L, 1L, "BOOKED");
        when(appointmentRepository.findByIdAndCustomer(5L, 10L)).thenReturn(a);
        assertSame(a, service.getAppointment(10L, 5L));
    }

    // ---- lists and COMPLETED ----

    @Test
    void getCustomerAppointments_completesFinishedAppointmentsFirst() {
        when(appointmentRepository.findForCustomer(10L, true)).thenReturn(List.<AppointmentSummary>of());

        service.getCustomerAppointments(10L, true);

        InOrder order = inOrder(appointmentRepository);
        order.verify(appointmentRepository).completeFinished();
        order.verify(appointmentRepository).findForCustomer(10L, true);
    }

    @Test
    void getProviderAppointments_notAProvider_throwsNotFound() {
        when(slotRepository.findProviderIdByUserId(2L)).thenReturn(null);
        assertThrows(ResourceNotFoundException.class, () -> service.getProviderAppointments(2L));
    }
}