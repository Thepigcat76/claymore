package com.thepigcat.claymore;

import com.mojang.logging.LogUtils;
import com.mojang.patchy.BlockedServers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import javax.swing.plaf.basic.BasicComboBoxUI;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(Claymore.MODID)
public class Claymore {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "claymore";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> CLAYMORE = ITEMS.register("claymore", () -> new ClaymoreItem(new Item.Properties()));

    public Claymore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEventBus);
        ClaymoreNetworking.register();

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        ItemStack clayMoreItem = CLAYMORE.get().getDefaultInstance();
        ClaymoreUtils.init(clayMoreItem);
        if (event.getTabKey() == CreativeModeTabs.COMBAT)
            event.accept(clayMoreItem);
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        public static final KeyMapping TOGGLE_STATE = new KeyMapping("key.category.claymore.toggle_state", GLFW.GLFW_KEY_O, "key.category.claymore");

        @SubscribeEvent
        public static void registerKey(RegisterKeyMappingsEvent event) {
            event.register(TOGGLE_STATE);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class CommonModEvents {
        @SubscribeEvent
        public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                event.addCapability(new ResourceLocation(MODID, "timer"), new ClaymoreCapabilities.TimerProvider(10));
            }
        }

        @SubscribeEvent
        public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
            BlockPos blockPos = event.getHitVec().getBlockPos();
            Level level = event.getLevel();
            BlockState blockState = level.getBlockState(blockPos);
            if (blockState.getBlock() instanceof LavaCauldronBlock cauldronBlock) {
                Player entity = event.getEntity();
                if (cauldronBlock.isFull(blockState) && entity.getMainHandItem().is(Items.IRON_SWORD)) {
                    ItemStack itemStack = Claymore.CLAYMORE.get().getDefaultInstance();
                    ClaymoreUtils.init(itemStack);
                    entity.getMainHandItem().setCount(0);
                    entity.playSound(SoundEvents.LAVA_EXTINGUISH);
                    ItemHandlerHelper.giveItemToPlayer(entity, itemStack);
                    level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (ClientModEvents.TOGGLE_STATE.consumeClick()) {
                ItemStack itemStack = Minecraft.getInstance().player.getMainHandItem();
                if (itemStack.getItem() instanceof ClaymoreItem) {
                    ClaymoreNetworking.sendToServer(new ClaymoreNetworking.C2SToggleState());
                }
            }
        }

        @SubscribeEvent
        public static void playerTick(TickEvent.PlayerTickEvent event) {
            if (event.side == LogicalSide.SERVER) {
                Player player = event.player;
                LazyOptional<ClaymoreCapabilities.TempHeal> optionalHeal = player.getCapability(ClaymoreCapabilities.TEMP_HEAL);
                optionalHeal.ifPresent(tempHeal -> {
                    if (tempHeal.hasTimerStarted()) {
                        if (tempHeal.getTime() >= tempHeal.getMaxTime()) {
                            player.hurt(player.damageSources().magic(), tempHeal.getPostHealAmount());
                            tempHeal.setTime(0);
                            tempHeal.setPostHealAmount(0);
                            ClaymoreNetworking.sendToClients(new ClaymoreNetworking.S2CTempHealSync(0, tempHeal.getMaxTime()));
                            tempHeal.stopTimer();
                        } else {
                            if (player.level().getGameTime() % 20 == 0) {
                                tempHeal.setTime(tempHeal.getTime() + 1);
                                ClaymoreNetworking.sendToClients(new ClaymoreNetworking.S2CTempHealSync(tempHeal.getTime(), tempHeal.getMaxTime()));
                            }
                        }
                    }
                });
            }
        }
    }
}
