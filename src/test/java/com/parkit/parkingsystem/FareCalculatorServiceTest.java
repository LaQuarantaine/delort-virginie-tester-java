package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;

    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }


    @Test
    @DisplayName("Vérifier que le tarif voiture pour 1h est bien calculé")
    public void calculateFareCar(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 60 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    
    @Test
    @DisplayName("Vérifier que le tarif moto pour 1h est bien calculé")
    public void calculateFareBike(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 60 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket, false);
        
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }


    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si le type de véhicule est inconnu")
    public void calculateFareUnkownType(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 60 * 60 * 1000), new Date(), new ParkingSpot(1, null, false));
        
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }
    

    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si inTime est dans le futur")
    public void calculateFareBikeWithFutureInTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() + 60 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.BIKE, false));
        
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }
    

    @Test
    @DisplayName("Vérifier que le tarif est proportionnel pour un stationnement moto de 45 minutes")
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 45 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket, false);
        
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }
    

    @Test
    @DisplayName("Vérifier que le tarif est correct pour 24h de stationnement voiture")
    public void calculateFareCarWithMoreThanADayParkingTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 24* 60 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        double ExpectedPrice = 24 * Fare.CAR_RATE_PER_HOUR;
                
        assertEquals(ExpectedPrice, ticket.getPrice());
    }
    
    
    @Test
    @DisplayName("Vérifier que le tarif est gratuit pour une moto stationnée moins de 30 minutes")
    public void calculateFareBikeWithLessThan30minutesParkingTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 15 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(0.0, ticket.getPrice());
    }
    

    @Test
    @DisplayName("Vérifier que le tarif est gratuit pour une voiture stationnée moins de 30 minutes")
    public void calculateFareCarWithLessThan30minutesParkingTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 20 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(0.0, ticket.getPrice());
    }
    

    @Test
    @DisplayName("Vérifier que la remise est bien appliquée pour une auto")
    public void calculateFareCarWithDiscount(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 90 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket, true);
        double ExpectedPrice = 1.5 * Fare.CAR_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()), ticket.getPrice());
    }
    

    @Test
    @DisplayName("Vérifier que la remise est bien appliquée pour une moto")
    public void calculateFareBikeWithDiscount(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 150 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.BIKE, false));

        fareCalculatorService.calculateFare(ticket, true);
        double ExpectedPrice = 2.5 * Fare.BIKE_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        System.out.println("Tarif attendu : " + ExpectedPrice);
        System.out.println("Tarif calculé : " + ticket.getPrice());
        assertEquals((bd.doubleValue()), ticket.getPrice());;
    }


    @Test
    @DisplayName("Vérifier que le tarif voiture est bien calculé pour 2h")
    public void calculateFareCarWithTwoOneHourParkingTime(){
    	Ticket ticket = createTicket(new Date(System.currentTimeMillis() - 120 * 60 * 1000), new Date(), new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        double ExpectedPrice = 2 * Fare.CAR_RATE_PER_HOUR;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()), ticket.getPrice());
    }

    
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si le ticket est null")
    public void calculateFare_NullTicket_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(null));
    }
    
    
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si inTime est null")
    public void calculateFare_NullInTime_ShouldThrow() {
    	Ticket ticket = createTicket(null, new Date(), new ParkingSpot(1, ParkingType.CAR, false));
    	    	
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }
    
    
    private Ticket createTicket(Date inTime, Date outTime, ParkingSpot parkingSpot) {
    	Ticket ticket = new Ticket();
        ticket.setInTime(inTime); 
        ticket.setOutTime(outTime);
        ticket.setParkingSpot(parkingSpot);
        return ticket;
    }

}
