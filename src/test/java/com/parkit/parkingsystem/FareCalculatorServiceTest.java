package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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


    // US1 – En tant qu’utilisateur CAR, je veux que mon tarif soit calculé 
    // pour une heure complète et au tarif CAR
    @Test
    @DisplayName("Vérifier que le tarif voiture pour 1h est bien calculé")
    public void calculateFareCar(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    
    // US2 – En tant qu’utilisateur BIKE, je veux que mon tarif soit calculé 
    // pour une heure complète et au tarif BIKE
    @Test
    @DisplayName("Vérifier que le tarif moto pour 1h est bien calculé")
    public void calculateFareBike(){
    	Ticket ticket = new Ticket();
    	ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket, false);
        
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }

    // US3 – En tant qu’utilisateur BIKE, je veux que le stationnement < 30 min soit gratuit
    @Test
    @DisplayName("Vérifier que le tarif est gratuit pour une moto stationnée moins de 30 minutes")
    public void calculateFareBikeWithLessThan30minutesParkingTime(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 15 * 60 * 1000)); // 15min
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(0.0, ticket.getPrice());
    }
    
    // US3 – En tant qu’utilisateur CAR, je veux que le stationnement < 30 min soit gratuit
    @Test
    @DisplayName("Vérifier que le tarif est gratuit pour une voiture stationnée moins de 30 minutes")
    public void calculateFareCarWithLessThan30minutesParkingTime(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 20 * 60 * 1000)); // 20min
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        
        assertEquals(0.0 , ticket.getPrice());
    }
    
    // US4 – En tant qu’utilisateur BIKE, je veux une facturation proportionnelle si je reste moins d’une heure
    @Test
    @DisplayName("Vérifier que le tarif est proportionnel pour un stationnement moto de 45 minutes")
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 45 * 60 * 1000)); // 45min
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.BIKE, false));
        
        fareCalculatorService.calculateFare(ticket, false);
        
        assertEquals((0.75 * Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }
    
    // US5 – En tant qu’utilisateur BIKE régulier, je veux bénéficier de la remise prévue
    @Test
    @DisplayName("Vérifier que la remise est bien appliquée pour une moto")
    public void calculateFareBikeWithDiscount(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 150 * 60 * 1000)); // 2h30
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.BIKE, false));

        fareCalculatorService.calculateFare(ticket, true);
        double ExpectedPrice = 2.5 * Fare.BIKE_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()) , ticket.getPrice());;
    }

    // US5 – En tant qu’utilisateur CAR régulier, je veux bénéficier de la remise prévue
    public void calculateFareCarWithDiscount(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 90 * 60 * 1000)); // 1h30
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket, true);
        double ExpectedPrice = 1.5 * Fare.CAR_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()) , ticket.getPrice());
    }
    
    
    // US6 – En tant qu’utilisateur CAR, je veux que la durée soit bien calculée pour une durée de 2h
    @Test
    @DisplayName("Vérifier que le tarif voiture est bien calculé pour 2h")
    public void calculateFareCarWithTwoOneHourParkingTime(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 120 * 60 * 1000)); // 2h
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        double ExpectedPrice = 2 * Fare.CAR_RATE_PER_HOUR;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()) , ticket.getPrice());
    }

    
    // US6 – Cas extrême : 24h de stationnement CAR
    @Test
    @DisplayName("Vérifier que le tarif est correct pour 24h de stationnement voiture")
    public void calculateFareCarWithMoreThanADayParkingTime(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // 24h
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        
        fareCalculatorService.calculateFare(ticket);
        double ExpectedPrice = 24 * Fare.CAR_RATE_PER_HOUR;
        BigDecimal bd = new BigDecimal(ExpectedPrice).setScale(2, RoundingMode.HALF_UP);
        
        assertEquals((bd.doubleValue()) , ticket.getPrice());
    }
    
        
    // US8 – En tant que gestionnaire, je veux que le système gère les exceptions 
    // sur les véhicules non prévus
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si le type de véhicule est inconnu")
    public void calculateFareUnkownType_ShouldThrow(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, null, false));
        
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    
    // US8 – En tant que gestionnaire, je veux que le système gère les tickets invalides (null ticket)
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si le ticket est null")
    public void calculateFare_NullTicket_ShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(null));
    }
    
    
    // US8 – En tant que gestionnaire, je veux que le système gère les tickets invalides (inTime null)
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si inTime est null")
    public void calculateFare_NullInTime_ShouldThrow() {
        Ticket ticket = new Ticket();
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));

        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }
    
    
    // US9 – En tant que gestionnaire, je veux que le système gère une exception si l'heure d'entrée 
    // est postérieure à l'heure de sortie
    @Test
    @DisplayName("Vérifier que calculateFare() lève une exception si inTime est dans le futur")
    public void calculateFareBikeWithFutureInTime_ShouldThrow(){
    	Ticket ticket = new Ticket();
        ticket.setInTime(new Date(System.currentTimeMillis() + 60 * 60 * 1000)); // futur
        ticket.setOutTime(new Date());
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.BIKE, false));
        
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }
}
