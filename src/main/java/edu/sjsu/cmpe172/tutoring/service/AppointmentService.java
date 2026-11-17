package edu.sjsu.cmpe172.tutoring.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sjsu.cmpe172.tutoring.dto.AppointmentSummary;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.repository.AppointmentRepository;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;

@Service
public class AppointmentService {

    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(SlotRepository slotRepository, AppointmentRepository appointmentRepository) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public Appointment bookSlot(Long customerId, Long slotId) {
        // Slot must exist and not have started
        LocalDateTime startAt = slotRepository.getSlotStartAt(slotId);
        if (startAt == null) {
            throw new ResourceNotFoundException("Slot not found with ID: " + slotId);
        }
        if (!startAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book a slot that has already started.");
        }

        // Read current version
        Integer currentVersion = slotRepository.getSlotVersion(slotId);
        if (currentVersion == null) {
            throw new ResourceNotFoundException("Slot not found with ID: " + slotId);
        }

        // optimistic lock
        if (slotRepository.updateSlotVersion(slotId, currentVersion) == 0) {
            throw new SlotAlreadyBookedException("Slot is already booked or modified by another request.");
        }

        // double booking guard
        try {
            return appointmentRepository.createAppointment(customerId, slotId);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new SlotAlreadyBookedException("Slot is already booked.");
        }
    }

    @Transactional
    public void cancelAppointment(Long customerId, Long appointmentId) {
        int updatedRows = appointmentRepository.cancelAppointment(customerId, appointmentId);
        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Appointment not found or already cancelled.");
        }
    }

    public Appointment getAppointment(Long customerId, Long appointmentId) {
        appointmentRepository.completeFinished();   // so the status is up to date
        Appointment a = appointmentRepository.findByIdAndCustomer(appointmentId, customerId);
        if (a == null) {
            throw new ResourceNotFoundException("Appointment not found.");
        }
        return a;
    }

    /** upcoming = true -> BOOKED; false -> history (CANCELLED and COMPLETED). */
    public List<AppointmentSummary> getCustomerAppointments(Long customerId, boolean upcoming) {
        appointmentRepository.completeFinished();
        return appointmentRepository.findForCustomer(customerId, upcoming);
    }

    public List<AppointmentSummary> getProviderAppointments(Long userId) {
        Long providerId = slotRepository.findProviderIdByUserId(userId);
        if (providerId == null) {
            throw new ResourceNotFoundException("Provider not found by userId");
        }
        appointmentRepository.completeFinished();
        return appointmentRepository.findForProvider(providerId);
    }
}