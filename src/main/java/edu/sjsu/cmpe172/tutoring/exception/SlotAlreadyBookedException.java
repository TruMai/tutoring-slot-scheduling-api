package edu.sjsu.cmpe172.tutoring.exception;

// concurrent exception
public class SlotAlreadyBookedException extends RuntimeException {
    public SlotAlreadyBookedException(String message){
        super(message);
    }
}
