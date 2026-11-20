package com.mythiccontracts.item;

import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.capability.ModCapabilities;
import com.mythiccontracts.capability.PlayerCoreData;
import com.mythiccontracts.network.ClientPacketHandler;
import com.mythiccontracts.util.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SubjugationSealItem extends Item {
    public static final String TAG_SUBJUGATED_OWNER = "SubjugatedOwner";

    public SubjugationSealItem(Properties properties) {
        super(properties);
    }

    public static boolean isSubjugated(Entity entity) {
        return entity != null && entity.getPersistentData().hasUUID(TAG_SUBJUGATED_OWNER);
    }

    @Nullable
    public static UUID getOwnerUUID(Entity entity) {
        if (!isSubjugated(entity)) {
            return null;
        }
        return entity.getPersistentData().getUUID(TAG_SUBJUGATED_OWNER);
    }

    public static boolean isAlliedWithOwner(Entity target, UUID ownerUUID) {
        if (target == null || ownerUUID == null) {
            return false;
        }
        if (ownerUUID.equals(target.getUUID())) {
            return true;
        }
        UUID targetOwner = getOwnerUUID(target);
        return ownerUUID.equals(targetOwner);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!target.getType().is(ModTags.SUBJUGATABLE)) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal("§cEsta entidade não pode ser subjugada pelo Selo."), true);
            }
            return InteractionResult.FAIL;
        }

        if (target.getPersistentData().hasUUID(TAG_SUBJUGATED_OWNER)) {
            if (!player.level().isClientSide()) {
                player.displayClientMessage(Component.literal("§cEsta entidade já foi subjugada e possui um mestre."), true);
            }
            return InteractionResult.FAIL;
        }

        if (!target.isAlive() || target.getHealth() <= 0.0F) {
            return InteractionResult.FAIL;
        }

        PlayerCoreData data = ModCapabilities.get(player).orElse(null);
        if (data == null) {
            return InteractionResult.FAIL;
        }

        float threshold = (data.getFaction() == FactionType.QUEBRADORES_DE_TRONOS) ? 0.25F : 0.15F;
        float maxHealth = Math.max(1.0F, target.getMaxHealth());
        float healthRatio = target.getHealth() / maxHealth;

        if (healthRatio > threshold) {
            if (!player.level().isClientSide()) {
                String currentPct = String.format(Locale.ROOT, "%.1f%%", healthRatio * 100.0F);
                String requiredPct = (data.getFaction() == FactionType.QUEBRADORES_DE_TRONOS) ? "25%" : "15%";
                Component msg = Component.literal(
                        "§cA entidade resiste ao Selo! Vida atual: §e" + currentPct + " §c(Necessário: <= §6" + requiredPct + "§c)"
                );
                player.displayClientMessage(msg, true);
                player.sendSystemMessage(msg);
            }
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel serverLevel) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            if (entityId == null) {
                return InteractionResult.FAIL;
            }

            data.addSubjugatedEntity(entityId);
            String targetName = target.getType().getDescription().getString();

            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    35,
                    0.5D,
                    0.5D,
                    0.5D,
                    0.05D
            );
            serverLevel.sendParticles(
                    ParticleTypes.ENCHANT,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    40,
                    0.6D,
                    0.6D,
                    0.6D,
                    0.15D
            );
            serverLevel.playSound(
                    null,
                    serverPlayer.blockPosition(),
                    SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.PLAYERS,
                    1.0F,
                    0.85F
            );

            target.discard();

            if (!serverPlayer.getAbilities().instabuild) {
                stack.shrink(1);
            }

            ModCapabilities.sync(serverPlayer);

            serverPlayer.sendSystemMessage(
                    Component.literal("§6[Selo de Subjugação] §e" + targetName + " §afoi subjugado e selado! Use §fShift + Clique Direito §apara invocá-lo.")
            );
        }

        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            PlayerCoreData data = ModCapabilities.get(serverPlayer).orElse(null);
            if (data == null || data.peekLastSubjugatedEntity() == null) {
                serverPlayer.displayClientMessage(
                        Component.literal("§cVocê não possui nenhuma entidade selada armazenada."),
                        true
                );
                return InteractionResultHolder.fail(stack);
            }

            ResourceLocation entityId = data.popLastSubjugatedEntity();
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
            if (type == null) {
                ModCapabilities.sync(serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal("§cFalha ao localizar o tipo de entidade selada: " + entityId));
                return InteractionResultHolder.fail(stack);
            }

            Entity created = type.create(serverLevel);
            if (created == null) {
                data.addSubjugatedEntity(entityId);
                ModCapabilities.sync(serverPlayer);
                return InteractionResultHolder.fail(stack);
            }

            Vec3 look = serverPlayer.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
            if (horizontal.lengthSqr() < 1.0E-4D) {
                horizontal = new Vec3(0.0D, 0.0D, 1.0D);
            } else {
                horizontal = horizontal.normalize();
            }

            double spawnX = serverPlayer.getX() + horizontal.x * 2.5D;
            double spawnY = serverPlayer.getY();
            double spawnZ = serverPlayer.getZ() + horizontal.z * 2.5D;

            created.moveTo(spawnX, spawnY, spawnZ, serverPlayer.getYRot(), 0.0F);
            created.getPersistentData().putUUID(TAG_SUBJUGATED_OWNER, serverPlayer.getUUID());
            created.setCustomName(Component.literal(
                    "§6[Subjugado de " + serverPlayer.getName().getString() + "] " + created.getType().getDescription().getString()
            ));
            created.setCustomNameVisible(true);

            if (created instanceof Mob mob) {
                mob.setPersistenceRequired();
                mob.setTarget(null);
            }

            serverLevel.addFreshEntity(created);

            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    spawnX,
                    spawnY + 1.0D,
                    spawnZ,
                    40,
                    0.6D,
                    0.8D,
                    0.6D,
                    0.06D
            );
            serverLevel.playSound(
                    null,
                    serverPlayer.blockPosition(),
                    SoundEvents.END_PORTAL_FRAME_FILL,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.2F
            );

            ModCapabilities.sync(serverPlayer);

            serverPlayer.sendSystemMessage(
                    Component.literal("§6[Selo de Subjugação] §aVocê invocou seu servo subjugado: §e" + created.getType().getDescription().getString() + "§a!")
            );
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        tooltipComponents.add(Component.literal("Clique Direito em um Boss enfraquecido para selá-lo.")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("• Limite Padrão: <= 15% de Vida")
                .withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.literal("• Quebradores de Tronos: <= 25% de Vida")
                .withStyle(ChatFormatting.RED));
        tooltipComponents.add(Component.literal("Shift + Clique Direito para invocar a última entidade selada.")
                .withStyle(ChatFormatting.AQUA));

        if (level != null && level.isClientSide()) {
            PlayerCoreData clientData = DistExecutor.unsafeCallWhenOn(
                    Dist.CLIENT,
                    () -> ClientPacketHandler::getClientPlayerCoreData
            );
            if (clientData != null) {
                boolean isThroneBreaker = clientData.getFaction() == FactionType.QUEBRADORES_DE_TRONOS;
                String activeThreshold = isThroneBreaker ? "25% (Quebradores de Tronos)" : "15% (Padrão)";
                tooltipComponents.add(Component.literal("• Seu Limite Ativo: <= " + activeThreshold)
                        .withStyle(isThroneBreaker ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.YELLOW));

                List<ResourceLocation> sealed = clientData.getSubjugatedEntities();
                ResourceLocation lastEntityId = clientData.peekLastSubjugatedEntity();
                if (sealed.isEmpty() || lastEntityId == null) {
                    tooltipComponents.add(Component.literal("• Entidades Seladas: 0 (Nenhuma)")
                            .withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(lastEntityId);
                    String entityName = type != null ? type.getDescription().getString() : lastEntityId.toString();
                    tooltipComponents.add(Component.literal("• Entidades Seladas: " + sealed.size()
                                    + " (Próxima: " + entityName + ")")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }
}
