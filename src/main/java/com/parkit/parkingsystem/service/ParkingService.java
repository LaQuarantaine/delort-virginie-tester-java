package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
 
public class ParkingService {

    private static final Logger logger = LogManager.getLogger("ParkingService");

    private FareCalculatorService fareCalculatorService;

    private InputReaderUtil inputReaderUtil;
    private ParkingSpotDAO parkingSpotDAO;
    private  TicketDAO ticketDAO; 
 
    public ParkingService(InputReaderUtil inputReaderUtil, ParkingSpotDAO parkingSpotDAO, TicketDAO ticketDAO, FareCalculatorService fareCalculatorService){
        this.inputReaderUtil = inputReaderUtil;
        this.parkingSpotDAO = parkingSpotDAO;
        this.ticketDAO = ticketDAO;
        this.fareCalculatorService = fareCalculatorService;
    }

     
    public void processIncomingVehicle() {
        try{
            ParkingSpot parkingSpot = getNextParkingNumberIfAvailable();
            
            if (parkingSpot == null) {
            	return;
            }

            String vehicleRegNumber = getVehicleRegNumber();
            
            // ajout d'un controle dans BDD sur l'unicité de l'immatriculation
            if (ticketDAO.isVehicleAlreadyParked(vehicleRegNumber)) {
                System.out.println("Un véhicule avec cette immatriculation est déjà garé !");
                return;
            }
            // sauvegarde de la table parking déplacée APRES maj table ticket pour ne pas bloquer une place si aucun ticket n'est généré

            Date inTime = new Date();
            Ticket ticket = new Ticket();
                //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
                //ticket.setId(ticketID);
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(vehicleRegNumber);
            ticket.setPrice(0);
            ticket.setInTime(inTime);
            ticket.setOutTime(null);
            
            ticketDAO.saveTicket(ticket);
                
                //mise à jour BDD déplacée APRES enregistrement du ticket
            parkingSpot.setAvailable(false); 
            parkingSpotDAO.updateParking(parkingSpot);//allot this parking space and mark it's availability as false
                
            int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
            
            //ajout de logger en remplacement de messages console inapproprié pour un utilisateur
            logger.info("Generated Ticket and saved in DB");
            logger.info("Recorded in-time for vehicle number :"+vehicleRegNumber+" is: "+inTime );
            
            if (nbTickets > 1) {
                System.out.println("Heureux de vous revoir ! En tant qu’utilisateur régulier de notre parking, vous allez obtenir une remise de 5%");
                System.out.println("Please park your vehicle in spot number:"+parkingSpot.getId());
            } else {
                System.out.println("Please park your vehicle in spot number:"+parkingSpot.getId());
            } 

        }catch(Exception e){
            logger.error("Unable to process incoming vehicle",e);
        }
    }
 
    private String getVehicleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    public ParkingSpot getNextParkingNumberIfAvailable(){
    	ParkingSpot parkingSpot = null;
        
        try{
            ParkingType parkingType = getVehicleType();
                        
            int parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
                        
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
                System.out.println("Soyez le(la) bienvenu(e) !");
            }else{
            	System.out.println ("Aucune place " +parkingType+ " disponible"); //ajout message console
            }
        }catch(IllegalArgumentException ie){
            logger.error("Error parsing user input for type of vehicle", ie);
            
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
        }
        
        return parkingSpot;
    }

    private ParkingType getVehicleType(){
        System.out.println("Please select vehicle type from menu");
        System.out.println("1 CAR");
        System.out.println("2 BIKE");
        int input = inputReaderUtil.readSelection();
        switch(input){
            case 1: {
                return ParkingType.CAR;
            }
            case 2: {
                return ParkingType.BIKE;
            }
            default: {
                System.out.println("Incorrect input provided");
                throw new IllegalArgumentException("Entered input is invalid");
            }
        }
    }

    public void processExitingVehicle() {
        try{
            String vehicleRegNumber = getVehicleRegNumber();
            Ticket ticket = ticketDAO.getTicket(vehicleRegNumber);
            
            // message utilisateur en cas d'erreur de saisie de l'immatriculation
            if (ticket == null) {
                System.out.println("Erreur : Aucun ticket trouvé pour le véhicule " + vehicleRegNumber);
                return;
            }

            /** message utilisateur si la sortie a deja été enregistrée afin de ne pas recalculer un prix
            concretement un utilisateur aurait selectionner sortie dans le menu au lieu d'entrer
            if (ticket.getOutTime() != null) {
                System.out.println("Erreur : Ce véhicule a déjà quitté le parking ! ");
                return;
            }
            */
            
            Date outTime = new Date();
            ticket.setOutTime(outTime);
            
            // adapter le tarif en fonction du type d'utilisateur (régulier ou occassionnel)
            if (ticketDAO.getNbTicket(vehicleRegNumber) > 1) {
            	fareCalculatorService.calculateFare(ticket, true);
            } else {
            	fareCalculatorService.calculateFare(ticket);
            }
            
            if(ticketDAO.updateTicket(ticket)) {
                ParkingSpot parkingSpot = ticket.getParkingSpot();
                parkingSpot.setAvailable(true);
                parkingSpotDAO.updateParking(parkingSpot);
                System.out.println("Please pay the parking fare:" + ticket.getPrice());
                System.out.println("Recorded out-time for vehicle number:" + ticket.getVehicleRegNumber() + " is:" + outTime);
            }else{
                System.out.println("Unable to update ticket information. Error occurred");
            }
        }catch(Exception e){
            logger.error("Unable to process exiting vehicle",e);
        }
    }
    
}
