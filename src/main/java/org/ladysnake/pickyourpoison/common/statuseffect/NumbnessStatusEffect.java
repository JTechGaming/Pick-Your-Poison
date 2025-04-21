package org.ladysnake.pickyourpoison.common.statuseffect;

import org.ladysnake.pickyourpoison.cca.PickYourPoisonEntityComponents;
import org.ladysnake.pickyourpoison.common.damage.PoisonDamageSources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class NumbnessStatusEffect extends StatusEffect {
    public NumbnessStatusEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    LivingEntity entity;
    int amplifier;

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        this.entity = entity;
        this.amplifier = amplifier;
        super.onApplied(entity, amplifier);
    }

    @Override
    public void onRemoved(AttributeContainer attributeContainer) {
        super.onRemoved(attributeContainer);

        if (entity == null) {
            return;
        }

        entity.damage(entity.getDamageSources().pypSources().backlash(), PickYourPoisonEntityComponents.NUMBNESS_DAMAGE.get(entity).getDamageAccumulated());

        PickYourPoisonEntityComponents.NUMBNESS_DAMAGE.get(entity).setDamageAccumulated(0f);
        PickYourPoisonEntityComponents.NUMBNESS_DAMAGE.get(entity).setFromLicking(false);
    }
}
