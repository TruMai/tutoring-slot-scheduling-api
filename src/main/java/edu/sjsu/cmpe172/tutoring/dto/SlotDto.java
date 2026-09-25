package edu.sjsu.cmpe172.tutoring.dto;

public class SlotDto {
    private final Long id;
    private final String providerName;
    private final String serviceName;
    private final Double price;
    private final Integer durationMin;
    private final String startAt;
    private final String endAt;

    public SlotDto(Long id, String providerName, String serviceName, Double price, Integer durationMin, String startAt, String endAt) {
        this.id = id;
        this.providerName = providerName;
        this.serviceName = serviceName;
        this.price = price;
        this.durationMin = durationMin;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long getId() { return id; }
    public String getProviderName() { return providerName; }
    public String getServiceName() { return serviceName; }
    public Double getPrice() { return price; }
    public Integer getDurationMin() { return durationMin; }
    public String getStartAt() { return startAt; }
    public String getEndAt() { return endAt; }
}