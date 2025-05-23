package org.ladysnake.pickyourpoison.common.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleUtil;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.pickyourpoison.common.PickYourPoison;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Util;
import net.minecraft.world.World;

import java.util.List;
import java.util.Set;

public class PoisonDartEntity extends PersistentProjectileEntity {
    private static final TrackedData<Integer> COLOR = DataTracker.registerData(PoisonDartEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<ItemStack> ITEM = DataTracker.registerData(PoisonDartEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private final Set<StatusEffectInstance> effects = Sets.newHashSet();
    private boolean colorSet;

    public PoisonDartEntity(EntityType<? extends PoisonDartEntity> entityType, World world) {
        super(entityType, world);
    }

    public PoisonDartEntity(World world, double x, double y, double z, ItemStack itemStack) {
        super(PickYourPoison.POISON_DART, x, y, z, world, itemStack, null);
    }

    public PoisonDartEntity(World world, LivingEntity owner, ItemStack itemStack) {
        super(PickYourPoison.POISON_DART, owner, world, itemStack, null);
    }

    protected ItemStack getItem() {
        return this.getDataTracker().get(ITEM);
    }

    public void setItem(ItemStack item) {
        this.getDataTracker().set(ITEM, Util.make(item.copy(), stack -> stack.setCount(1)));
    }

    @Override
    public double getDamage() {
        return 0.1f;
    }

    public void addEffect(StatusEffectInstance effect) {
        this.effects.add(effect);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(COLOR, -1);
        builder.add(ITEM, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) {
            if (this.inGround) {
                if (this.inGroundTime % 5 == 0) {
                    this.spawnParticles(1);
                }
            } else {
                this.spawnParticles(2);
            }
        } else if (this.inGround && this.inGroundTime != 0 && !this.effects.isEmpty() && this.inGroundTime >= 600) {
            this.getWorld().sendEntityStatus(this, (byte) 0);
            this.effects.clear();
            this.dataTracker.set(COLOR, -1);
        }
    }

    @Override
    protected boolean tryPickup(PlayerEntity player) {
        switch (this.pickupType) {
            case ALLOWED: {
                if (player.isCreative()) {
                    return true;
                } else {
                    return player.getInventory().insertStack(this.asItemStack());
                }
            }
            case CREATIVE_ONLY: {
                return player.getAbilities().creativeMode;
            }
        }
        return false;
    }

    private void spawnParticles(int amount) {
        int i = this.getColor();
        if (i == -1 || amount <= 0) {
            return;
        }
        double d = (double) (i >> 16 & 0xFF) / 255.0;
        double e = (double) (i >> 8 & 0xFF) / 255.0;
        double f = (double) (i >> 0 & 0xFF) / 255.0;
        for (int j = 0; j < amount; ++j) {
            // For some reason ENTITY_EFFECT is one of the few particles that is not a SimpleParticleType
            // So for now, just using the EFFECT particle
            this.getWorld().addParticle(ParticleTypes.EFFECT, this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), d, e, f);
        }
    }

    public int getColor() {
        return this.dataTracker.get(COLOR);
    }

    public void setColor(int color) {
        this.colorSet = true;
        this.dataTracker.set(COLOR, color);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        if (this.colorSet) {
            nbt.putInt("Color", this.getColor());
        }
        if (!this.effects.isEmpty()) {
            NbtList nbtList = new NbtList();
            for (StatusEffectInstance statusEffectInstance : this.effects) {
                nbtList.add(statusEffectInstance.writeNbt());
            }
            nbt.put("CustomPotionEffects", nbtList);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        for (StatusEffectInstance statusEffectInstance : getCustomPotionEffects(nbt)) {
            this.addEffect(statusEffectInstance);
        }
        if (nbt.contains("Color", 99)) {
            this.setColor(nbt.getInt("Color"));
        }

        this.setItem(ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("Item")).get());
    }

    /// Derived from the legacy 1.20.1 minecraft source from the PotionUtil class
    public static List<StatusEffectInstance> getCustomPotionEffects(@Nullable NbtCompound nbt) {
        List<StatusEffectInstance> list = Lists.newArrayList();
        if (nbt != null && nbt.contains("CustomPotionEffects", 9)) {
            NbtList nbtList = nbt.getList("CustomPotionEffects", 10);

            for(int i = 0; i < nbtList.size(); ++i) {
                NbtCompound nbtCompound = nbtList.getCompound(i);
                StatusEffectInstance statusEffectInstance = StatusEffectInstance.fromNbt(nbtCompound);
                if (statusEffectInstance != null) {
                    list.add(statusEffectInstance);
                }
            }
        }
        return list;
    }

    @Override
    protected void onHit(LivingEntity target) {
        super.onHit(target);
        Entity entity = this.getEffectCause();

        if (!this.effects.isEmpty()) {
            for (StatusEffectInstance statusEffectInstance : this.effects) {
                target.addStatusEffect(statusEffectInstance, entity);
            }
        }
    }

    @Override
    protected ItemStack asItemStack() {
        return this.getItem();
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return null;
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 0) {
            int i = this.getColor();
            if (i != -1) {
                double d = (double) (i >> 16 & 0xFF) / 255.0;
                double e = (double) (i >> 8 & 0xFF) / 255.0;
                double f = (double) (i >> 0 & 0xFF) / 255.0;
                for (int j = 0; j < 20; ++j) {
                    // For some reason ENTITY_EFFECT is one of the few particles that is not a SimpleParticleType
                    // So for now, just using the EFFECT particle
                    this.getWorld().addParticle(ParticleTypes.EFFECT, this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), d, e, f);
                }
            }
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    protected SoundEvent getHitSound() {
        return PickYourPoison.ENTITY_POISON_DART_HIT;
    }
}

