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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
    	// Given @BeforeAll + @BeforeEach + ...
        FareCalculatorService fareCalculatorService = new FareCalculatorService(); 
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        
        // When
        parkingService.processIncomingVehicle();
        
        // Then
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        // Vérifier que le ticket est bien enregistré en base
        assertNotNull(ticket, "Le ticket ne doit pas être null !");
        assertNotNull(ticket.getInTime(), "L'heure d'entrée ne doit pas être null !");
        
        // Vérifier que le numéro de la place de parking est correct
        assertEquals(1, ticket.getParkingSpot().getId(), "L'ID du parking doit être 1 !");
        
        // Vérifier que la place de parking a bien été mise à jour
        int availableSlot = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertTrue(availableSlot > 0, "Une place de parking doit être disponible après l'entrée du véhicule !");
        
        // Vérifier que la mise à jour de la table de parking s'est bien effectuée
        boolean updateSuccessful = parkingSpotDAO.updateParking(ticket.getParkingSpot());
        assertTrue(updateSuccessful, "La mise à jour du parking a réussi !");
        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }

    @Test
    public void testParkingLotExit(){
        // Given
    	testParkingACar();
        FareCalculatorService fareCalculatorService = new FareCalculatorService(); 
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        
        // When
        parkingService.processExitingVehicle();
        
        // Then
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        assertNotNull(ticket, "Le ticket ne doit pas être null !");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie ne doit pas être null !");
        assertNotNull(ticket.getPrice(), "Le prix ne doit pas être null !");
        //TODO: check that the fare generated and out time are populated correctly in the database
    }
    
    @Test
    public void testParkingLotExitRecurringUser() {
    	//Given
    	
    	
    	// When
    	parkingService.processIncomingVehicle();
    	parkingService.processExitingVehicle();
    	
    	//Then
    	
    	// TODO: vérifier le calcul du prix d'un ticket pour un utilisateur récurrent (remise de 5%)
    }

}
