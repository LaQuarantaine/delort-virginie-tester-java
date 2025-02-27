package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ParkingServiceTest {

    private static ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;
    @Mock
    private static ParkingSpotDAO parkingSpotDAO;
    @Mock
    private static TicketDAO ticketDAO;
    @Mock
    private FareCalculatorService fareCalculatorService;


    @BeforeEach 
    @DisplayName("Préparation du test : Initialisation des mocks et des objets nécessaires")
    private void setUpPerTest() {
        try {
            when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");

            ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
            Ticket ticket = new Ticket();
            ticket.setInTime(new Date(System.currentTimeMillis() - (60*60*1000)));
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber("ABCDEF");
            
            when(ticketDAO.getTicket(anyString())).thenReturn(ticket);
            when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);
            when(parkingSpotDAO.updateParking(any(ParkingSpot.class))).thenReturn(true);
            when(ticketDAO.getNbTicket(anyString())).thenReturn(1);

            parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }
 
    @Test 
    @DisplayName("Mise à jour de la disponibilité du parking")
    public void processExitingVehicleUpdateParking(){
        parkingService.processExitingVehicle();
        verify(parkingSpotDAO, Mockito.times(1)).updateParking(any(ParkingSpot.class));
    }
    
    @Test
    @DisplayName("Vérifier si utilisateur régulier")
    public void processExitingVehicle_ShouldCheckUserRegular(){
        // Given
	    when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2); 
        
        // When
    	parkingService.processExitingVehicle();
    	
    	// Then
        verify(ticketDAO, Mockito.times(1)).getNbTicket("ABCDEF");
    }
    
    @Test
    @DisplayName("Vérifier que l'utilisateur occasionnel ne bénéficie pas de la remise")
    public void processExitingVehicle_ShouldNotApplyDiscount_IfUserNotRegular() {
    	// Given
		
	    // When
  	    parkingService.processExitingVehicle();
	    
	    // Then
	    verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class));
    }   
    
    @Test
    @DisplayName("Vérifier que l'utilisateur régulier bénéficie de la remise")
    public void processExitingVehicle_ShouldApplyDiscount_IfUserRegular() {
    	// Given
        when(ticketDAO.getNbTicket(anyString())).thenReturn(2);
	    
	    // When
	    parkingService.processExitingVehicle();
	    
	    // Then
	    verify(fareCalculatorService, Mockito.times(1)).calculateFare(any(Ticket.class), eq(true));
    }   
}
