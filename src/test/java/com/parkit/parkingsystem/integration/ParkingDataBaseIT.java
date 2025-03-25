package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private static FareCalculatorService fareCalculatorService;

    @Mock
    private InputReaderUtil inputReaderUtil;
 
    @BeforeAll
    static void setUp() throws Exception{
    	parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        fareCalculatorService = new FareCalculatorService();
        dataBasePrepareService = new DataBasePrepareService();
    }

   
    @AfterAll
    static void tearDown(){
    	// dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    @DisplayName("Vérifier que le ticket est enregistré et qu'une place CAR passe indisponible")
    public void testParkingACar() throws Exception{
    	// Given @BeforeAll + @BeforeEach + ...
    	when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        
        // When
        parkingService.processIncomingVehicle();
        
        // Then Vérifier que le ticket est bien enregistré en base
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Le ticket doit être sauvegardé dans la BDD !");
        assertEquals("ABCDEF", ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime(), "L'heure d'entrée ne doit pas être null !");
        assertNull(ticket.getOutTime(), "L’heure de sortie doit être null après l’entrée");
        assertEquals(0.0, ticket.getPrice(), "Le prix doit être 0 à l’entrée");
        
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertEquals(1, ticket.getParkingSpot().getId(), "L'ID du parking doit être 1 !");
        assertTrue(parkingSpot.getId() >= 1 && parkingSpot.getId() <= 3);
        assertFalse(parkingSpot.isAvailable());
        
        // Vérifier que la mise à jour de la table de parking s'est bien effectuée
        boolean updateSuccessful = parkingSpotDAO.updateParking(ticket.getParkingSpot());
        assertTrue(updateSuccessful, "La mise à jour du parking a réussi !");
        
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }

    @Test
    @DisplayName("Vérifier que le ticket est enregistré et qu'une place BIKE passe indisponible")
    public void testParkingABike() throws Exception {
        // Given : override inputReader pour simuler BIKE
        when(inputReaderUtil.readSelection()).thenReturn(2); // 2 = BIKE
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("BIKE123");
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        // When
        parkingService.processIncomingVehicle();

        // Then
        Ticket ticket = ticketDAO.getTicket("BIKE123");
        assertNotNull(ticket, "Le ticket doit être enregistré en BDD");
        assertEquals("BIKE123", ticket.getVehicleRegNumber());
        assertNotNull(ticket.getInTime(), "L'heure d'entrée ne doit pas être null !");
        assertNull(ticket.getOutTime(), "L’heure de sortie doit être null après l’entrée");
        assertEquals(0.0, ticket.getPrice(), "Le prix doit être 0 à l’entrée");
        
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        assertNotNull(parkingSpot);
        assertEquals(ParkingType.BIKE, parkingSpot.getParkingType());
        assertEquals(4, ticket.getParkingSpot().getId(), "L'ID du parking doit être 4 !");
        assertTrue(parkingSpot.getId() >= 4 && parkingSpot.getId() <= 5);
        assertFalse(parkingSpot.isAvailable());
        
        boolean updateSuccessful = parkingSpotDAO.updateParking(ticket.getParkingSpot());
        assertTrue(updateSuccessful, "La mise à jour du parking a réussi !");
    }
    
    @Test
    @DisplayName("Vérifier que l'heure de sortie et le tarif sont corrects dans la BDD")
    public void testParkingLotExit() throws Exception {
        // Given
    	when(inputReaderUtil.readSelection()).thenReturn(1);
    	when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("AAA111");
    	
    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
    	
 
    	try (Connection con = dataBaseTestConfig.getConnection()) {
    		PreparedStatement ps = con.prepareStatement("UPDATE ticket SET in_time = ? WHERE vehicle_reg_number = ?");
    		ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - 60 * 60 * 1000)); // 1h avant
    		ps.setString(2, "AAA111");
    		ps.executeUpdate();
    	}
    	
        // When
        parkingService.processExitingVehicle();
        
        // Then
        Ticket ticket = ticketDAO.getTicket("ABC123");
        
        assertNotNull(ticket, "Le ticket doit exister !");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie soit être renseignée !");
        assertTrue(ticket.getPrice() >0, "Le prix doit être sup à 0 pour une durée sup à 30 min !");
        //TODO: check that the fare generated and out time are populated correctly in the database
    }
    
    @Test
    public void testParkingLotExitRecurringUser() {
    	//Given
    	
    	
    	// When

    	
    	//Then
    	
    	// TODO: vérifier le calcul du prix d'un ticket pour un utilisateur récurrent (remise de 5%)
    }

}
