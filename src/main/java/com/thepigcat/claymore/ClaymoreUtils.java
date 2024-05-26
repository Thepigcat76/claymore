package com.thepigcat.claymore;

import net.minecraft.world.item.ItemStack;

public class ClaymoreUtils {

    public static int getAttackDamage(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("AttackDamage");
    }

    public static void setAttackDamage(ItemStack itemStack, int value) {
        itemStack.getOrCreateTag().putInt("AttackDamage", value);
    }

    public static int getAttackSpeed(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("AttackSpeed");
    }

    public static void setAttackSpeed(ItemStack itemStack, int value) {
        itemStack.getOrCreateTag().putInt("AttackSpeed", value);
    }

    public static int getMonumentum(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("Monumentum");
    }

    public static void setMonumentum(ItemStack itemStack, int value) {
        itemStack.getOrCreateTag().putInt("Monumentum", value);
    }

    public static float getBlood(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getFloat("Blood");
    }

    public static void setBlood(ItemStack itemStack, float value) {
        itemStack.getOrCreateTag().putFloat("Blood", value);
    }

    public static float getMaxBlood(ItemStack itemStack) {
        return getAttackDamage(itemStack);
    }

    public static void fillBlood(ItemStack itemStack, float amount) {
        setBlood(itemStack, Math.min(getBlood(itemStack) + amount, getMaxBlood(itemStack)));
    }

    public static void drainBlood(ItemStack itemStack, float amount) {
        setBlood(itemStack, Math.min(getBlood(itemStack) - amount, getMaxBlood(itemStack)));
    }

    public static void init(ItemStack itemStack) {
        setBlood(itemStack, 0);
        setAttackDamage(itemStack, 2);
        setAttackSpeed(itemStack, -2);
        setMonumentum(itemStack, 1);
    }

    public static void incrState(ItemStack itemStack) {
        int index = getState(itemStack).toIndex();
        int newIndex = index + 1;
        if (newIndex >= ClaymoreItem.States.values().length) {
            setState(itemStack, ClaymoreItem.States.fromIndex(0));
        } else {
            setState(itemStack, ClaymoreItem.States.fromIndex(newIndex));
        }
        setMonumentum(itemStack, 0);
    }

    public static ClaymoreItem.States getState(ItemStack itemStack) {
        return ClaymoreItem.States.fromIndex(itemStack.getOrCreateTag().getInt("ClaymoreState"));
    }

    public static void setState(ItemStack itemStack, ClaymoreItem.States state) {
        itemStack.getOrCreateTag().putInt("ClaymoreState", state.toIndex());
    }
}
