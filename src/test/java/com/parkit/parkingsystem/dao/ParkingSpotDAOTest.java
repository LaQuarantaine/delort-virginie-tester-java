package com.parkit.parkingsystem.dao;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.model.ParkingSpot;

import java.sql.*;

public class ParkingSpotDAOTest {
	private ParkingSpotDAO parkingSpotDAO;
    private DataBaseConfig mockDbConfig;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws Exception {
        mockDbConfig = mock(DataBaseConfig.class);
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = mockDbConfig;

        when(mockDbConfig.getConnection()).thenReturn(mockConnection);
    }

    @Test
    void testGetNextAvailableSlot_success() throws Exception {
        when(mockConnection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt(1)).thenReturn(5);

        int result = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        assertEquals(5, result);

        verify(mockPreparedStatement).setString(1, "CAR");
    }

    @Test
    void testGetNextAvailableSlot_noResult() throws Exception {
        when(mockConnection.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // pas de résultat

        int result = parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE);
        assertEquals(-1, result); // valeur par défaut si rien trouvé
    }

    @Test
    void testUpdateParking_success() throws Exception {
        when(mockConnection.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // simulate one row updated

        ParkingSpot spot = new ParkingSpot(3, ParkingType.CAR, false);
        boolean updated = parkingSpotDAO.updateParking(spot);

        assertTrue(updated);
        verify(mockPreparedStatement).setBoolean(1, false);
        verify(mockPreparedStatement).setInt(2, 3);
    }

    @Test
    void testUpdateParking_failure() throws Exception {
        when(mockConnection.prepareStatement(DBConstants.UPDATE_PARKING_SPOT)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeUpdate()).thenReturn(0); // aucune ligne mise à jour

        ParkingSpot spot = new ParkingSpot(99, ParkingType.BIKE, true);
        boolean updated = parkingSpotDAO.updateParking(spot);

        assertFalse(updated);
    }

}
