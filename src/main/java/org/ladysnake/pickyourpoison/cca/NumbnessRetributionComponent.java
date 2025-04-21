package org.ladysnake.pickyourpoison.cca;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

public class NumbnessRetributionComponent implements AutoSyncedComponent {
    private float damageAccumulated;
    private boolean fromLicking;

    public float getDamageAccumulated() {
        return damageAccumulated;
    }

    public void setDamageAccumulated(float damageAccumulated) {
        this.damageAccumulated = damageAccumulated;
    }

    public boolean isFromLicking() {
        return fromLicking;
    }

    public void setFromLicking(boolean fromLicking) {
        this.fromLicking = fromLicking;
    }

    @Override
    public void readFromNbt(NbtCompound compoundTag, RegistryWrapper.WrapperLookup registryLookup) {
        this.damageAccumulated = compoundTag.getFloat("DamageAccumulated");
        this.fromLicking = compoundTag.getBoolean("FromLicking");
    }

    @Override
    public void writeToNbt(NbtCompound compoundTag, RegistryWrapper.WrapperLookup registryLookup) {
        compoundTag.putFloat("DamageAccumulated", this.damageAccumulated);
        compoundTag.putBoolean("FromLicking", this.fromLicking);
    }
}
