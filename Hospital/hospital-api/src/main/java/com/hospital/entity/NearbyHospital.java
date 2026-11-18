package com.hospital.entity;

public class NearbyHospital {
    private String name;
    private String address;
    private Double distance;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String emergencyLink;
    private String type;
    private Boolean isOpen;

    public NearbyHospital() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmergencyLink() { return emergencyLink; }
    public void setEmergencyLink(String emergencyLink) { this.emergencyLink = emergencyLink; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getIsOpen() { return isOpen; }
    public void setIsOpen(Boolean isOpen) { this.isOpen = isOpen; }
}
