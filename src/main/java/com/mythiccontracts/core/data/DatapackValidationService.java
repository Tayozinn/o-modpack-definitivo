package com.mythiccontracts.core.data;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import com.mythiccontracts.contract.ContractDefinition;
import com.mythiccontracts.contract.ContractManager;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.faction.FactionDefinition;
import com.mythiccontracts.faction.FactionRegistry;
import com.mythiccontracts.skill.definition.DivineSkillDefinition;
import com.mythiccontracts.skill.definition.DivineSkillRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Serviço de validação cruzada entre todos os registros data-driven do mod:
 * Reinos, Facções, Contratos v2 e Resquícios Divinos.
 */
public final class DatapackValidationService {

    public record ValidationReport(
            int realmCount,
            int factionCount,
            int contractCount,
            int skillCount,
            List<String> errors,
            List<String> warnings
    ) {
        public ValidationReport {
            errors = errors != null ? List.copyOf(errors) : List.of();
            warnings = warnings != null ? List.copyOf(warnings) : List.of();
        }

        public boolean hasErrors() {
            return !this.errors.isEmpty();
        }

        public boolean isClean() {
            return this.errors.isEmpty() && this.warnings.isEmpty();
        }
    }

    private DatapackValidationService() {
    }

    public static ValidationReport validateAll() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Collection<RealmDefinition> realms = RealmRegistry.getAllRealms();
        Collection<FactionDefinition> factions = FactionRegistry.getAllFactions();
        Collection<ResourceLocation> contractIds = ContractManager.getRegisteredContractIds();
        Collection<DivineSkillDefinition> skills = DivineSkillRegistry.getAllSkills();

        int maxBodyOrder = Math.max(1, RealmRegistry.getMaxOrder(CultivationPath.BODY));
        int maxSpiritOrder = Math.max(1, RealmRegistry.getMaxOrder(CultivationPath.SPIRIT));

        // 1. Reinos: ordens duplicadas por caminho & previous_realm quebrado
        Set<String> seenPathOrder = new HashSet<>();
        for (RealmDefinition realm : realms) {
            String key = realm.path().name() + ":" + realm.order();
            if (!seenPathOrder.add(key)) {
                errors.add("Reino '" + realm.id() + "' possui ordem duplicada (" + key + ").");
            }
            ResourceLocation prev = realm.requirements().previousRealm();
            if (prev != null && RealmRegistry.getRealm(prev) == null) {
                warnings.add("Reino '" + realm.id() + "' referencia 'previous_realm' inexistente: '" + prev + "'.");
            }
        }

        // 2. Facções: limiares e relações default inválidas
        for (FactionDefinition faction : factions) {
            if (faction.id() == null) {
                errors.add("Facção com ID nulo registrada.");
                continue;
            }
            if (faction.subjugationThreshold() <= 0.0F || faction.subjugationThreshold() > 1.0F) {
                errors.add("Facção '" + faction.id() + "' possui subjugation_threshold inválido: " + faction.subjugationThreshold());
            }
            for (ResourceLocation relTarget : faction.defaultRelationships().keySet()) {
                if (FactionRegistry.getFaction(relTarget) == null) {
                    warnings.add("Facção '" + faction.id() + "' referencia relacionamento com facção inexistente: '" + relTarget + "'.");
                }
            }
        }

        // 3. Contratos v2: interseção allowed/forbidden e requisitos de reino acima do máximo
        for (ResourceLocation contractId : contractIds) {
            ContractDefinition def = ContractManager.getDefinition(contractId);
            if (def == null) {
                continue;
            }
            for (FactionType allowed : def.allowedFactions()) {
                if (def.forbiddenFactions().contains(allowed)) {
                    errors.add("Contrato '" + contractId + "' lista facção '" + allowed.getId()
                            + "' simultaneamente em allowed_factions e forbidden_factions.");
                }
            }
            if (def.minBodyRealm() > maxBodyOrder) {
                warnings.add("Contrato '" + contractId + "' exige Corpo R" + def.minBodyRealm()
                        + ", acima do máximo carregado (R" + maxBodyOrder + ").");
            }
            if (def.minSpiritRealm() > maxSpiritOrder) {
                warnings.add("Contrato '" + contractId + "' exige Espírito R" + def.minSpiritRealm()
                        + ", acima do máximo carregado (R" + maxSpiritOrder + ").");
            }
        }

        // 4. Habilidades Divinas: parâmetros numéricos e pré-requisitos de reino
        for (DivineSkillDefinition skill : skills) {
            if (skill.id() == null) {
                errors.add("Habilidade divina com ID nulo registrada.");
                continue;
            }
            if (skill.costMultiplier() <= 0.0F || skill.cooldownMultiplier() <= 0.0F || skill.baseRadius() <= 0.0F) {
                errors.add("Habilidade '" + skill.id() + "' possui parâmetros numéricos inválidos (<= 0).");
            }
            if (skill.minBodyRealm() > maxBodyOrder) {
                warnings.add("Habilidade '" + skill.id() + "' exige Corpo R" + skill.minBodyRealm()
                        + ", acima do máximo carregado (R" + maxBodyOrder + ").");
            }
            if (skill.minSpiritRealm() > maxSpiritOrder) {
                warnings.add("Habilidade '" + skill.id() + "' exige Espírito R" + skill.minSpiritRealm()
                        + ", acima do máximo carregado (R" + maxSpiritOrder + ").");
            }
        }

        return new ValidationReport(
                realms.size(),
                factions.size(),
                contractIds.size(),
                skills.size(),
                Collections.unmodifiableList(errors),
                Collections.unmodifiableList(warnings)
        );
    }

    public static ValidationReport logValidationReport() {
        ValidationReport report = validateAll();
        MythicContractsMod.LOGGER.info(
                "[{}] Validação de datapacks: {} reinos, {} facções, {} contratos, {} resquícios | erros={}, avisos={}",
                MythicContractsMod.MOD_ID,
                report.realmCount(),
                report.factionCount(),
                report.contractCount(),
                report.skillCount(),
                report.errors().size(),
                report.warnings().size()
        );
        for (String err : report.errors()) {
            MythicContractsMod.LOGGER.error("[{}] [Datapack Erro] {}", MythicContractsMod.MOD_ID, err);
        }
        for (String warn : report.warnings()) {
            MythicContractsMod.LOGGER.warn("[{}] [Datapack Aviso] {}", MythicContractsMod.MOD_ID, warn);
        }
        return report;
    }

    @Mod.EventBusSubscriber(modid = MythicContractsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class ServerValidationEvents {
        private ServerValidationEvents() {
        }

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            logValidationReport();
        }

        @SubscribeEvent
        public static void onDatapackSync(OnDatapackSyncEvent event) {
            if (event.getPlayer() == null) {
                logValidationReport();
            }
        }
    }
}
