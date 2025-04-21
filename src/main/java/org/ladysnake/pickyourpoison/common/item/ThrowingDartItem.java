package org.ladysnake.pickyourpoison.common.item;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.*;
import org.ladysnake.pickyourpoison.common.PickYourPoison;
import org.ladysnake.pickyourpoison.common.entity.PoisonDartEntity;
import org.ladysnake.pickyourpoison.common.entity.PoisonDartFrogEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ThrowingDartItem extends Item {
    StatusEffectInstance statusEffectInstance;

    public static final DecimalFormat MODIFIER_FORMAT = (DecimalFormat) Util.make(new DecimalFormat("#.##"), (decimalFormat) -> decimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT)));

    public ThrowingDartItem(Settings settings, StatusEffectInstance statusEffectInstance) {
        super(settings);
        this.statusEffectInstance = statusEffectInstance;
    }

    public static StatusEffectInstance getDartPoisonEffect(Item item) {
        if (item == PickYourPoison.COMATOSE_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.COMATOSE), 600); // 30s
        } else if (item == PickYourPoison.BATRACHOTOXIN_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.BATRACHOTOXIN), 80); // 4s
        } else if (item == PickYourPoison.NUMBNESS_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.NUMBNESS), 200); // 10s
        } else if (item == PickYourPoison.VULNERABILITY_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.VULNERABILITY), 200); // 10s
        } else if (item == PickYourPoison.TORPOR_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.TORPOR), 200); // 10s
        } else if (item == PickYourPoison.STIMULATION_POISON_DART) {
            return new StatusEffectInstance((PickYourPoison.STIMULATION), 600); // 30s
        } else if (item == PickYourPoison.BLINDNESS_POISON_DART) {
            return new StatusEffectInstance(StatusEffects.BLINDNESS, 200); // 10s
        }

        return null;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null, user.getBlockPos(), PickYourPoison.ITEM_POISON_DART_THROW, SoundCategory.NEUTRAL, 0.5f, 1.0f);

        if (!world.isClient) {
            PoisonDartEntity throwingDart = new PoisonDartEntity(world, user, itemStack);
            throwingDart.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 3.0f, 1.0f);
            throwingDart.setDamage(throwingDart.getDamage());
            throwingDart.setItem(itemStack);
            if (this.statusEffectInstance != null) {
                StatusEffectInstance potion = new StatusEffectInstance(statusEffectInstance);
                throwingDart.addEffect(potion);
                throwingDart.setColor(potion.getEffectType().value().getColor());
            }
            world.spawnEntity(throwingDart);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }

        return TypedActionResult.success(itemStack, world.isClient());
    }

    public StatusEffectInstance getStatusEffectInstance() {
        return statusEffectInstance;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        buildTooltip(tooltip, 1.0f);
    }

    public void buildTooltip(List<Text> list, float durationMultiplier) {
        ArrayList<Pair<EntityAttribute, EntityAttributeModifier>> list3 = Lists.newArrayList();
        if (this.statusEffectInstance == null) {
            list.add(Text.translatable("effect.none").formatted(Formatting.GRAY));
        } else {
            MutableText mutableText = Text.translatable(statusEffectInstance.getTranslationKey());
            StatusEffect statusEffect = statusEffectInstance.getEffectType().value();
            Map<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator> map = statusEffect.attributeModifiers;
            if (!map.isEmpty()) {
                for (Map.Entry<RegistryEntry<EntityAttribute>, StatusEffect.EffectAttributeModifierCreator> entry : map.entrySet()) {
                    StatusEffect.EffectAttributeModifierCreator entityAttributeModifierCreator = entry.getValue();
                    EntityAttributeModifier entityAttributeModifier2 = new EntityAttributeModifier(entityAttributeModifierCreator.id(), adjustModifierAmount(statusEffectInstance.getAmplifier(), entityAttributeModifierCreator.baseValue()), entityAttributeModifierCreator.operation());
                    list3.add(new Pair<>(entry.getKey().value(), entityAttributeModifier2));
                    //TODO commented out for now, add back later
                }
            }
            if (statusEffectInstance.getAmplifier() > 0) {
                mutableText = Text.translatable("potion.withAmplifier", mutableText, Text.translatable("potion.potency." + statusEffectInstance.getAmplifier()));
            }
            if (statusEffectInstance.getDuration() > 20) {
                mutableText = Text.translatable("potion.withDuration", mutableText, StatusEffectUtil.getDurationText(statusEffectInstance, durationMultiplier, 20)); //TODO actually make this set the (20) to the current tick rate
            }
            list.add(mutableText.formatted(statusEffect.getCategory().getFormatting()));
        }
        if (!list3.isEmpty()) {
            list.add(Text.of(""));
            list.add(Text.translatable("potion.whenDrank").formatted(Formatting.DARK_PURPLE));
            for (Pair<EntityAttribute, EntityAttributeModifier> pair : list3) {
                EntityAttributeModifier entityAttributeModifier3 = (EntityAttributeModifier) pair.getSecond();
                double d = entityAttributeModifier3.value();
                double e = entityAttributeModifier3.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE || entityAttributeModifier3.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? entityAttributeModifier3.value() * 100.0 : entityAttributeModifier3.value();
                if (d > 0.0) {
                    list.add(Text.translatable("attribute.modifier.plus." + entityAttributeModifier3.operation().getId(), MODIFIER_FORMAT.format(e), Text.translatable(((EntityAttribute) pair.getFirst()).getTranslationKey())).formatted(Formatting.BLUE));
                    continue;
                }
                if (!(d < 0.0)) continue;
                list.add(Text.translatable("attribute.modifier.take." + entityAttributeModifier3.operation().getId(), MODIFIER_FORMAT.format(e *= -1.0), Text.translatable(((EntityAttribute) pair.getFirst()).getTranslationKey())).formatted(Formatting.RED));
            }
        }
    }

    public double adjustModifierAmount(int amplifier, double value) {
        return value * (double)(amplifier + 1);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof PoisonDartFrogEntity poisonDartFrog && PoisonDartFrogEntity.getFrogPoisonEffect(poisonDartFrog.getPoisonDartFrogType()) != null && statusEffectInstance == null) {
            Item item = PickYourPoison.THROWING_DART;
            switch (((PoisonDartFrogEntity) entity).getPoisonDartFrogType()) {
                case BLUE -> item = PickYourPoison.COMATOSE_POISON_DART;
                case GOLDEN -> item = PickYourPoison.BATRACHOTOXIN_POISON_DART;
                case GREEN -> item = PickYourPoison.NUMBNESS_POISON_DART;
                case ORANGE -> item = PickYourPoison.VULNERABILITY_POISON_DART;
                case CRIMSON -> item = PickYourPoison.TORPOR_POISON_DART;
                case RED -> item = PickYourPoison.STIMULATION_POISON_DART;
                case LUXINTRUS -> item = PickYourPoison.BLINDNESS_POISON_DART;
            }

            ItemStack itemStack = new ItemStack(item);

            if (!user.getAbilities().creativeMode) {
                if (user.getStackInHand(hand).getCount() > 1) {
                    user.getStackInHand(hand).decrement(1);
                    user.getInventory().insertStack(itemStack);
                } else {
                    user.setStackInHand(hand, itemStack);
                }
            } else {
                user.getInventory().insertStack(itemStack);
            }

            user.getWorld().playSound(user, user.getBlockPos(), PickYourPoison.ITEM_POISON_DART_COAT, SoundCategory.NEUTRAL, 1.0f, 1.0f);
            return ActionResult.SUCCESS;
        }

        return super.useOnEntity(stack, user, entity, hand);
    }

}

