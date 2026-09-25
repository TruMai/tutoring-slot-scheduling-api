package edu.sjsu.cmpe172.tutoring.service;

import java.util.List;

import org.springframework.stereotype.Service;

import edu.sjsu.cmpe172.tutoring.dto.SlotDto;
import edu.sjsu.cmpe172.tutoring.repository.SlotRepository;
@Service 
public class TutoringService{
    private final SlotRepository slotRepository;

    public TutoringService(SlotRepository slotRepository){
        this.slotRepository = slotRepository;
    }

    public List<SlotDto> getAvailableSlots(){
        return slotRepository.findAvailableSlots();
    }
}