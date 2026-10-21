package main.java.ridehailing.subsystemB;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class BillingGateway {
    private final Path filePath;

    public BillingGateway(Path storageDir) {
        try {
            Files.createDirectories(storageDir);
            this.filePath = storageDir.resolve("subsystem_b_billing.csv");
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize BillingGateway storage", e);
        }
    }

    public void save(FareInvoice invoice) {
        Map<String, FareInvoice> all = findAllMap();
        all.put(invoice.getRideId(), invoice);
        writeAll(all.values());
    }

    public Optional<FareInvoice> findByRideId(String rideId) {
        return Optional.ofNullable(findAllMap().get(rideId));
    }

    public List<FareInvoice> findAll() {
        return new ArrayList<>(findAllMap().values());
    }

    public void clearAll() {
        writeAll(Collections.emptyList());
    }

    private Map<String, FareInvoice> findAllMap() {
        Map<String, FareInvoice> map = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",");
                FareInvoice inv = new FareInvoice(
                        p[0],
                        Double.parseDouble(p[1]),
                        Double.parseDouble(p[2]),
                        p[3],
                        BillingStatus.valueOf(p[4])
                );
                map.put(inv.getRideId(), inv);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read billing store", e);
        }
        return map;
    }

    private void writeAll(Collection<FareInvoice> list) {
        List<String> lines = new ArrayList<>();
        for (FareInvoice inv : list) {
            lines.add(String.join(",",
                    inv.getRideId(),
                    String.valueOf(inv.getTotalFare()),
                    String.valueOf(inv.getSurgeMultiplier()),
                    inv.getReceiptCode(),
                    inv.getStatus().name()));
        }
        try {
            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write billing store", e);
        }
    }
}