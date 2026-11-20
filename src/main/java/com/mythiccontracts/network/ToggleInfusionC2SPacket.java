package com.mythiccontracts.network;

import com.mythiccontracts.capability.ModCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ToggleInfusionC2SPacket {

    public ToggleInfusionC2SPacket() {
    }

    public ToggleInfusionC2SPacket(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null || !sender.isAlive() || sender.isDeadOrDying()) {
                return;
            }
            ModCapabilities.get(sender).ifPresent(data -> {
                boolean active = !data.isManaInfusionActive();
                data.setManaInfusionActive(active);
                ModCapabilities.sync(sender);

                ServerLevel serverLevel = sender.serverLevel();
                if (active) {
                    sender.displayClientMessage(
                            Component.literal("✦ Infusão de Mana: ATIVADA (Corpo Nv." + data.getBodyRealm()
                                            + " / Espírito Nv." + data.getSpiritRealm() + ")")
                                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                            true
                    );
                    serverLevel.sendParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            sender.getX(),
                            sender.getY() + 1.0D,
                            sender.getZ(),
                            20,
                            0.4D,
                            0.6D,
                            0.4D,
                            0.03D
                    );
                    serverLevel.playSound(
                            null,
                            sender.getX(),
                            sender.getY(),
                            sender.getZ(),
                            SoundEvents.BEACON_ACTIVATE,
                            SoundSource.PLAYERS,
                            0.8F,
                            1.25F
                    );
                } else {
                    sender.displayClientMessage(
                            Component.literal("✦ Infusão de Mana: DESATIVADA")
                                    .withStyle(ChatFormatting.GRAY),
                            true
                    );
                    serverLevel.sendParticles(
                            ParticleTypes.SMOKE,
                            sender.getX(),
                            sender.getY() + 1.0D,
                            sender.getZ(),
                            12,
                            0.3D,
                            0.5D,
                            0.3D,
                            0.02D
                    );
                    serverLevel.playSound(
                            null,
                            sender.getX(),
                            sender.getY(),
                            sender.getZ(),
                            SoundEvents.BEACON_DEACTIVATE,
                            SoundSource.PLAYERS,
                            0.6F,
                            1.1F
                    );
                }
            });
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
