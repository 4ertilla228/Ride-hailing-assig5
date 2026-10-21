package main.java.ridehailing.facade;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ChainStateGateway {
    private final Path filePath;

    public ChainStateGateway(Path storageDir) {
        try {
            Files.createDirectories(storageDir);
            this.filePath = storageDir.resolve("chain_progress.csv");
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize ChainStateGateway", e);
        }
    }

    public void save(RideChainState state) {
        Map<String, RideChainState> all = findAllMap();
        all.put(state.getRideId(), state);
        writeAll(all.values());
    }

    public Optional<RideChainState> findByRideId(String rideId) {
        return Optional.ofNullable(findAllMap().get(rideId));
    }

    public List<RideChainState> findAll() {
        return new ArrayList<>(findAllMap().values());
    }

    public void clearAll() {
        writeAll(Collections.emptyList());
    }

    private Map<String, RideChainState> findAllMap() {
        Map<String, RideChainState> map = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(filePath)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", 4);
                RideChainState s = new RideChainState(
                        p[0], p[1],
                        ChainStageStatus.valueOf(p[2]),
                        p.length > 3 ? p[3] : ""
                );
                map.put(s.getRideId(), s);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chain store", e);
        }
        return map;
    }

    private void writeAll(Collection<RideChainState> list) {
        List<String> lines = new ArrayList<>();
        for (RideChainState s : list) {
            String safeNote = s.getNote() == null ? "" : s.getNote().replace(",", ";");
            lines.add(String.join(",", s.getRideId(), s.getRiderId(), s.getStatus().name(), safeNote));
        }
        try {
            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write chain store", e);
        }
    }
}