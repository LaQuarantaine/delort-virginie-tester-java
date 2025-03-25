package com.parkit.parkingsystem.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.dao.TicketDAO;

public class FareCalculatorService {


	public void calculateFare(Ticket ticket, boolean discount){
        if (ticket == null || ticket.getParkingSpot() == null || ticket.getInTime() == null) {
        	throw new IllegalArgumentException("Données manquantes pour ticket.");
        }
		if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Erreur sur heure de sortie : "+ticket.getOutTime().toString());
        }
        
		long durationMillis = Duration.getDurationMillis(ticket.getInTime(), ticket.getOutTime());

        if (durationMillis < Fare.FREE_PARKING_DURATION) {
        	ticket.setPrice(0.0);
        	return;
        } 
        
        double durationHours = Duration.getDurationToHoursDecimal(durationMillis); 
        
        double price;
         	switch (ticket.getParkingSpot().getParkingType()){
        		case CAR: {
        		price = durationHours * Fare.CAR_RATE_PER_HOUR;
                break;
        		}
        		case BIKE: {
        		price = durationHours * Fare.BIKE_RATE_PER_HOUR;
                break;
        		}
            default: throw new IllegalArgumentException("Unkown Parking Type");
        	}
        
        if (discount) {
        	price *= Fare.COEF_DISCOUNT;
        }
        	
    	ticket.setPrice(roundPrice(price));
	}      
        
	private double roundPrice (double price) {
		return new BigDecimal(price).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
	
	
	public void calculateFare(Ticket ticket){
    	calculateFare(ticket, false);
    }

	
}
