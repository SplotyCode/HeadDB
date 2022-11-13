package tsp.headdb.core.storage;

import tsp.headdb.HeadDB;
import tsp.warehouse.storage.file.SerializableFileDataManager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerStorage extends SerializableFileDataManager<Collection<PlayerData>> {

    private final Map<UUID, PlayerData> players = new HashMap<>();

    public PlayerStorage(Storage storage) {
        super(new File("data/players.data"), storage.getExecutor());
    }

    public void set(PlayerData data) {
        this.players.put(data.uniqueId(), data);
    }

    public Optional<PlayerData> get(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    public Map<UUID, PlayerData> getPlayersMap() {
        return Collections.unmodifiableMap(players);
    }

    public void init() {
        load().whenComplete((data, ex) -> {
            for (PlayerData entry : data) {
                players.put(entry.uniqueId(), entry);
            }

            HeadDB.getInstance().getLog().debug("Loaded " + players.values().size() + " player data!");
        });
    }

    public void backup() {
        save(players.values()).whenComplete((success, ex) -> HeadDB.getInstance().getLog().debug("Saved " + players.values().size() + " player data!"));
    }

    public void suspend() {
        Boolean saved = save(players.values())
                .exceptionally(ex -> {
                    HeadDB.getInstance().getLog().error("Failed to save player data! | Stack Trace: ");
                    ex.printStackTrace();
                    return false;
                })
                .join();

        if (Boolean.TRUE.equals(saved)) {
            HeadDB.getInstance().getLog().debug("Saved " + players.values().size() + " player data!");
        }
    }

}