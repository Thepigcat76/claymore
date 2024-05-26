package com.thepigcat.claymore;


import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Claymore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClaymoreOverlay {
    public static final IGuiOverlay HUD_CLAYMORE = ((gui, guiGraphics, partialTick, width, height) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack itemStack = player.getMainHandItem();
        if (!(itemStack.getItem() instanceof ClaymoreItem))
            return;

        int x = width - width / 6;

        ClaymoreCapabilities.TempHeal tempHeal = player.getCapability(ClaymoreCapabilities.TEMP_HEAL).orElseThrow(NullPointerException::new);
        List<Pair<String, ?>> displays = List.of(
                Pair.of("Attack Damage", (double) ClaymoreUtils.getAttackDamage(itemStack)),
                Pair.of("Blood", ClaymoreUtils.getBlood(itemStack)),
                Pair.of("Claymore State", ClaymoreUtils.getState(itemStack)),
                Pair.of("Monumentum Attack", ClaymoreUtils.getAttackDamage(itemStack) * (ClaymoreUtils.getMonumentum(itemStack) / 5D)),
                Pair.of("Monumentum", ClaymoreUtils.getMonumentum(itemStack) / 5D),
                Pair.of("Temporary Heal Timer", tempHeal.getTime()+"/"+tempHeal.getMaxTime()+"s")
        );

        Font font = Minecraft.getInstance().font;

        int y = height - height / 4;

        for (Pair<String, ?> display : displays) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.literal(display.getFirst() + ": ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(display.getSecond().toString()).withStyle(ChatFormatting.RED)),
                    x, y, 256);
            y += font.lineHeight;
        }
    });

    @SubscribeEvent
    public static void registerGuiOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("claymore_info", HUD_CLAYMORE);
    }
}