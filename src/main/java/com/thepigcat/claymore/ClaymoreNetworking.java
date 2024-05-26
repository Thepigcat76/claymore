package com.thepigcat.claymore;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ClaymoreNetworking {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(Claymore.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(C2SToggleState.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(C2SToggleState::new)
                .encoder(C2SToggleState::toBytes)
                .consumerMainThread(C2SToggleState::handle)
                .add();

        net.messageBuilder(S2CTempHealSync.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(S2CTempHealSync::new)
                .encoder(S2CTempHealSync::toBytes)
                .consumerMainThread(S2CTempHealSync::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToClients(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    public static class C2SToggleState {
        public C2SToggleState() {
        }

        public C2SToggleState(FriendlyByteBuf buf) {

        }

        public void toBytes(FriendlyByteBuf buf) {

        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            ItemStack itemStack = supplier.get().getSender().getMainHandItem();
            ClaymoreUtils.incrState(itemStack);
            return true;
        }
    }

    public static class S2CTempHealSync {
        private final int time;
        private final int maxTime;

        public S2CTempHealSync(int time, int maxTime) {
            this.time = time;
            this.maxTime = maxTime;
        }

        public S2CTempHealSync(FriendlyByteBuf buf) {
            this(buf.readInt(), buf.readInt());
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeInt(time);
            buf.writeInt(maxTime);
        }

        public boolean handle(Supplier<NetworkEvent.Context> supplier) {
            LocalPlayer player = Minecraft.getInstance().player;
            ClaymoreCapabilities.ITempHeal clientCap = player.getCapability(ClaymoreCapabilities.TEMP_HEAL).orElseThrow(NullPointerException::new);
            clientCap.setTime(time);
            clientCap.setMaxTime(maxTime);
            return true;
        }
    }
}
