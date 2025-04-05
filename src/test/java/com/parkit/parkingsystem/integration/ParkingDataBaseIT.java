package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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
    
    @Mock
    private TicketDAO mockTicketDAO;
 
    @BeforeAll
    static void setUp() throws Exception{
    	parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        
        fareCalculatorService = new FareCalculatorService();
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        dataBasePrepareService.clearDataBaseEntries();
    }
    
    @AfterAll
    static void tearDown(){
    	// dataBasePrepareService.clearDataBaseEntries();
    }

    @Test
    @DisplayName("Vérifier que le ticket est enregistré et qu'une place CAR passe indisponible")
    public void testParkingACar() throws Exception{
    	// Given 
    	when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("AAA111");
    	ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        
        // When
        parkingService.processIncomingVehicle();
        
        // Then Vérifier que le ticket est enregistré en base
        Ticket ticket = ticketDAO.getTicket("AAA111");
        
        assertAll("Vérification du ticket à l'entrée",
        	    () -> assertNotNull(ticket, "Le ticket doit être sauvegardé dans la BDD !"),
        	    () -> assertEquals("AAA111", ticket.getVehicleRegNumber()),
        	    () -> assertNotNull(ticket.getInTime(), "L'heure d'entrée ne doit pas être null !"),
        	    () -> assertNull(ticket.getOutTime(), "L’heure de sortie doit être null après l’entrée"),
        	    () -> assertEquals(0.0, ticket.getPrice(), "Le prix doit être 0 à l’entrée")
        	);
        
        // Then Vérifier la mise à jour du parking
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        
        assertAll("Vérification complète de la place de parking",
        	    () -> assertNotNull(parkingSpot, "Le parking ne doit pas être null !"),
        	    () -> assertEquals(ParkingType.CAR, parkingSpot.getParkingType(), "Le type de parking doit être CAR !"),
        	    () -> assertEquals(1, parkingSpot.getId(), "L'ID du parking doit être 1 !"),
        	    () -> assertFalse(parkingSpot.isAvailable(), "La place ne doit pas être disponible !"),
        	    () -> assertTrue(parkingSpotDAO.updateParking(parkingSpot), "La mise à jour du parking a réussi !")
        	);

        //TODO: check that a ticket is actualy saved in DB and Parking table is updated with availability
    }

  
    @Test
    @DisplayName("Vérifier que l'heure de sortie et le tarif (gratuité) sont corrects dans la BDD")
    public void testParkingLotExit() throws Exception {
        // Given
    	String vehicleRegNumber = "BBB222";
    	when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processIncomingVehicle();
        
        // When 
        Thread.sleep(2000);
        parkingService.processExitingVehicle();

        // Then 
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

        assertAll("Vérification du ticket à la sortie",
        		() -> assertNotNull(ticket.getOutTime(), "L'heure de sortie doit être renseignée !"),
        		() -> assertTrue(ticket.getInTime().before(ticket.getOutTime()), "L'heure d'entrée doit être antérieure à l'heure de sortie !"),
        		() -> assertNotNull(ticket.getPrice(), "Le prix doit être renseigné")
        	);
        //TODO: check that the fare generated and out time are populated correctly in the database
    }
    
    @Test
    @DisplayName("Vérifier que l'heure de sortie et le tarif sont corrects dans la BDD")
    public void testParkingLotExit2() throws Exception {
        // Given
    	String vehicleRegNumber = "BBB222";
    	when(inputReaderUtil.readSelection()).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        Ticket entryTicket = createTicket(1, vehicleRegNumber, new Date(System.currentTimeMillis() - 60 * 60 * 1000), null, new ParkingSpot(1, ParkingType.CAR, false));
        ticketDAO.saveTicket(entryTicket);
        parkingService.processIncomingVehicle();
        
        // When 
        parkingService.processExitingVehicle();

        // Then 
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

        assertAll("Vérification du ticket à la sortie",
        		() -> assertNotNull(ticket.getOutTime(), "L'heure de sortie doit être renseignée !"),
        		() -> assertNotNull(ticket.getPrice(), "Le prix doit être renseigné")
        	);
   
    }
    
    @Test
    @DisplayName("Vérifier que le visiteur régulier bénéficie de la remise fidélité")
    public void testRecurringUserIntegration() throws Exception {
        String vehicleRegNumber = "CCC333";

        // Spy pour mocker partiellement (return 3)
        TicketDAO spyTicketDAO = Mockito.spy(ticketDAO);
        when(spyTicketDAO.getNbTicket(vehicleRegNumber)).thenReturn(3);

        // ParkingService avec le vrai DAO (spy)
        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, spyTicketDAO, fareCalculatorService);

        // Sauvegarde réelle d'un ticket d'entrée 1h avant
        Ticket ticket = createTicket(1, vehicleRegNumber, new Date(System.currentTimeMillis() - 60 * 60 * 1000), null, new ParkingSpot(1, ParkingType.CAR, false));
        spyTicketDAO.saveTicket(ticket);

        // Simule saisie immatriculation à la sortie
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);

        // Appel réel de la sortie
        parkingService.processExitingVehicle();

        // Vérif
        Ticket finalTicket = spyTicketDAO.getTicket(vehicleRegNumber);

        assertNotNull(finalTicket, "Un ticket doit exister pour le véhicule à la sortie");
        assertNotNull(finalTicket.getOutTime(), "L'heure de sortie doit être renseignée");

        double expectedPrice = Fare.CAR_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        assertEquals(expectedPrice, finalTicket.getPrice(), 0.01,
            "Le tarif doit inclure une remise de 5% pour utilisateur récurrent");
    }
    

    
    private Ticket createTicket(int Id,String vehicleRegNumber, Date inTime, Date outTime, ParkingSpot parkingSpot) {
    	Ticket ticket = new Ticket();
    	ticket.setId(1);
        ticket.setVehicleRegNumber(vehicleRegNumber);
        ticket.setInTime(inTime); 
        ticket.setOutTime(null);
        ticket.setParkingSpot(parkingSpot);
        ticket.setPrice(0.0);
        return ticket;
    }
    
}
    

