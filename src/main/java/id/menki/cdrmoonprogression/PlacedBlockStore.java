package id.menki.cdrmoonprogression;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlacedBlockStore {
    private static final int MAGIC = 0x43445250; // CDRP
    private static final int VERSION = 1;

    private final CdrMoonProgressionPlugin plugin;
    private final File file;
    private final Set<BlockKey> blocks = new HashSet<>();
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private volatile boolean dirty;

    public PlacedBlockStore(CdrMoonProgressionPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-blocks.dat");
    }

    public void load() {
        blocks.clear();
        if (!file.exists()) return;

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
            int magic = in.readInt();
            int version = in.readInt();
            if (magic != MAGIC || version != VERSION) {
                throw new IOException("format placed-blocks.dat tidak dikenali");
            }
            int count = in.readInt();
            if (count < 0 || count > 20_000_000) throw new IOException("jumlah entry tidak valid: " + count);
            for (int i = 0; i < count; i++) {
                UUID worldId = new UUID(in.readLong(), in.readLong());
                blocks.add(new BlockKey(worldId, in.readInt(), in.readInt(), in.readInt()));
            }
            plugin.getLogger().info("Loaded " + blocks.size() + " tracked player-placed blocks.");
        } catch (EOFException ex) {
            plugin.getLogger().severe("placed-blocks.dat terpotong/corrupt; anti-exploit placement tidak dimuat penuh.");
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal membaca placed-blocks.dat: " + ex.getMessage());
        }
        dirty = false;
    }

    public boolean contains(Block block) {
        return blocks.contains(BlockKey.from(block));
    }

    public void mark(Block block) {
        if (blocks.add(BlockKey.from(block))) dirty = true;
    }

    public boolean remove(Block block) {
        boolean removed = blocks.remove(BlockKey.from(block));
        if (removed) dirty = true;
        return removed;
    }

    public void removeAll(Collection<Block> removedBlocks) {
        boolean changed = false;
        for (Block block : removedBlocks) {
            changed |= blocks.remove(BlockKey.from(block));
        }
        if (changed) dirty = true;
    }

    public void moveTracked(List<Block> movedBlocks, BlockFace direction) {
        Set<BlockKey> sources = new HashSet<>();
        for (Block block : movedBlocks) {
            BlockKey source = BlockKey.from(block);
            if (blocks.contains(source)) sources.add(source);
        }
        if (sources.isEmpty()) return;

        blocks.removeAll(sources);
        for (BlockKey source : sources) {
            blocks.add(source.offset(direction.getModX(), direction.getModY(), direction.getModZ()));
        }
        dirty = true;
    }

    public int size() {
        return blocks.size();
    }

    public void requestAsyncSave() {
        if (!dirty || !saving.compareAndSet(false, true)) return;
        Set<BlockKey> snapshot = new HashSet<>(blocks);
        dirty = false;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                writeSnapshot(snapshot);
            } catch (IOException ex) {
                dirty = true;
                plugin.getLogger().severe("Gagal menyimpan placed-blocks.dat: " + ex.getMessage());
            } finally {
                saving.set(false);
            }
        });
    }

    public void saveSync() {
        try {
            writeSnapshot(new HashSet<>(blocks));
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("Gagal menyimpan placed-blocks.dat saat shutdown: " + ex.getMessage());
        }
    }

    private void writeSnapshot(Set<BlockKey> snapshot) throws IOException {
        if (!plugin.getDataFolder().exists()) Files.createDirectories(plugin.getDataFolder().toPath());
        File temp = new File(plugin.getDataFolder(), "placed-blocks.dat.tmp");

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temp.toPath())))) {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeInt(snapshot.size());
            for (BlockKey key : snapshot) {
                out.writeLong(key.worldId().getMostSignificantBits());
                out.writeLong(key.worldId().getLeastSignificantBits());
                out.writeInt(key.x());
                out.writeInt(key.y());
                out.writeInt(key.z());
            }
        }

        try {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        static BlockKey from(Block block) {
            return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        BlockKey offset(int dx, int dy, int dz) {
            return new BlockKey(worldId, x + dx, y + dy, z + dz);
        }
    }
}
