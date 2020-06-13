package teamair.stellarcontracts.entity;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import teamair.stellarcontracts.StellarContracts;
import teamair.stellarcontracts.util.RandomUtilities;

public class RocketEntityMk1 extends Entity {
    public static final Identifier SPAWN_PACKET = StellarContracts.id("spawn/rocket_mk1");
    private static final TrackedData<Integer> DAMAGE_WOBBLE_TICKS = DataTracker.registerData(RocketEntityMk1.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> DAMAGE_WOBBLE_SIDE = DataTracker.registerData(RocketEntityMk1.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> DAMAGE_WOBBLE_STRENGTH = DataTracker.registerData(RocketEntityMk1.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Boolean> LAUNCHED = DataTracker.registerData(RocketEntityMk1.class, TrackedDataHandlerRegistry.BOOLEAN);

    public RocketEntityMk1(EntityType<? extends RocketEntityMk1> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(DAMAGE_WOBBLE_TICKS, 0);
        this.dataTracker.startTracking(DAMAGE_WOBBLE_SIDE, 1);
        this.dataTracker.startTracking(DAMAGE_WOBBLE_STRENGTH, 0.0F);
        this.dataTracker.startTracking(LAUNCHED, false);
    }

    @Override
    protected void readCustomDataFromTag(CompoundTag tag) {
    }

    @Override
    protected void writeCustomDataToTag(CompoundTag tag) {
    }

    @Override
    public Box getHardCollisionBox(Entity collidingEntity) {
        return collidingEntity.isPushable() ? collidingEntity.getBoundingBox() : null;
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        return attacker instanceof PlayerEntity && !this.world.canPlayerModifyAt((PlayerEntity) attacker, this.getBlockPos());
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (!this.world.isClient() && !this.removed) {
            if (this.isInvulnerableTo(source)) {
                return false;
            }

            this.setDamageWobbleSide(-this.getDamageWobbleSide());
            this.setDamageWobbleTicks(10);
            this.scheduleVelocityUpdate();
            this.setDamageWobbleStrength(this.getDamageWobbleStrength() + amount * 10.0F);

            boolean isCreative = source.getAttacker() instanceof PlayerEntity && ((PlayerEntity) source.getAttacker()).abilities.creativeMode;

            if (isCreative || this.getDamageWobbleStrength() > 40.0F) {
                if (isCreative && !this.hasCustomName()) {
                    this.remove();
                } else {
                    // TODO: Drop contents
                    this.remove();

                    if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                        // TODO: Drop the item with the custom name
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean collides() {
        return !this.removed;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (player.world.isClient()) {
            return super.interact(player, hand);
        }

        this.setLaunched(true);
        return ActionResult.SUCCESS; // TODO: Implement inventory?
    }

    @Override
    public Packet<?> createSpawnPacket() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeVarInt(this.getEntityId());
        buf.writeUuid(this.getUuid());
        buf.writeIdentifier(Registry.ENTITY_TYPE.getId(this.getType()));
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());
        buf.writeByte(MathHelper.floor(this.pitch * 256.0F / 360.0F));
        buf.writeByte(MathHelper.floor(this.yaw * 256.0F / 360.0F));
        buf.writeShort((int) (MathHelper.clamp(this.getVelocity().getX(), -3.9D, 3.9D) * 8000.0D));
        buf.writeShort((int) (MathHelper.clamp(this.getVelocity().getY(), -3.9D, 3.9D) * 8000.0D));
        buf.writeShort((int) (MathHelper.clamp(this.getVelocity().getZ(), -3.9D, 3.9D) * 8000.0D));

        return ServerSidePacketRegistry.INSTANCE.toPacket(SPAWN_PACKET, buf);
    }

    public void setDamageWobbleStrength(float strength) {
        this.dataTracker.set(DAMAGE_WOBBLE_STRENGTH, strength);
    }

    public float getDamageWobbleStrength() {
        return this.dataTracker.get(DAMAGE_WOBBLE_STRENGTH);
    }

    public void setDamageWobbleTicks(int wobbleTicks) {
        this.dataTracker.set(DAMAGE_WOBBLE_TICKS, wobbleTicks);
    }

    public int getDamageWobbleTicks() {
        return this.dataTracker.get(DAMAGE_WOBBLE_TICKS);
    }

    public void setDamageWobbleSide(int wobbleSide) {
        this.dataTracker.set(DAMAGE_WOBBLE_SIDE, wobbleSide);
    }

    public int getDamageWobbleSide() {
        return this.dataTracker.get(DAMAGE_WOBBLE_SIDE);
    }

    public void setLaunched(boolean launched) {
        this.dataTracker.set(LAUNCHED, launched);
    }

    public boolean isLaunched() {
        return this.dataTracker.get(LAUNCHED);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getDamageWobbleTicks() > 0) {
            this.setDamageWobbleTicks(this.getDamageWobbleTicks() - 1);
        }

        if (this.getDamageWobbleStrength() > 0.0F) {
            this.setDamageWobbleStrength(this.getDamageWobbleStrength() - 1.0F);
        }

        if (this.getY() < -64.0D) {
            this.destroy();
        }

        // For testing only
        if (this.getY() > 300.0D) {
            this.destroy();
        }

        if (isLaunched()) {

            // Move the rocket to space
            this.addVelocity(0, 0.01, 0);
            this.move(MovementType.SELF, getVelocity());

            // We don't need the particles to be sync with the server
            if (this.world.isClient()) {

                // TODO move this to a config file
                float deviation = 0.10f;
                float speed = 0.2f;
                int particlesPerTick = 5;

                for (int i = 0; i < particlesPerTick; i++) {

                    this.world.addParticle(
                        ParticleTypes.FLAME,
                        getPos().x,
                        getPos().y,
                        getPos().z,
                        RandomUtilities.center_random() * deviation,
                        -speed,
                        RandomUtilities.center_random() * deviation
                    );

                    // Smoke particles live for less time that flame particles
                    this.world.addParticle(
                        ParticleTypes.SMOKE,
                        getPos().x,
                        getPos().y,
                        getPos().z,
                        RandomUtilities.center_random() * deviation,
                        -speed * 2,
                        RandomUtilities.center_random() * deviation
                    );
                }
            }
        }
    }
}