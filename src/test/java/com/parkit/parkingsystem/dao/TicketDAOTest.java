package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) 
class TicketDAOTest {

    @InjectMocks
    private TicketDAO ticketDAO; 

    @Mock
    private DataBaseConfig mockDataBaseConfig; 

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet resultSet;

    private Ticket ticket;

    @BeforeEach
    void setUp() throws Exception {
        // Simule l'obtention de la connexion
        when(mockDataBaseConfig.getConnection()).thenReturn(mockConnection);

        // Initialisation d'un ticket pour les tests
        ticket = new Ticket();
        ticket.setId(1);
        ticket.setVehicleRegNumber("ABC123");
        ticket.setPrice(10.0);
        ticket.setInTime(new Date());
        ticket.setOutTime(new Date());
        
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));

        // Simule la création d'un PreparedStatement
        lenient().when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    }
    

    @Test 
    @DisplayName("Enregistrement d'un ticket de sortie dans BDD")
    void testSaveTicket_Exiting() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // WHEN
        boolean result = ticketDAO.saveTicket(ticket);

        // THEN
        assertTrue(result); // Vérifie que le ticket de sortie est bien enregistré
        verify(mockPreparedStatement, times(1)).setInt(1, ticket.getParkingSpot().getId());
        verify(mockPreparedStatement, times(1)).setString(2, ticket.getVehicleRegNumber());
        verify(mockPreparedStatement, times(1)).setDouble(3, ticket.getPrice());
        verify(mockPreparedStatement, times(1)).setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
        verify(mockPreparedStatement, times(1)).setTimestamp(5, new Timestamp(ticket.getOutTime().getTime()));
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }


    @Test 
    @DisplayName("Enregistrement d'un ticket d'entrée dans BDD")
    void testSaveTicket_Incoming() throws Exception {
        // GIVEN
    	ticket = new Ticket();
        ticket.setId(2);
        ticket.setVehicleRegNumber("XYZ999");
        ticket.setPrice(0.0);
        ticket.setInTime(new Date());
        ticket.setOutTime(null); // Pas encore de sortie 
        ticket.setParkingSpot(new ParkingSpot(2, ParkingType.CAR, false));
       
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // WHEN
        boolean result = ticketDAO.saveTicket(ticket);

        // THEN
        assertTrue(result); // Vérifie que le ticket d'entrée est bien enregistré 
        verify(mockPreparedStatement, times(1)).setInt(1, ticket.getParkingSpot().getId());
        verify(mockPreparedStatement, times(1)).setString(2, ticket.getVehicleRegNumber());
        verify(mockPreparedStatement, times(1)).setDouble(3, ticket.getPrice());
        verify(mockPreparedStatement, times(1)).setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
        verify(mockPreparedStatement, times(1)).setNull(5, Types.TIMESTAMP); // car outTime == null
        verify(mockPreparedStatement, times(1)).executeUpdate();
    }

    @Test
    @DisplayName("Echec Enregistrement ticket lié à une exception")
    void testSaveTicket_ThrowsException() throws Exception {
        // GIVEN
        when(mockDataBaseConfig.getConnection()).thenThrow(new SQLException("Database unavailable"));

        // WHEN
        boolean result = ticketDAO.saveTicket(ticket);

        // THEN
        assertFalse(result, "Si la base est inaccessible, saveTicket() doit retourner false");

        // Vérifier que l'exception a bien été loggée
        verify(mockPreparedStatement, never()).execute(); // La requête ne doit jamais être exécutée
    }
    
    @Test
    @DisplayName("saveTicket retourne false si aucune ligne n'est insérée")
    void testSaveTicket_WhenExecuteUpdateReturnsZero() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);

        // WHEN
        boolean result = ticketDAO.saveTicket(ticket);

        // THEN
        assertFalse(result, "saveTicket doit retourner false si aucune ligne insérée");
    }
    

    @Test
    @DisplayName("Récupérer un ticket existant dans BDD")
    void testGetTicket_() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(3);
        when(resultSet.getInt(2)).thenReturn(12);
        when(resultSet.getDouble(3)).thenReturn(8.5);
        when(resultSet.getTimestamp(4)).thenReturn(new Timestamp(ticket.getInTime().getTime()));
        when(resultSet.getTimestamp(5)).thenReturn(new Timestamp(ticket.getOutTime().getTime()));
        when(resultSet.getString(6)).thenReturn("CAR");
        
        // WHEN
        Ticket result = ticketDAO.getTicket("ABC123");

        // THEN
        assertNotNull(result);
        assertEquals("ABC123", result.getVehicleRegNumber());
        assertEquals(8.5, result.getPrice());
        assertEquals(3, result.getParkingSpot().getId());
    }

    
    @Test
    @DisplayName("Echec récupération ticket lié à une exception")
    void testGetTicket_ThrowsException() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Query failed"));

        // WHEN
        Ticket result = ticketDAO.getTicket("ABC123");

        // THEN
        assertNull(result, "Si erreur, `getTicket` doit retourner `null`");

        // Vérifier que la requête SQL n'a pas avancé
        verify(resultSet, never()).next(); // Le ResultSet ne doit jamais être parcouru
    }
    
    
    @Test
    @DisplayName("Echec mise à jour ticket lié à exception")
    void testUpdateTicket_WhenExecuteFails() throws Exception {
        // GIVEN
        when(mockPreparedStatement.execute()).thenThrow(new SQLException("Update failed"));

        // WHEN
        boolean result = ticketDAO.updateTicket(ticket);

        // THEN
        assertFalse(result, "Si l'update échoue, la méthode doit retourner `false`");

        // Vérifier que la requête a bien été tentée
        verify(mockPreparedStatement, times(1)).setDouble(1, ticket.getPrice());
    }
    

    @Test
    @DisplayName("Compte le nombre de tickets pour un véhicule")
    void testGetNbTicket() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(3); // l'utilisateur a 3 tickets

        // WHEN
        int nbTickets = ticketDAO.getNbTicket("ABC123");

        // THEN
        assertEquals(3, nbTickets);
    }
    
    @Test
    @DisplayName("Renvoi 0 tickets si pas de ticket en BDD")
    void testGetNbTicket_WhenNoTicketExists() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // WHEN
        int nbTickets = ticketDAO.getNbTicket("DEF123");

        // THEN
        assertEquals(0, nbTickets);
    }

    
    @Test
    @DisplayName("Echec comptage nb ticket suite à erreur de la BDD")  
    void TestGetNbTicket_WhenDatabaseFails() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenThrow(new SQLException("Count query failed"));

        // WHEN
        int nbTickets = ticketDAO.getNbTicket("ABC123");

        // THEN
        assertEquals(0, nbTickets, "En cas d'erreur, `getNbTicket` doit retourner `0`");

        // Vérifier que la requête SQL a été tentée
        verify(mockPreparedStatement, times(1)).executeQuery();
    }
    
    @Test
    @DisplayName("Vérifie si un véhicule(immatriculation) est déjà stationner")   
    void testIsVehicleAlreadyParked() throws Exception {
        // GIVEN
        when(mockPreparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(1); // Supposons qu'il y a déjà un ticket actif

        // WHEN
        boolean isParked = ticketDAO.isVehicleAlreadyParked("ABC123");

        // THEN
        assertTrue(isParked);
    }
}