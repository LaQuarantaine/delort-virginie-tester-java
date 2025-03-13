package com.parkit.parkingsystem.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

	public FareCalculatorService() {
		
	}
    public void calculateFare(Ticket ticket, boolean discount){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        long durationMillis = outTimeMillis - inTimeMillis;
        double durationHours = durationMillis / (1000.0 * 60 * 60);
        
        if (durationMillis < (30 * 60* 1000)) {
        	ticket.setPrice(0.0);
        } 
        
        else if (durationMillis >= (30 * 60* 1000) && discount == true) {
        	switch (ticket.getParkingSpot().getParkingType()){
        		case CAR: {
        		double price = durationHours * Fare.CAR_RATE_PER_HOUR * 0.95;
                BigDecimal bd = new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
                ticket.setPrice(bd.doubleValue()); // Met à jour avec l'arrondi
        		break;
        		}
        		case BIKE: {
        		double price = durationHours * Fare.BIKE_RATE_PER_HOUR * 0.95;
                BigDecimal bd = new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
                ticket.setPrice(bd.doubleValue()); // Met à jour avec l'arrondi
                break;
        		}
            default: throw new IllegalArgumentException("Unkown Parking Type");
        	}
        }
        
        else 
        	switch (ticket.getParkingSpot().getParkingType()){
    		case CAR: {
    		double price = durationHours * Fare.CAR_RATE_PER_HOUR;
            BigDecimal bd = new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
            ticket.setPrice(bd.doubleValue()); // Met à jour avec l'arrondi
            break;
    		}
    		case BIKE: {
    		double price = durationHours * Fare.BIKE_RATE_PER_HOUR;
            BigDecimal bd = new BigDecimal(price).setScale(2, RoundingMode.HALF_UP);
            ticket.setPrice(bd.doubleValue()); // Met à jour avec l'arrondi
            break;
    		}
        default: throw new IllegalArgumentException("Unkown Parking Type");
    	}
    }
        
        
       public void calculateFare(Ticket ticket){
    	   calculateFare(ticket, false);
       }

}
