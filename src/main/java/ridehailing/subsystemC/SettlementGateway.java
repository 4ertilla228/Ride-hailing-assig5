package main.java.ridehailing.subsystemC;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class SettlementGateway {
    private final Path filePath;

    public SettlementGateway(Path storageDir) {
        try {
            Files.createDirectories(storageDir);
            this.filePath = storageDir.resolve("subsystem_c_settlement.csv");
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize SettlementGateway storage", e);
        }
    }

    public void save(SettlementRecord record) {
        Map<String, SettlementRecord> all = findAllMap();
        all.put(record.getRideId(), record);
        writeAll(all.values());
    }

    public Optional<SettlementRecord> findByRideId(String rideId) {
        return Optional.ofNullable(findAllMap().get(rideId));
    }

    public List<SettlementRecord> findAll() {
        return new ArrayList<>(findAllMap().values());
    }

    public void clearAll() {
        writeAll(Collections.emptyList());
    }

    private Map<String, SettlementRecord> findAllMap() {
        Map<String, SettlementRecord> map = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",");
                SettlementRecord rec = new SettlementRecord(
                        p[0], p[1],
                        Double.parseDouble(p[2]),
                        p[3],
                        SettlementStatus.valueOf(p[4])
                );
                map.put(rec.getRideId(), rec);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read settlement store", e);
        }
        return map;
    }

    private void writeAll(Collection<SettlementRecord> list) {
        List<String> lines = new ArrayList<>();
        for (SettlementRecord r : list) {
            lines.add(String.join(",",
                    r.getRideId(),
                    r.getPaymentToken(),
                    String.valueOf(r.getChargedAmount()),
                    r.getRatingTicket(),
                    r.getStatus().name()));
        }
        try {
            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write settlement store", e);
        }
    }
}