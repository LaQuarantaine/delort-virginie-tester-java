package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.DBConstants;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        
        // Then Vérifier que le ticket est bien enregistré en base
        Ticket ticket = ticketDAO.getTicket("AAA111");
        assertNotNull(ticket, "Le ticket doit être sauvegardé dans la BDD !");
        assertEquals("AAA111", ticket.getVehicleRegNumber());
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
    @DisplayName("Vérifier que l'heure de sortie et le tarif sont corrects dans la BDD")
    public void testParkingLotExit() throws Exception {
        // Given
    	String vehicleRegNumber = "BBB222";

        when(inputReaderUtil.readSelection()).thenReturn(1); 
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);

        parkingService.processIncomingVehicle();

        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE ticket SET in_time = ? WHERE vehicle_reg_number = ?");
            Timestamp oneHourBefore = new Timestamp(System.currentTimeMillis() - 60 * 60 * 1000); // 1h avant
            ps.setTimestamp(1, oneHourBefore);
            ps.setString(2, vehicleRegNumber);
            ps.executeUpdate();
            ps.close();
        }

        // When 
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);
        parkingService.processExitingVehicle();

        // Then 
        Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);

        assertNotNull(ticket, "Le ticket doit exister !");
        assertNotNull(ticket.getOutTime(), "L'heure de sortie doit être renseignée !");
        assertTrue(ticket.getPrice() > 0, "Le prix doit être supérieur à 0 pour une durée supérieure à 30 minutes.");
        
        //TODO: check that the fare generated and out time are populated correctly in the database
    }
    
    @Test
    public void testRecurringUserIntegration() throws Exception {
        // Given
    	String vehicleRegNumber = "ABC123";

    	// === Étape 1 : Insertion manuelle d'un ticket dans BDD
    	try (Connection con = dataBaseTestConfig.getConnection()) {
    	   	PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET);
    	    ps.setInt(1, 1);
    	    ps.setString(2, vehicleRegNumber);
    	    ps.setDouble(3, Fare.CAR_RATE_PER_HOUR);
    	    ps.setTimestamp(4, new Timestamp(System.currentTimeMillis() - 24 * 60 * 60 * 1000)); // hier
    	    ps.setTimestamp(5, new Timestamp(System.currentTimeMillis() - 23 * 60 * 60 * 1000)); // hier + 1h
    	    ps.executeUpdate();
    	}

        // 2nde visite (réelle) 1h avant
        when(inputReaderUtil.readSelection()).thenReturn(1); // CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn(vehicleRegNumber);

        ParkingService parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        parkingService.processIncomingVehicle();

        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "UPDATE ticket SET in_time = ? WHERE vehicle_reg_number = ? AND out_time IS NULL");
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis() - 60 * 60 * 1000));
            ps.setString(2, vehicleRegNumber);
            ps.executeUpdate();
        }

        parkingService.processExitingVehicle();

        // Vérification du tarif avec tarif discount
        Ticket finalTicket = ticketDAO.getTicket(vehicleRegNumber);
        assertNotNull(finalTicket, "Un ticket doit exister pour le véhicule à la sortie");

        double expectedPrice = Fare.CAR_RATE_PER_HOUR * Fare.COEF_DISCOUNT;
        assertEquals(expectedPrice, finalTicket.getPrice(), 0.01,
            "Le tarif doit inclure une remise de 5% pour utilisateur récurrent");
    }
    
    @Test
    @DisplayName("Enregistrement d'un ticket d'entrée dans BDD")
    void testSaveTicketIntegration() throws Exception {
        TicketDAO ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = new DataBaseTestConfig();

        // Given
        Ticket ticket = new Ticket();
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setVehicleRegNumber("AAA111");
        ticket.setPrice(0.0);
        ticket.setInTime(new Date());
        ticket.setOutTime(null);

        // When
        boolean result = ticketDAO.saveTicket(ticket);
        
        // Then
        assertTrue(result, "Le ticket doit être sauvegardé avec succès");


        // Vérifier directement en base
        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM ticket WHERE vehicle_reg_number = ?");
            ps.setString(1, "AAA111");
            ResultSet rs = ps.executeQuery();

            assertTrue(rs.next(), "Le ticket doit exister en BDD après insertion");
        }
    }
    
    @Test
    void testGetTicketIntegration() throws Exception {
        TicketDAO ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = new DataBaseTestConfig();

        // Given : insertion directe dans la BDD
        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO ticket (parking_number, vehicle_reg_number, price, in_time, out_time) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, 1);
            ps.setString(2, "BBB222");
            ps.setDouble(3, 3.0);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis() - 60*60*1000)); // 1h avant
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis())); 
            ps.executeUpdate();
        }

        // When
        Ticket ticket = ticketDAO.getTicket("BBB222");

        // Then
        assertNotNull(ticket);
        assertEquals("BBB222", ticket.getVehicleRegNumber());
        assertEquals(3.0, ticket.getPrice());
}
   
    @Test
    void testUpdateTicketIntegration() throws Exception {
        TicketDAO ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = new DataBaseTestConfig();

        // Given : insertion initiale du ticket
        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, 1);
            ps.setString(2, "CCC333");
            ps.setDouble(3, 0.0);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis() - 60*60*1000));
            ps.setNull(5, Types.TIMESTAMP);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int idTicket = rs.getInt(1);
                Ticket ticket = new Ticket();
                ticket.setId(idTicket);
                ticket.setPrice(15.0);
                ticket.setOutTime(new Date());

                // When
                boolean updated = ticketDAO.updateTicket(ticket);

                // Then
                assertTrue(updated);

                // Vérification finale en base
                PreparedStatement psCheck = con.prepareStatement("SELECT price, out_time FROM ticket WHERE id = ?");
                psCheck.setInt(1, idTicket);
                ResultSet rsCheck = psCheck.executeQuery();
                assertTrue(rsCheck.next());
                assertEquals(15.0, rsCheck.getDouble("price"));
                assertNotNull(rsCheck.getTimestamp("out_time"));
            } 
        }
    }
    
    @Test
    void testGetNbTicket_WhenNoTicketExists_ShouldReturnZero() throws Exception {
        TicketDAO ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = new DataBaseTestConfig();
        String vehicleRegNumber = "ZZZ000";

        // Java : appel de la méthode
        int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
        assertEquals(0, nbTickets);

        // SQL : confirmation directe
        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT COUNT(*) FROM ticket WHERE vehicle_reg_number = ?");
            ps.setString(1, vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }
    
    @Test
    void testGetNbTicketIntegration() throws Exception {
        TicketDAO ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = new DataBaseTestConfig();

        String vehicleRegNumber = "DDD444";

         // === Cas plusieurs tickets ===
        try (Connection con = dataBaseTestConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(DBConstants.SAVE_TICKET);

            for (int i = 0; i < 3; i++) { // on insère 3 tickets
                ps.setInt(1, 1);
                ps.setString(2, vehicleRegNumber);
                ps.setDouble(3, Fare.CAR_RATE_PER_HOUR);
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis() - (24 + i) * 60 * 60 * 1000));
                ps.setTimestamp(5, new Timestamp(System.currentTimeMillis() - (23 + i) * 60 * 60 * 1000));
                ps.executeUpdate();
            }
        }
        int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
        assertEquals(3, ticketDAO.getNbTicket(vehicleRegNumber), "Doit retourner exactement 3 tickets");
    }
    
    @Test
    void testGetNextAvailableSlotIntegration() throws Exception {
        ParkingSpotDAO dao = new ParkingSpotDAO();
        dao.dataBaseConfig = new DataBaseTestConfig();

        int nextSlot = dao.getNextAvailableSlot(ParkingType.CAR);
        assertTrue(nextSlot > 0, "Il devrait exister une place de parking libre");
    }
}
    

