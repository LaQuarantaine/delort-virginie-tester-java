package com.parkit.parkingsystem;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;

class ParkingSpotTest {

	@Test
    @DisplayName("Vérifier que ParkingSpot est correctement instancié")
    void shouldCreateParkingSpotWithCorrectAttributes() {
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR, true);

        assertEquals(1, parkingSpot.getId());
        assertEquals(ParkingType.CAR, parkingSpot.getParkingType());
        assertTrue(parkingSpot.isAvailable());
    }
}
