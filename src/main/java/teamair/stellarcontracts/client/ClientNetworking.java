package teamair.stellarcontracts.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import teamair.stellarcontracts.StellarContracts;

import java.util.UUID;

@Environment(EnvType.CLIENT)
final class ClientNetworking {
    private static final Logger LOGGER = LogManager.getLogger(StellarContracts.class);

    static void spawnNonLivingEntity(PacketContext context, PacketByteBuf buf) {
        int entityId = buf.readVarInt();
        UUID entityUuid = buf.readUuid();
        Identifier typeId = buf.readIdentifier();

        // Verify we actually have the type the game is trying to spawn, otherwise release the packet and stop
        if (!Registry.ENTITY_TYPE.containsId(typeId)) {
            LOGGER.warn("Recieved invalid spawn packet for type {}", typeId);
            buf.release();
        }

        EntityType<?> entityType = Registry.ENTITY_TYPE.get(typeId);

        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int pitch = buf.readByte();
        int yaw = buf.readByte();
        int velocityX = buf.readShort();
        int velocityY = buf.readShort();
        int velocityZ = buf.readShort();

        context.getTaskQueue().execute(() -> {
            ClientWorld world = (ClientWorld) context.getPlayer().getEntityWorld();
            Entity entity = entityType.create(world);

            if (entity != null) {
                entity.updateTrackedPosition(x, y, z);
                entity.pitch = (pitch * 360) / 256.0F;
                entity.yaw = (yaw * 360) / 256.0F;
                entity.setEntityId(entityId);
                entity.setUuid(entityUuid);
                entity.setVelocity(velocityX, velocityY, velocityZ);

                world.addEntity(entityId, entity);
            }
        });
    }

    private ClientNetworking() {
    }
}
