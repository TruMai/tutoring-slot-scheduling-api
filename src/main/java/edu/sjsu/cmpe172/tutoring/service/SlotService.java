package edu.sjsu.cmpe172.tutoring.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.sjsu.cmpe172.tutoring.dto.CreateSlotRequest;
import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.dto.SlotResponse;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
import edu.sjsu.cmpe172.tutoring.exception.SlotAlreadyBookedException;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;

@Service
public class SlotService {
    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository){
        this.slotRepository = slotRepository;
    }

    public SlotResponse createSlot(Long userId, CreateSlotRequest request){
        if(request.startTime() == null || request.endTime() == null){
            throw new IllegalArgumentException("Start time and end time are required");
        }
        if(!request.startTime().isBefore(request.endTime())){
            throw new IllegalArgumentException("startTime must come before endTime");
        }
        if (!request.startTime().isAfter(LocalDateTime.now())){
            throw new IllegalArgumentException("Cannot create slot in the past");
        }

        Long providerId = slotRepository.findProviderIdByUserId(userId);
        if(providerId == null){
            throw new ResourceNotFoundException("Provider not found by userId");
        }
        boolean hasOverlap = slotRepository.existsOverlappingSlot(providerId, request.startTime(), request.endTime());
        if (hasOverlap) {
            throw new IllegalArgumentException("A slot already exists during this time range.");
        }

        // handle serviceId in case null
        Long serviceId = request.serviceId();
        return slotRepository.createSlot(providerId, serviceId, request.startTime(), request.endTime());
    }

    public List<SlotDto> getAvailableSlots(Long providerId, Long serviceId, LocalDate date, int limit, int offset){
        return slotRepository.findAvailableSlots(providerId, serviceId, date, limit, offset);
    }

    public List<SlotDto> getProviderSlots(Long userId) {
        Long providerId = slotRepository.findProviderIdByUserId(userId);
        if (providerId == null) {
            throw new ResourceNotFoundException("Provider not found by userId");
        }
        return slotRepository.findProviderSlots(providerId);
    }

    public SlotDto getSlotById(Long slotId) {
        SlotDto slot = slotRepository.findSlotById(slotId);
        if (slot == null){
            throw new ResourceNotFoundException("Slot not found with ID: " + slotId);
        }
        return slot;
    }

    /** Provider remove one of their slots,  if it has no appointments. */
    @Transactional
    public void removeSlot(Long userId, Long slotId) {
        Long providerId = slotRepository.findProviderIdByUserId(userId);
        if (providerId == null) {
            throw new ResourceNotFoundException("Provider not found by userId");
        }
        Long ownerId = slotRepository.findSlotProviderId(slotId);
        if (ownerId == null || !ownerId.equals(providerId)) {          // ownership check
            throw new ResourceNotFoundException("Slot not found with ID: " + slotId);
        }
        if (slotRepository.countAppointmentsForSlot(slotId) > 0) {
            throw new SlotAlreadyBookedException("Slot has appointments and cannot be removed.");
        }
        slotRepository.deleteSlot(slotId, providerId);
    }
}