package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class InteractiveShell {

    private static final Logger logger = LogManager.getLogger("InteractiveShell");

    public static void loadInterface(){
        logger.info("App initialized!!!");
        System.out.println("Welcome to Parking System!");

        boolean continueApp = true;
        
        InputReaderUtil inputReaderUtil;
        ParkingSpotDAO parkingSpotDAO;
        TicketDAO ticketDAO;
        FareCalculatorService fareCalculatorService;
        ParkingService parkingService;

        try {
        	inputReaderUtil = new InputReaderUtil();
        	parkingSpotDAO = new ParkingSpotDAO();
        	ticketDAO = new TicketDAO();
        	fareCalculatorService = new FareCalculatorService();
        	parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO, fareCalculatorService);
        } catch (Exception e) {
        	logger.fatal("Critical error during system initialization", e);
            System.out.println("Erreur critique au démarrage, l'application va fermer !");
            return; 
        }
        
        while(continueApp){
            loadMenu();
            
            int option = inputReaderUtil.readSelection();

            if (option==-1) {
            	continue;
            }
            
            try {
            	switch(option){
                	case 1: {
                		parkingService.processIncomingVehicle();
                		break;
                	}
                	case 2: {
                		parkingService.processExitingVehicle();
                		break;
                	}
                	case 3: {
                		System.out.println("Exiting from the system!");
                		continueApp = false;
                		break;
                	}
                	default: System.out.println("Unsupported option. Please enter a number corresponding to the provided menu");
            	}
            } catch (Exception e) {
        	logger.error("An unexpected error occurred during processing", e);
            System.out.println("A technical error occurred. Please try again.");
            }
        }
    }
        

    private static void loadMenu(){
        System.out.println("Please select an option. Simply enter the number to choose an action");
        System.out.println("1 New Vehicle Entering - Allocate Parking Space");
        System.out.println("2 Vehicle Exiting - Generate Ticket Price");
        System.out.println("3 Shutdown System");
    }

}
