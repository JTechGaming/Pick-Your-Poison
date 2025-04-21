package org.ladysnake.pickyourpoison.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.pickyourpoison.cca.PickYourPoisonEntityComponents;
import org.ladysnake.pickyourpoison.common.PickYourPoison;
import org.ladysnake.pickyourpoison.common.item.PoisonDartFrogBowlItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    @Shadow
    public float prevHeadYaw;
    float stuckYaw = 0;
//    @ModifyVariable(method = "fall", at = @At(value = "FIELD",
//            target = "Lnet/minecraft/world/World;isClient:Z",
//            ordinal = 1
//    ), ordinal = 1)
//    public boolean noParticles(boolean onGround) {
//        return onGround && !((Object) this instanceof PoisonDartFrogEntity);
//    }

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "getPreferredEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void getPreferredEquipmentSlot(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        if (stack.getItem() instanceof PoisonDartFrogBowlItem) {
            cir.setReturnValue(EquipmentSlot.HEAD);
        }
    }

    @Shadow
    public abstract boolean hasStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    public abstract void setHeadYaw(float headYaw);

    @Shadow
    public abstract void setSprinting(boolean sprinting);

    @Shadow
    @Nullable
    public abstract StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect);

    @Shadow
    public abstract boolean damage(DamageSource source, float amount);

    @Shadow
    public abstract void heal(float amount);

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot slot);

    @Shadow
    public abstract void equipStack(EquipmentSlot slot, ItemStack stack);

    @Shadow public abstract float getHealth();

    @Shadow public abstract void setHealth(float health);

    @Inject(method = "canSee", at = @At("HEAD"), cancellable = true)
    public void canSee(Entity entity, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (PickYourPoison.isComatose((LivingEntity) (Object) this)) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    public void tick(CallbackInfo callbackInfo) {
        if (PickYourPoison.isComatose((LivingEntity) (Object) this)) {
            this.setPitch(90);
            this.prevPitch = 90;
            this.setHeadYaw(stuckYaw);
            this.prevHeadYaw = stuckYaw;
            this.setYaw(stuckYaw);
            this.setSneaking(false);
            this.setSprinting(false);
        } else {
            this.stuckYaw = this.getYaw();
        }

        if (this.hasStatusEffect((PickYourPoison.BATRACHOTOXIN)) && (this.age % (20 / (MathHelper.clamp(this.getStatusEffect((PickYourPoison.BATRACHOTOXIN)).getAmplifier() + 1, 1, 20))) == 0)) {
            this.damage(getDamageSources().pypSources().batrachotoxin(), 1);
            this.timeUntilRegen = 0;
        }


        if (this.hasStatusEffect((PickYourPoison.COMATOSE)) && this.age % (40 / (MathHelper.clamp(this.getStatusEffect((PickYourPoison.COMATOSE)).getAmplifier() + 1, 1, 40))) == 0) {
            this.heal(1);
        }
    }

    @ModifyVariable(method = "damage", at = @At("HEAD"), argsOnly = true)
    private float multiplyDamageForVulnerability(float amount) {
        if (this.hasStatusEffect((PickYourPoison.VULNERABILITY))) {
            return amount + (amount * (0.25f * (this.getStatusEffect((PickYourPoison.VULNERABILITY)).getAmplifier() + 1)));
        }
        return amount;
    }

    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    public void torporCancelHeal(float amount, CallbackInfo callbackInfo) {
        if (this.hasStatusEffect((PickYourPoison.TORPOR))) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "applyDamage", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/LivingEntity;getHealth()F"), cancellable = true)
    private void pickyourpoison$saveFromNumbness(DamageSource source, float amount, CallbackInfo ci) {
        if (!getWorld().isClient && getHealth() - amount <= 0) {
            PickYourPoisonEntityComponents.NUMBNESS_DAMAGE.maybeGet(this).ifPresent(retributionComponent -> {
                if (retributionComponent.getDamageAccumulated() > 0 && !retributionComponent.isFromLicking()) {
                    this.setHealth(1.0f);
                    ci.cancel();
                }
            });
        }
    }
}