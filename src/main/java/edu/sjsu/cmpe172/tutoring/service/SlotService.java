package edu.sjsu.cmpe172.tutoring.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.sjsu.cmpe172.tutoring.dto.CreateSlotRequest;
import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.dto.SlotResponse;
import edu.sjsu.cmpe172.tutoring.exception.ResourceNotFoundException;
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
    if (!request.startTime().isAfter(java.time.LocalDateTime.now())){
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
    
    public List<SlotDto> getAvailableSlots(Long providerId, Long serviceId, int limit, int offset){
        return slotRepository.findAvailableSlots(providerId, serviceId, limit, offset);
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
            throw new ResourceNotFoundException("Slot not found with ID: ");
        } 
        return slot;
    }
}
