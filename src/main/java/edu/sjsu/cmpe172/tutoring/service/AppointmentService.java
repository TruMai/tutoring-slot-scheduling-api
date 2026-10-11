package edu.sjsu.cmpe172.tutoring.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.model.Appointment;
import edu.sjsu.cmpe172.tutoring.repository.AppointmentRepository;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;
// versioning logic in here

@Service
public class AppointmentService {

    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentService(SlotRepository slotRepository, AppointmentRepository appointmentRepository) {
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // ensures all DB writes execute within a transaction
    @Transactional
    public Appointment bookSlot(Long customerId, Long slotId) {
        // 1 Read current version from database
        Integer currentVersion = slotRepository.getSlotVersion(slotId);
        if (currentVersion == null) {
            throw new IllegalArgumentException("Slot not found with ID: " + slotId);
        }

        //2 Optimistic Lock
        int updatedRows = slotRepository.updateSlotVersion(slotId, currentVersion);
        if (updatedRows == 0) {
            // race condition lost
            throw new SlotAlreadyBookedException("Slot is already booked or modified by another request.");
        }

        // 3 Insert appointment record
        try {
            return appointmentRepository.createAppointment(customerId, slotId);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new SlotAlreadyBookedException("Slot is already booked.");
        }
    }
    
    @Transactional
    // Cancels an appointment
    public void cancelAppointment(Long customerId, Long appointmentId) {
        // SQL query to verify and update status
        int updatedRows = appointmentRepository.cancelAppointment(customerId, appointmentId);

        if (updatedRows == 0) {
            throw new ResourceNotFoundException("Appointment not found or already cancelled.");
        }
    }
}