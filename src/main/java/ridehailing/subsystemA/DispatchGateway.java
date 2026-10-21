package main.java.ridehailing.subsystemA;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DispatchGateway {
    private final Path filePath;

    public  DispatchGateway(Path storageDir) {
        try {
            Files.createDirectories(storageDir);
            this.filePath = storageDir.resolve("subsystem_A_dispatch.csv");
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось инициализировать хранилище DispatchGateway",e);
        }
    }

    public void save(DispatchReservation reservation) {
        Map<String, DispatchReservation> all = findAllMap();
        all.put(reservation.getRideId(), reservation);
        writeAll(all.values());
    }

    public Optional<DispatchReservation> findByRideId(String rideId) {
        return Optional.ofNullable(findAllMap().get(rideId));
    }

    public List<DispatchReservation> findAll() {
        return new ArrayList<>(findAllMap().values());
    }

    public void clearAll(){
        writeAll(Collections.emptyList());
    }

    private Map<String, DispatchReservation> findAllMap() {
        Map<String, DispatchReservation> map = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)){
                if (line.isBlank()) continue;
                String[] p = line.split(",");
                DispatchReservation r = new DispatchReservation(
                  p[0],p[1],p[2],
                  Double.parseDouble(p[3]),
                  Integer.parseInt(p[4]),
                  DispatchStatus.valueOf(p[5])
                );
                map.put(r.getRideId(), r);
            }
        } catch (IOException e) {
            throw new RuntimeException("Не удалось прочитать хранилище отправки",e);
        }
        return map;
    }

    private void writeAll(Collection<DispatchReservation> list) {
        List<String> lines = new ArrayList<>();
        for (DispatchReservation r : list) {
            lines.add(String.join(",",
                    r.getRideId(), r.getRiderId(), r.getDriverId(),
                    String.valueOf(r.getDistanceKm()),
                    String.valueOf(r.getEstimatedMinutes()),
                    r.getStatus().name())
            );
        }try {
            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write dispatch store", e);
        }
    }

}
