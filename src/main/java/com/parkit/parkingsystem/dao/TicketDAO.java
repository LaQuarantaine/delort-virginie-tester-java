package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;

public class TicketDAO {

    private static final Logger logger = LogManager.getLogger("TicketDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();

    public boolean saveTicket(Ticket ticket){
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = dataBaseConfig.getConnection();
            ps = con.prepareStatement(DBConstants.SAVE_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            //ps.setInt(1,ticket.getId());
            ps.setInt(1,ticket.getParkingSpot().getId());
            ps.setString(2, ticket.getVehicleRegNumber());
            ps.setDouble(3, ticket.getPrice());
            ps.setTimestamp(4, new Timestamp(ticket.getInTime().getTime()));
            
            if (ticket.getOutTime() == null) {
                ps.setNull(5, Types.TIMESTAMP); 
            } else {
                ps.setTimestamp(5, new Timestamp(ticket.getOutTime().getTime()));
            }

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
            
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
            return false;
        }finally {
        	dataBaseConfig.closePreparedStatement(ps);
            dataBaseConfig.closeConnection(con);
        }
   }


    public Ticket getTicket(String vehicleRegNumber) {
        Connection con = null;
        Ticket ticket = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_TICKET);
            //ID, PARKING_NUMBER, VEHICLE_REG_NUMBER, PRICE, IN_TIME, OUT_TIME)
            ps.setString(1,vehicleRegNumber);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                ticket = new Ticket();
                ParkingSpot parkingSpot = new ParkingSpot(rs.getInt(1), ParkingType.valueOf(rs.getString(6)),false);
                ticket.setParkingSpot(parkingSpot);
                ticket.setId(rs.getInt(2));
                ticket.setVehicleRegNumber(vehicleRegNumber);
                ticket.setPrice(rs.getDouble(3));
                ticket.setInTime(rs.getTimestamp(4));
                ticket.setOutTime(rs.getTimestamp(5));
            }
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        return ticket;
    }
 

    public boolean updateTicket(Ticket ticket) {
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_TICKET);
            ps.setDouble(1, ticket.getPrice());
            ps.setTimestamp(2, new Timestamp(ticket.getOutTime().getTime()));
            ps.setInt(3,ticket.getId());
            ps.execute();
            return true;
        }catch (Exception ex){
            logger.error("Error saving ticket info",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        return false;
    }
     
    public int getNbTicket(String vehicleRegNumber) {
    	int nbTickets = 0 ;
    	
    	try (Connection con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.NB_TICKET)) {
            	
            ps.setString(1,vehicleRegNumber);
            
            try (ResultSet rs = ps.executeQuery()){
                 if(rs.next()) {
                	 nbTickets = rs.getInt(1);
                 }
            }
         }catch (Exception ex){
            	logger.error("Error get number tickets",ex);
            }

    	return nbTickets;
    }

    // contrôler l'unicité d'une immatriculation
	public boolean isVehicleAlreadyParked(String vehicleRegNumber) {
		boolean vehicleAlreadyParked = false;
		
		try (Connection con = dataBaseConfig.getConnection();
	        PreparedStatement ps = con.prepareStatement(DBConstants.IS_VEHICLE_ALREADY_PARKED)) {
	            	
	        ps.setString(1,vehicleRegNumber);
            
	        try (ResultSet rs = ps.executeQuery()){
                 if(rs.next() && rs.getInt(1) > 0){ 
                	 vehicleAlreadyParked = true;
                 }
	        }
        } catch (Exception ex) {
            	logger.error("Erreur lors de la vérification de l'immatriculation",ex);
            }
		return vehicleAlreadyParked;
	}
}