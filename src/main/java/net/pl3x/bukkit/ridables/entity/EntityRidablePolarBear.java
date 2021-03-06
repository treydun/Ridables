package net.pl3x.bukkit.ridables.entity;

import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityLiving;
import net.minecraft.server.v1_13_R1.EntityPlayer;
import net.minecraft.server.v1_13_R1.EntityPolarBear;
import net.minecraft.server.v1_13_R1.MathHelper;
import net.minecraft.server.v1_13_R1.MobEffect;
import net.minecraft.server.v1_13_R1.MobEffects;
import net.minecraft.server.v1_13_R1.SoundEffects;
import net.minecraft.server.v1_13_R1.World;
import net.pl3x.bukkit.ridables.Ridables;
import net.pl3x.bukkit.ridables.configuration.Config;
import net.pl3x.bukkit.ridables.util.Mover;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_13_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;

public class EntityRidablePolarBear extends EntityPolarBear implements RidableEntity {
    private static Field jumping;
    private boolean isJumping = false;

    public EntityRidablePolarBear(World world) {
        super(world);
        Q = Config.POLAR_BEAR_STEP_HEIGHT; // stepHeight
        persistent = true;

        if (jumping == null) {
            try {
                jumping = EntityLiving.class.getDeclaredField("bg");
                jumping.setAccessible(true);
            } catch (NoSuchFieldException ignore) {
            }
        }
    }

    public boolean isFood(ItemStack itemstack) {
        return false;
    }

    @Override
    protected boolean isTypeNotPersistent() {
        return false; // we definitely want persistence
    }

    // travel(strafe, vertical, forward)
    @Override
    public void a(float f, float f1, float f2) {
        EntityPlayer rider = getRider();
        if (rider != null) {
            Q = Config.POLAR_BEAR_STEP_HEIGHT; // stepHeight

            // do not target anything while being ridden
            setGoalTarget(null, null, false);

            // eject rider if in water or lava
            if (isInWater() || ax()) {
                ejectPassengers();
                rider.stopRiding();
                return;
            }

            // rotation
            setYawPitch(lastYaw = yaw = rider.yaw, pitch = rider.pitch * 0.5F);
            aS = aQ = yaw;

            // controls
            float forward = rider.bj;
            float strafe = rider.bh * 0.5F;
            if (forward <= 0.0F) {
                forward *= 0.25F;
            }

            if (jumping != null && !isJumping) {
                try {
                    isJumping = jumping.getBoolean(rider);
                    if (Config.POLAR_BEAR_STAND && isJumping && onGround && !isStanding() && forward == 0 && strafe == 0) {
                        isJumping = false;
                        setStanding(true);
                        this.a(SoundEffects.ENTITY_POLAR_BEAR_WARNING, 1.0F, 1.0F);
                        Bukkit.getServer().getScheduler().runTaskLater(
                                Ridables.getPlugin(Ridables.class),
                                () -> setStanding(false), 20);
                    }
                } catch (IllegalAccessException ignore) {
                }
            }

            if (isJumping && onGround && !isStanding()) {
                motY = (double) Config.POLAR_BEAR_JUMP_POWER;
                MobEffect jump = getEffect(MobEffects.JUMP);
                if (jump != null) {
                    motY += (double) ((float) (jump.getAmplifier() + 1) * 0.1F);
                }
                impulse = true;
                if (forward > 0.0F) {
                    motX += (double) (-0.4F * MathHelper.sin(yaw * 0.017453292F) * Config.POLAR_BEAR_JUMP_POWER);
                    motZ += (double) (0.4F * MathHelper.cos(yaw * 0.017453292F) * Config.POLAR_BEAR_JUMP_POWER);
                }
            }

            // move
            Mover.moveOnLand(this, strafe, f1, forward, Config.POLAR_BEAR_SPEED);

            if (onGround) {
                isJumping = false;
            }
            return;
        }
        super.a(f, f1, f2);
    }

    private EntityPlayer getRider() {
        if (passengers != null && !passengers.isEmpty()) {
            Entity entity = passengers.get(0); // only care about first rider
            if (entity instanceof EntityPlayer) {
                return (EntityPlayer) entity;
            }
        }
        return null; // aww, lonely bear is lonely
    }

    private boolean isStanding() {
        return dA();
    }

    private void setStanding(boolean standing) {
        s(standing);
    }
}
