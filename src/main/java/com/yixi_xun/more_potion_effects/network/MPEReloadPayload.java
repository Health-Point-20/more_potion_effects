package com.yixi_xun.more_potion_effects.network;

import com.yixi_xun.more_potion_effects.MorePotionEffectsMod;
import com.yixi_xun.more_potion_effects.api.PotionBrewingSystem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端 -> 客户端的通知包：告知客户端重新读取本地 config 中的酿造配方。
 * 配方内容由客户端从本地文件加载，因此包体本身不携带数据。
 */
@EventBusSubscriber(modid = MorePotionEffectsMod.MOD_ID)
public record MPEReloadPayload() implements CustomPacketPayload {
    public static final Type<MPEReloadPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MorePotionEffectsMod.MOD_ID, "reload_brewing"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MPEReloadPayload> STREAM_CODEC =
            StreamCodec.unit(new MPEReloadPayload());

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(TYPE, STREAM_CODEC, (payload, context) -> {
            // 处理器已在主线程执行（PayloadRegistrar 默认行为）
            PotionBrewingSystem.reloadRecipes();
            PotionBrewingSystem.invalidateCreativeTabCache();
            com.yixi_xun.more_potion_effects.client.CreativeTabRefresher.refreshNow();
        });
    }
}
