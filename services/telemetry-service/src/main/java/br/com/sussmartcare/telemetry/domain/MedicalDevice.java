package br.com.sussmartcare.telemetry.domain;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@Entity
@Table(name = "medical_devices")
public class MedicalDevice {
    @Id
    private UUID id;
    @Column(nullable = false, unique = true)
    private String externalId;
    @Column(nullable = false)
    private String deviceType;
    private String manufacturer;
    private String model;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;
    @Column(nullable = false)
    private Instant registeredAt;

    protected MedicalDevice() {
    }

    public MedicalDevice(String externalId, String type, String manufacturer, String model) {
        id = UUID.randomUUID();
        this.externalId = externalId;
        deviceType = type;
        this.manufacturer = manufacturer;
        this.model = model;
        status = DeviceStatus.ACTIVE;
        registeredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getModel() {
        return model;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}