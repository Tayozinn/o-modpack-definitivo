package com.mythiccontracts.faction;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro central data-driven de Facções com fallbacks built-in para todas as 5 facções principais.
 */
public final class FactionRegistry {

    private static final Map<ResourceLocation, FactionDefinition> BUILTIN_FALLBACKS = createBuiltinFallbacks();
    private static volatile Map<ResourceLocation, FactionDefinition> factionsById = BUILTIN_FALLBACKS;

    private FactionRegistry() {
    }

    private static Map<ResourceLocation, FactionDefinition> createBuiltinFallbacks() {
        Map<ResourceLocation, FactionDefinition> map = new LinkedHashMap<>();

        ResourceLocation noneId = id("none");
        ResourceLocation veilId = id("errantes_do_veu");
        ResourceLocation lightId = id("juramentados_da_luz");
        ResourceLocation debtId = id("irmandade_divida_eterna");
        ResourceLocation throneId = id("quebradores_de_tronos");

        map.put(noneId, new FactionDefinition(
                noneId,
                FactionType.NONE.getDisplayName(),
                "Cultivador livre sem juramento institucional.",
                0xFF94A3B8,
                0.15F,
                "Sem restrições de votos sagrados ou profanos de facção.",
                "Sem acesso a bônus exclusivos de facção até jurar lealdade.",
                List.of(),
                Map.of()
        ));

        map.put(veilId, new FactionDefinition(
                veilId,
                FactionType.ERRANTES_DO_VEU.getDisplayName(),
                "Cultivadores independentes que caminham entre Corpo, Espírito e Véu.",
                0xFF38BDF8,
                0.15F,
                "Equilíbrio versátil entre pactos neutros e domínio dos 4 Resquícios Divinos.",
                "Não firma contratos exclusivos dos Quebradores de Tronos.",
                List.of(),
                Map.of(lightId, "neutro", debtId, "neutro")
        ));

        map.put(lightId, new FactionDefinition(
                lightId,
                FactionType.JURAMENTADOS_DA_LUZ.getDisplayName(),
                "Guardiões de votos sagrados e proteção aos inocentes.",
                0xFFFBBF24,
                0.15F,
                "Compatível com Voto de Paladino (Absorção passiva; quebra ao atacar aldeões).",
                "Proíbe Pacto de Lúcifer e Juramento de Sangue.",
                List.of(),
                Map.of(debtId, "rival")
        ));

        map.put(debtId, new FactionDefinition(
                debtId,
                FactionType.IRMANDADE_DIVIDA_ETERNA.getDisplayName(),
                "Pactuantes pragmáticos que trocam vitalidade por poder ofensivo.",
                0xFFEF4444,
                0.15F,
                "Acesso livre ao Pacto de Lúcifer e Juramento de Sangue.",
                "Proíbe Voto de Paladino e Selo Quebra-Tronos.",
                List.of(),
                Map.of(lightId, "rival")
        ));

        map.put(throneId, new FactionDefinition(
                throneId,
                FactionType.QUEBRADORES_DE_TRONOS.getDisplayName(),
                "Domínio marcial focado em subjugação de entidades e supremacia.",
                0xFFA855F7,
                0.25F,
                "Subjugação facilitada (<= 25% HP contra 15% padrão) e acesso ao Selo Quebra-Tronos.",
                "Rejeitado pelas demais facções para contratos exclusivos Quebra-Tronos.",
                List.of(),
                Map.of(lightId, "hostil")
        ));

        return Collections.unmodifiableMap(map);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MythicContractsMod.MOD_ID, path);
    }

    public static void setLoadedFactions(Map<ResourceLocation, FactionDefinition> parsed) {
        Map<ResourceLocation, FactionDefinition> merged = new LinkedHashMap<>(BUILTIN_FALLBACKS);
        if (parsed != null) {
            merged.putAll(parsed);
        }
        factionsById = Collections.unmodifiableMap(merged);
    }

    public static void resetToBuiltins() {
        factionsById = BUILTIN_FALLBACKS;
    }

    public static Collection<FactionDefinition> getAllFactions() {
        return factionsById.values();
    }

    @Nullable
    public static FactionDefinition getFaction(@Nullable ResourceLocation factionId) {
        return factionId != null ? factionsById.get(factionId) : null;
    }

    public static FactionDefinition getForType(@Nullable FactionType type) {
        FactionType safe = type != null ? type : FactionType.NONE;
        ResourceLocation key = id(safe.getId());
        FactionDefinition def = factionsById.get(key);
        return def != null ? def : BUILTIN_FALLBACKS.get(id(FactionType.NONE.getId()));
    }

    public static float getSubjugationThreshold(@Nullable FactionType type) {
        return getForType(type).subjugationThreshold();
    }
}
