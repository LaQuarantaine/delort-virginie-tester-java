package com.parkit.parkingsystem.model;

import java.util.Date;

public class Ticket {
    private int id;
    private ParkingSpot parkingSpot;
    private String vehicleRegNumber;
    private double price;
    private Date inTime;
    private Date outTime;

    
    public Ticket (ParkingSpot parkingSpot, String vehicleRegNumber, Date inTime) {
    	if (parkingSpot == null || vehicleRegNumber == null || vehicleRegNumber.trim().isEmpty() || inTime == null) {
    		throw new IllegalArgumentException ("Ticket invalide : ParkingSpot, vehicleRegNumber, inTime sont nécessaires.");
    	}
    	this.parkingSpot = parkingSpot;
    	this.vehicleRegNumber = vehicleRegNumber;
    	this.price = 0.0;
    	this.inTime = inTime;
    	this.outTime = null;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }

    public void setParkingSpot(ParkingSpot parkingSpot) {
    	if (parkingSpot == null) {
    		throw new IllegalArgumentException(" ParkingSpot ne peut pas être null.");
    	}
        this.parkingSpot = parkingSpot;
    }

    public String getVehicleRegNumber() {
        return vehicleRegNumber;
    }

    public void setVehicleRegNumber(String vehicleRegNumber) {
    	if (vehicleRegNumber == null || vehicleRegNumber.trim().isEmpty()) {
    		throw new IllegalArgumentException("L'immatriculation ne peut pas être vide.");
    	}
        this.vehicleRegNumber = vehicleRegNumber;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
    	if (price < 0.0) {
    		throw new IllegalArgumentException("Le prix ne peut pas être négatif."); 
    	}
        this.price = price;
    }

    public Date getInTime() {
        return inTime;
    }

    public void setInTime(Date inTime) {
    	if (inTime !=null && inTime.after(outTime)) {
    		throw new IllegalArgumentException("L'heure d'entrée ne peut pas être postérieure à l'heure de sortie.");
    	}
        this.inTime = inTime;
    }

    public Date getOutTime() {
        return outTime;
    }

    public void setOutTime(Date outTime) {
    	if (outTime !=null && inTime !=null && outTime.before(inTime)) {
    		throw new IllegalArgumentException("L'heure de sortie ne peut pas être antérieure à l'heure d'entrée.");
    	}
        this.outTime = outTime;
    }
}
