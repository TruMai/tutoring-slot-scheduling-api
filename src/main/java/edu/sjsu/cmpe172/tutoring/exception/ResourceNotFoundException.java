package edu.sjsu.cmpe172.tutoring.exception;

// throw exception when slot, ..., and appointment resources are not found in DB
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message){
        super(message);
    }
}
