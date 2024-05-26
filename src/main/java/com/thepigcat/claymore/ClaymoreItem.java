package com.thepigcat.claymore;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ClaymoreItem extends SwordItem {
    public static final int MAX_MONUMENTUM = 30;

    private static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 0;
        }

        @Override
        public float getSpeed() {
            return 0;
        }

        @Override
        public float getAttackDamageBonus() {
            return 0;
        }

        @Override
        public int getLevel() {
            return 4;
        }

        @Override
        public int getEnchantmentValue() {
            return 0;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    public ClaymoreItem(Properties p_43272_) {
        super(TIER, 0, 0, p_43272_);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                        BASE_ATTACK_DAMAGE_UUID,
                        "Weapon modifier",
                        stack.getOrCreateTag().getInt("AttackDamage") - 1,
                        AttributeModifier.Operation.ADDITION
                )
        );
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", stack.getOrCreateTag().getInt("AttackSpeed") - 1, AttributeModifier.Operation.ADDITION));
        return slot == EquipmentSlot.MAINHAND ? builder.build() : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack itemStack, LivingEntity entity, @NotNull LivingEntity player) {
        if (entity.getHealth() <= 0) {
            double newDamage = entity.getMaxHealth() * 0.1;
            if (newDamage > ClaymoreUtils.getAttackDamage(itemStack)) {
                ClaymoreUtils.setAttackDamage(itemStack, (int) (newDamage));
            }
        }
        float bloodAmount = 0.2F + (entity.getMaxHealth() / 200F);
        ClaymoreUtils.fillBlood(itemStack, bloodAmount);
        return super.hurtEnemy(itemStack, entity, player);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        switch (ClaymoreUtils.getState(itemStack)) {
            case KILL_AURA -> attackAOE(player, itemStack, 7, ClaymoreUtils.getAttackDamage(itemStack), false);
            case TEMP_HEAL -> tempHeal(player, itemStack);
            case BOUNCE -> bounce(player, itemStack);
            case NONE -> {
                return InteractionResultHolder.fail(itemStack);
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    private static void bounce(Player player, ItemStack itemStack) {
        if (ClaymoreUtils.getBlood(itemStack) >= 0.4F) {
            bounceUp(player);
            ClaymoreUtils.drainBlood(itemStack, 0.4F);
        }
    }

    private static void bounceUp(Player player) {
        Vec3 vec3 = player.getDeltaMovement();
        if (vec3.y < -0.5D) {
            double d0 = 1.0D;
            player.setDeltaMovement(vec3.x, -vec3.y * d0, vec3.z);
        } else if (vec3.y < 0D) {
            double d0 = 2.0D;
            player.setDeltaMovement(vec3.x, -vec3.y * d0, vec3.z);
        }

    }

    private static void tempHeal(Player player, ItemStack itemStack) {
        if (!player.level().isClientSide()) {
            if (player.getHealth() < player.getMaxHealth() && ClaymoreUtils.getBlood(itemStack) >= 1F) {
                player.heal(player.getMaxHealth() * 0.5F);
                LazyOptional<ClaymoreCapabilities.TempHeal> optional = player.getCapability(ClaymoreCapabilities.TEMP_HEAL);
                optional.ifPresent(tempHeal -> {
                    tempHeal.startTimer();
                    tempHeal.setPostHealAmount(tempHeal.getPostHealAmount() + player.getMaxHealth() * 0.75F);
                });
                ClaymoreUtils.drainBlood(itemStack, 1f);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack p_41404_, Level p_41405_, Entity entity, int p_41407_, boolean p_41408_) {
        if (entity instanceof Player player && p_41404_.getItem() instanceof ClaymoreItem) {
            if (player.level().getGameTime() % 20 == 0) {
                ClaymoreUtils.fillBlood(p_41404_, 0.2F);
            }
        }
    }

    private static void attackAOE(Player player, ItemStack itemStack, float range, float damage0, boolean hurtLiving) {
        if (ClaymoreUtils.getBlood(itemStack) >= 0.8F) {
            AABB aabb = player.getBoundingBox().deflate(range);
            List<Entity> toAttack = player.level().getEntities(player, aabb);
            DamageSource src = player.damageSources().magic();
            if (!toAttack.isEmpty()) {
                ClaymoreUtils.setMonumentum(itemStack, ClaymoreUtils.getMonumentum(itemStack) + 1);
                if (ClaymoreUtils.getMonumentum(itemStack) % 10 == 0 && !player.isCreative()) {
                    player.hurt(src, player.getMaxHealth() / 10);
                }
            } else {
                ClaymoreUtils.setMonumentum(itemStack, 1);
            }
            if (ClaymoreUtils.getMonumentum(itemStack) > MAX_MONUMENTUM) {
                ClaymoreUtils.setMonumentum(itemStack, MAX_MONUMENTUM / 2);
            }
            float damage = damage0 * (ClaymoreUtils.getMonumentum(itemStack) / 5F);
            ClaymoreUtils.drainBlood(itemStack, 0.8F);
            for (Entity entity : toAttack) {
                if (hurtLiving) {
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.hurt(src, damage);
                    }
                } else if (entity instanceof Mob) {
                    if (entity instanceof EnderDragon dragon) {
                        dragon.hurt(dragon.head, src, damage);
                    } else if (entity instanceof WitherBoss wither) {
                        wither.setInvulnerableTicks(0);
                        wither.hurt(src, damage);
                    } else {
                        entity.hurt(src, damage);
                    }
                }
                ClaymoreUtils.fillBlood(itemStack, 0.1F);
                renderParticles(player, entity);
            }
        }
    }

    private static void renderParticles(Player player, Entity entity) {
        if (entity instanceof LivingEntity entityToAttack) {
            double d5 = 1.4;
            double d0 = entityToAttack.getX() - player.getX();
            double d1 = entityToAttack.getY(0.5D) - player.getEyeY();
            double d2 = entityToAttack.getZ() - player.getZ();
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
            d0 /= d3;
            d1 /= d3;
            d2 /= d3;
            double d4 = player.getRandom().nextDouble();

            while (d4 < d3) {
                d4 += 1.8D - d5 + player.getRandom().nextDouble() * (1.7D - d5);
                player.level().addParticle(ParticleTypes.ANGRY_VILLAGER, player.getX() + d0 * d4, player.getEyeY() + d1 * d4, player.getZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack itemStack) {
        return true;
    }

    @Override
    public int getBarColor(@NotNull ItemStack itemStack) {
        return 0x921400;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack itemStack) {
        return bloodForDurabilityBar(itemStack);
    }

    public static int bloodForDurabilityBar(ItemStack itemStack) {
        return Math.round(13.0F - ((1 - ClaymoreUtils.getBlood(itemStack) / ClaymoreUtils.getMaxBlood(itemStack)) * 13.0F));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack p_41421_, @Nullable Level p_41422_, List<Component> components, @NotNull TooltipFlag p_41424_) {
        components.add(Component.translatable("desc.claymore.claymore").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(p_41421_, p_41422_, components, p_41424_);
    }

    public enum States {
        KILL_AURA,
        TEMP_HEAL,
        BOUNCE,
        NONE;

        public static States fromIndex(int index) {
            return switch (index) {
                case 0 -> KILL_AURA;
                case 1 -> TEMP_HEAL;
                case 2 -> BOUNCE;
                default -> NONE;
            };
        }

        public int toIndex() {
            return this.ordinal();
        }
    }
}
