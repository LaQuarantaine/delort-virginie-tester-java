package com.parkit.parkingsystem.constants;

public class Fare {
    public static final double BIKE_RATE_PER_HOUR = 1.0;
    public static final double CAR_RATE_PER_HOUR = 1.5;
    
  //ajout constantes métier
  	public static final double COEF_DISCOUNT = 0.95;	// soit 5% de remise pour les utilisateurs réguliers
  	public static final int FREE_PARKING_DURATION = 30 * 60 * 1000; // soit 30 min en millissecondes, durée de stationnement gratuite
}
