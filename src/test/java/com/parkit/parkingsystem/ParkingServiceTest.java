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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
    @DisplayName("Initialisation du service à chaque test")
    public void initService() {
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
    }
 
    
    @Nested
    @DisplayName ("Tests Process_Incoming_Vehicle")
    class ProcessIncomingVehicleTests {
    	

    @Test
    @DisplayName("Vérifier que processIncomingVehicle() s'arrête si vehicleRegNumber est null")
    public void processIncomingVehicle_ShouldStop_WhenVehicleRegNumberIsNull() throws Exception {
    	    // Given: entrée de véhicule, place disponible (auto ou moto)
    	when(inputReaderUtil.readSelection()).thenReturn(1);
    	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);

    	    // Given: on simule une exception qui entraîne un `null` pour vehicleRegNumber
    	when(inputReaderUtil.readVehicleRegistrationNumber()).thenThrow(new Exception());

    	    // When
    	parkingService.processIncomingVehicle();

    	    // Then: Vérifier que la méthode s'arrête avant d'enregistrer un ticket
    	 verify(ticketDAO, never()).saveTicket(any(Ticket.class));
    	}
    

    @Test
    @DisplayName("Vérifier que l'entrée est refusée s'il n'y a plus de place BIKE")
    public void processIncomingVehicle_ShouldStop_WhenNoBikeParkingAvailable() throws Exception {
        // Given : l'utilisateur choisit une moto (2), Aucune place BIKE disponible
        when(inputReaderUtil.readSelection()).thenReturn(2); 
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(0);  

        // When
        parkingService.processIncomingVehicle();

        // Then : le processus d'entrée est interrompu. Vérifie qu'aucun ticket n'est créé et qu'aucune place n'est mise à jour
        verify(ticketDAO, never()).saveTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    

    @Test
    @DisplayName("Vérifier que processIncomingVehicle() s'arrête si aucune place de parking n'est disponible")
    public void processIncomingVehicle_ShouldStop_IfNoParkingSpotAvailable() {
        // Given: Simuler la saisie d'entrée / Aucune place de parking n'est disponible
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(0);

        // When: Appel de la méthode
        parkingService.processIncomingVehicle();

        // Then: Vérifier que la méthode n'a pas continué après le `return`
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));  
        verify(ticketDAO, never()).saveTicket(any(Ticket.class));  
    }
    

    @Test
    @DisplayName("Vérifier que le ticket est sauvegardé lors de l'entrée d'un véhicule")
    public void processIncomingVehicle_shouldSaveTicketOnly() throws Exception {
    	// Given
    	when(inputReaderUtil.readSelection()).thenReturn(1); 
    	when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
    	when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
        when(ticketDAO.isVehicleAlreadyParked("ABC123")).thenReturn(false);

        // When
        parkingService.processIncomingVehicle();

        // Then
    	verify(ticketDAO, Mockito.times(1)).saveTicket(any(Ticket.class));
    }
    

    @Test
    @DisplayName("Vérifier qu'une place de parking CAR est bien demandée lors de l'entrée d'un véhicule")
    public void processIncomingVehicle_ShouldRequestCarParkingSpot() {
        // Given
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);

        // When
        parkingService.processIncomingVehicle();

        // Then
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(eq(ParkingType.CAR));
    }
    

    @Test
    @DisplayName("Vérifier qu'une place de parking BIKE est bien demandée lors de l'entrée d'un véhicule")
    public void processIncomingVehicle_ShouldUseBikeType_WhenUserSelects2() {
        // Given: Simuler la sélection utilisateur (2 = BIKE), une place de parking BIKE est disponible (place 5)
        when(inputReaderUtil.readSelection()).thenReturn(2);
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE)).thenReturn(5);

        // When: Appel de la méthode principale 
        parkingService.processIncomingVehicle();

        // Then: Vérifier que la méthode a bien récupéré une place BIKE
        verify(parkingSpotDAO, times(1)).getNextAvailableSlot(eq(ParkingType.BIKE)); 
    }
    

    @Test
    @DisplayName("Vérifier si utilisateur régulier")
    public void processIncomingVehicle_ShouldCheckUserRegular() throws Exception {
        // Given: Simuler une saisie CAR, place CAR dispo, saisie immat, vérif immat
        when(inputReaderUtil.readSelection()).thenReturn(1);  
        when(parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR)).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        when(ticketDAO.isVehicleAlreadyParked("ABCDEF")).thenReturn(false);
        when(ticketDAO.getNbTicket("ABCDEF")).thenReturn(2);

        // When
        parkingService.processIncomingVehicle();

        // Then: Vérifier que la méthode `getNbTicket()`est appelée
        verify(ticketDAO, times(1)).getNbTicket("ABCDEF");
    }
    
    
    @Test
    @DisplayName("Vérifier que processIncomingVehicle() s'arrête si le véhicule est déjà garé")
    public void processIncomingVehicle_ShouldStop_IfVehicleAlreadyParked() throws Exception {
        // Given: Simuler une saisie utilisateur valide pour le type de véhicule
        when(inputReaderUtil.readSelection()).thenReturn(1);
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");

        // Given: Simuler que le véhicule est déjà garé
        when(ticketDAO.isVehicleAlreadyParked("ABC123")).thenReturn(true);

        // When: Appel de la méthode
        parkingService.processIncomingVehicle();

        // Then: Vérifier que la méthode s'arrête immédiatement et ne continue pas
        verify(ticketDAO, never()).saveTicket(any(Ticket.class));  
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    

    @Test
    @DisplayName("Vérifier que la place de parking est mise à jour comme occupée")
    public void processIncomingVehicle_ShouldUpdateParkingSpot() throws Exception {
        // Given
        when(inputReaderUtil.readSelection()).thenReturn(1); // Menu : Entrée
        when(parkingSpotDAO.getNextAvailableSlot(any(ParkingType.class))).thenReturn(1);
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
        when(ticketDAO.isVehicleAlreadyParked("ABC123")).thenReturn(false);

        // When
        parkingService.processIncomingVehicle();

        // Then
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }
    
    }  
    
    
    @Nested
    @DisplayName("Tests Process_Exiting_Vehicle")
    class ProcessExistingVehicle {	  
    
    
    @Test
    @DisplayName("Vérifier que processExitingVehicle() applique la remise si l'utilisateur est régulier")
    public void processExitingVehicle_ShouldApplyDiscount_IfUserRegular() throws Exception {
        // Given: Simuler une saisie utilisateur valide (immatriculation)
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");

        // Given: Simuler que l'utilisateur a déjà utilisé le parking plus d'une fois
        when(ticketDAO.getNbTicket("ABC123")).thenReturn(2);

        // Given: Simuler un ticket existant
        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABC123");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1 heure avant
        when(ticketDAO.getTicket("ABC123")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // When: Exécution de `processExitingVehicle()`
        parkingService.processExitingVehicle();

        // Then: Vérifier que la remise est bien appliquée
        verify(fareCalculatorService, times(1)).calculateFare(any(Ticket.class), eq(true));
    }
    

    @Test
    @DisplayName("Vérifier que processExitingVehicle() gère les exceptions correctement")
    public void processExitingVehicle_ShouldLogError_WhenExceptionOccurs() throws Exception {
        // Given: Simuler une exception dans `getTicket()`
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
        when(ticketDAO.getTicket(anyString())).thenThrow(new RuntimeException("DB error"));

     // When
        assertDoesNotThrow(() -> parkingService.processExitingVehicle());

        // Then: Vérifier qu'aucune action supplémentaire n'est effectuée
        verify(ticketDAO, never()).updateTicket(any(Ticket.class));
        verify(parkingSpotDAO, never()).updateParking(any(ParkingSpot.class));
    }
    

    @Test
    @DisplayName("Vérifier que la place de parking est libérée lors de la sortie d'un véhicule")
    public void processExitingVehicle_ShouldUpdateParkingSpot_WhenVehicleExits() throws Exception {
        // Given
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("XYZ123");

        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("XYZ123");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000))); // 1h avant

        when(ticketDAO.getTicket("XYZ123")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // When
        parkingService.processExitingVehicle();

        // Then
        verify(parkingSpotDAO, times(1)).updateParking(any(ParkingSpot.class));
    }
    

    @Test
    @DisplayName("Vérifier que l'utilisateur occasionnel ne bénéficie pas de la remise")
    public void processExitingVehicle_ShouldNotApplyDiscount_IfUserNotRegular() throws Exception {
        // Given : utilisateur avec un seul ticket (occasionnel)
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("XYZ123");
        when(ticketDAO.getNbTicket("XYZ123")).thenReturn(1);

        Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("XYZ123");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - (60 * 60 * 1000)));

        when(ticketDAO.getTicket("XYZ123")).thenReturn(ticket);
        when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(true);

        // When
        parkingService.processExitingVehicle();

        // Then : la remise (boolean) ne doit PAS être appliquée
        verify(fareCalculatorService, times(1)).calculateFare(any(Ticket.class));
    }   
    
    
    @Test
    @DisplayName("Vérifier que si la mise à jour du ticket échoue, la mise à jour du parking n'a pas lieu ")
    public void processExitingVehicleUnableUpdate() throws Exception {
    	// Given
    	when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABC123");
    	Ticket ticket = new Ticket();
        ticket.setVehicleRegNumber("ABC123");
        ticket.setParkingSpot(new ParkingSpot(1, ParkingType.CAR, false));
        ticket.setInTime(new Date(System.currentTimeMillis() - 60 * 60 * 1000));
    	when(ticketDAO.getTicket("ABC123")).thenReturn(ticket);
    	
    	when(ticketDAO.updateTicket(any(Ticket.class))).thenReturn(false);
        
        // When
	    parkingService.processExitingVehicle();
	    
	    // Then
    	verify(ticketDAO, Mockito.times(1)).updateTicket(any(Ticket.class));
    	verify(parkingSpotDAO, Mockito.never()).updateParking(any(ParkingSpot.class)); // aucune mise à jour de la place n’a été faite
    }
    


    @Test
    @DisplayName("Vérifier que processExitingVehicle() s'arrête si aucun ticket n'est trouvé")
    public void processExitingVehicle_ShouldStop_WhenTicketIsNull() throws Exception {
        // Given: Simuler une immatriculation valide
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("XYZ789");

        // Given: Simuler que `getTicket()` retourne `null`
        when(ticketDAO.getTicket("XYZ789")).thenReturn(null);

        // When
        parkingService.processExitingVehicle();

        // Then: Vérifier que la méthode s'arrête avant de calculer le tarif
        verify(fareCalculatorService, never()).calculateFare(any(Ticket.class));
        verify(ticketDAO, never()).updateTicket(any(Ticket.class));
    }
    }
    
}