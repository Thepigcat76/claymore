package com.thepigcat.claymore;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClaymoreCapabilities {
    public static final Capability<TempHeal> TEMP_HEAL = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static class TimerProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final TempHeal TempHeal;
        private final LazyOptional<TempHeal> optional;

        public TimerProvider(int maxTime) {
            this.TempHeal = new TempHeal(maxTime);
            this.optional = LazyOptional.of(() -> TempHeal);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ClaymoreCapabilities.TEMP_HEAL.orEmpty(cap, this.optional);
        }

        @Override
        public CompoundTag serializeNBT() {
            return this.TempHeal.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.TempHeal.deserializeNBT(nbt);
        }
    }

    public static class TempHeal implements ITempHeal, INBTSerializable<CompoundTag> {
        private int time;
        private int maxTime;
        private float healAmount;
        private boolean timerStarted;

        public TempHeal(int maxTime) {
            this.time = 0;
            this.timerStarted = false;
            this.maxTime = maxTime;
        }

        @Override
        public float getPostHealAmount() {
            return healAmount;
        }

        @Override
        public void setPostHealAmount(float healAmount) {
            this.healAmount = healAmount;
        }

        @Override
        public int getTime() {
            return time;
        }

        @Override
        public int getMaxTime() {
            return maxTime;
        }

        @Override
        public void setTime(int time) {
            this.time = time;
        }

        @Override
        public void setMaxTime(int maxTime) {
            this.maxTime = maxTime;
        }

        @Override
        public boolean hasTimerStarted() {
            return timerStarted;
        }

        @Override
        public void startTimer() {
            this.timerStarted = true;
        }

        @Override
        public void stopTimer() {
            this.timerStarted = false;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("time", time);
            tag.putInt("maxTime", maxTime);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            this.time = nbt.getInt("time");
            this.maxTime = nbt.getInt("maxTime");
        }
    }

    @AutoRegisterCapability
    public interface ITempHeal {
        int getTime();
        int getMaxTime();
        float getPostHealAmount();
        void setPostHealAmount(float value);
        void setTime(int time);
        boolean hasTimerStarted();
        void setMaxTime(int maxTime);
        void startTimer();
        void stopTimer();
    }
}
