package main.java.ridehailing.subsystemA;

public class GpsAndDriverLogic {
    public int calculateRouteEtaMinutes(String pickup, String dropoff, double distanceKm){
        if (pickup == null || pickup.isBlank() || dropoff == null || dropoff.isBlank()){
            throw new IllegalArgumentException("места приема и высадки пассажиров не должны быть пустыми");
        }
        if (pickup.equalsIgnoreCase(dropoff)){
            throw new IllegalArgumentException("Отправка и выгрузка не могут осуществляться в одном и том же месте");
        }
        if (distanceKm <= 0 || distanceKm > 500){
            throw new IllegalArgumentException("Недопустимое расстояние по маршруту:"+ distanceKm+" km");
        }
        return (int) Math.ceil(distanceKm * 2.5);
    }

    public String matchAvailableDriver(String pickupZone, double distanceKm) {
        if ("NO_DRIVERS_ZONE".equalsIgnoreCase(pickupZone)) {
            throw new IllegalStateException("No available drivers in zone: " + pickupZone);
        }
        return "DRV-" + Math.abs(pickupZone.hashCode() % 900 + 100);
    }

}
