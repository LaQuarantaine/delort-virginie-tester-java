package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        long inTimeMillis = ticket.getInTime().getTime();
        long outTimeMillis = ticket.getOutTime().getTime();

        /**
        changement pour tenir compte de l'ensemble date et heure précise 
        et non plus le seul chiffre des heures, ce qui ignorait les minutes 
        et le changement de jour 
        la méthode calculateFare() s'appliquant aussi bien aux voitures qu'au 
        velo, cela solutionne les 3 erreurs
        */
        long durationMillis = outTimeMillis - inTimeMillis;
        double durationHours = durationMillis / (1000.0 * 60 * 60);

        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                ticket.setPrice(durationHours * Fare.CAR_RATE_PER_HOUR);
                break;
            }
            case BIKE: {
                ticket.setPrice(durationHours * Fare.BIKE_RATE_PER_HOUR);
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
}