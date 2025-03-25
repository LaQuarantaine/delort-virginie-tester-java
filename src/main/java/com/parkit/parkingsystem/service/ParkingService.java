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
            if (vehicleRegNumber == null) {
            	return;
            }      
            	
            if (ticketDAO.isVehicleAlreadyParked(vehicleRegNumber)) { 
            	System.out.println("Un véhicule avec cette immatriculation est déjà garé !");
            	return;
            }
            
            Ticket ticket = new Ticket();
            ticket.setParkingSpot(parkingSpot);
            ticket.setVehicleRegNumber(vehicleRegNumber);
            ticket.setInTime(new Date());
            ticket.setPrice(0); 
            
            ticketDAO.saveTicket(ticket);
            logger.info("Generated Ticket and saved in DB for vehicle: " + vehicleRegNumber);
            
            parkingSpot.setAvailable(false);
            parkingSpotDAO.updateParking(parkingSpot);
        	logger.info("Recorded in-time for vehicle number :"+vehicleRegNumber+" is: "+ ticket.getInTime());

        	discount(vehicleRegNumber);
        	
            System.out.println("Veuillez vous garer à l'emplacement : " + parkingSpot.getId());
            
        }catch(Exception e){
            logger.error("Unable to process incoming vehicle",e);
        }
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
    
    private ParkingSpot getNextParkingNumberIfAvailable(){
    	ParkingSpot parkingSpot = null;
        
        try{
            ParkingType parkingType = getVehicleType();
            if (parkingType == null) {
                logger.error("Le type de véhicule n'est pas défini !");
                return null;
            }
            
            int parkingNumber = parkingSpotDAO.getNextAvailableSlot(parkingType);
            logger.info("Numéro de place récupéré par parkingSpotDAO : " + parkingNumber);
            
            if(parkingNumber > 0){
                parkingSpot = new ParkingSpot(parkingNumber,parkingType, true);
                System.out.println("Soyez le(la) bienvenu(e) !");
            }else{
            	System.out.println ("Aucune place " +parkingType+ " disponible"); 
            	return null;
            }
     
            
        }catch(Exception e){
            logger.error("Error fetching next available parking slot", e);
            return null;
        }
        return parkingSpot;
    }
    
    private String getVehicleRegNumber() throws Exception {
        System.out.println("Please type the vehicle registration number and press enter key");
        return inputReaderUtil.readVehicleRegistrationNumber();
    }

    private boolean discount(String vehicleRegNumber) {
        int nbTickets = ticketDAO.getNbTicket(vehicleRegNumber);
        if (nbTickets > 1) {
            System.out.println("Heureux de vous revoir ! Vous bénéficiez d'une remise de 5%.");
            return true;
        }
        return false;
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