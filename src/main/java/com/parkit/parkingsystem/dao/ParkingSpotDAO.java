package com.parkit.parkingsystem.dao;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.DBConstants;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.model.ParkingSpot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ParkingSpotDAO {
    private static final Logger logger = LogManager.getLogger("ParkingSpotDAO");

    public DataBaseConfig dataBaseConfig = new DataBaseConfig();
 
    public int getNextAvailableSlot(ParkingType parkingType){
        Connection con = null; //déclare une connection
        int result= -1; //permet de déclarer aucun emplacement trouvé par défaut
        
        try {
            con = dataBaseConfig.getConnection();	//ouvre une connection à la BDD
            PreparedStatement ps = con.prepareStatement(DBConstants.GET_NEXT_PARKING_SPOT); //crée une requete pré-compilée. La constante contient la requete
            
            ps.setString(1, parkingType.toString()); //remplace le ? dans la requete 
            ResultSet rs = ps.executeQuery(); //exécute la requete et place le résultat dans la variable
            
            if(rs.next()){	//si la requete retourne un résultat
            	result = rs.getInt(1);
            }
            
            logger.info("Place trouvée pour " + parkingType + " : " + result);
            
            dataBaseConfig.closeResultSet(rs);
            dataBaseConfig.closePreparedStatement(ps);
        }catch (Exception ex){
            logger.error("Error fetching next available slot",ex);
        }finally {
            dataBaseConfig.closeConnection(con);
        }
        return result;
    }

    public boolean updateParking(ParkingSpot parkingSpot){
        //update the availability fo that parking slot
        Connection con = null;
        try {
            con = dataBaseConfig.getConnection();
            PreparedStatement ps = con.prepareStatement(DBConstants.UPDATE_PARKING_SPOT);
            ps.setBoolean(1, parkingSpot.isAvailable());
            ps.setInt(2, parkingSpot.getId());
            int updateRowCount = ps.executeUpdate(); // exécute la requete et retourne le nb de ligne
            dataBaseConfig.closePreparedStatement(ps);
            return (updateRowCount == 1); // 1 ligne mise à jour
        }catch (Exception ex){
            logger.error("Error updating parking info",ex);
            return false;
        }finally {
            dataBaseConfig.closeConnection(con);
        }
    }



}
