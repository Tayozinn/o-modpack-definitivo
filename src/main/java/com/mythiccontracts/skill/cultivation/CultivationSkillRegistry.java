package com.mythiccontracts.skill.cultivation;

import com.mythiccontracts.MythicContractsMod;
import com.mythiccontracts.capability.FactionType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registro central Data-Driven das 12 Técnicas de Cultivo R1–R7 com fallbacks built-in.
 */
public final class CultivationSkillRegistry {

    private static final Map<ResourceLocation, CultivationSkillDefinition> BUILTIN_FALLBACKS = createBuiltinFallbacks();
    private static volatile Map<ResourceLocation, CultivationSkillDefinition> skillsById = BUILTIN_FALLBACKS;

    private CultivationSkillRegistry() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(MythicContractsMod.MOD_ID, path);
    }

    private static Map<ResourceLocation, CultivationSkillDefinition> createBuiltinFallbacks() {
        Map<ResourceLocation, CultivationSkillDefinition> map = new LinkedHashMap<>();

        // R1 — Fundamentos
        register(map, new CultivationSkillDefinition(
                id("gale_meridian_step"),
                "Passo do Meridiano Vendaval",
                SkillPathType.BODY,
                CultivationSkillCategory.MOBILITY,
                1,
                0xFFF87171,
                "Impulso tático veloz com redução de 50% de dano de queda e Velocidade I por 3s.",
                0.0F, 25.0F, 0.0F, 100, false,
                new SkillUnlockRequirements(1, 2, 1, 1, null, false, Set.of(), Set.of(), 0, 0, 30.0F),
                true
        ));
        register(map, new CultivationSkillDefinition(
                id("inner_breath_meditation"),
                "Respiração do Lótus Interior",
                SkillPathType.SPIRIT,
                CultivationSkillCategory.SUPPORT,
                1,
                0xFFC084FC,
                "Ativa estado de Meditação instantâneo (+150% regen espiritual e Qi fora de combate por 6s) e limpa lentidão.",
                0.0F, 0.0F, 0.0F, 280, false,
                new SkillUnlockRequirements(1, 1, 1, 2, null, false, Set.of(), Set.of(), 0, 0, 30.0F),
                true
        ));

        // R2 — Despertar
        register(map, new CultivationSkillDefinition(
                id("jade_bone_guard"),
                "Guarda de Osso & Ferro",
                SkillPathType.BODY,
                CultivationSkillCategory.DEFENSIVE,
                2,
                0xFFF59E0B,
                "Por 4s: +6 Armadura, +40% Resistência a Repulsão e redireciona 25% do dano físico para o Vigor.",
                0.0F, 30.0F, 0.0F, 280, false,
                new SkillUnlockRequirements(2, 1, 1, 1, id("gale_meridian_step"), false, Set.of(), Set.of(), 0, 0, 50.0F),
                true
        ));
        register(map, new CultivationSkillDefinition(
                id("soul_subjugation"),
                "Grilhão de Subjugação Espiritual",
                SkillPathType.SPIRIT,
                CultivationSkillCategory.DOMAIN,
                2,
                0xFFA855F7,
                "Inicia a Disputa de Subjugação no alvo enfraquecido (reaproveita Selo como catalisador) ou invoca/recolhe servo.",
                30.0F, 15.0F, 0.0F, 200, false,
                new SkillUnlockRequirements(
                        1, 1, 2, 1, null, false, Set.of(),
                        EnumSet.of(FactionType.QUEBRADORES_DE_TRONOS), 0, 0, 40.0F
                ),
                true
        ));

        // R3 — Projeção
        register(map, new CultivationSkillDefinition(
                id("seismic_palm"),
                "Palma Sísmica Quebra-Rocha",
                SkillPathType.BODY,
                CultivationSkillCategory.OFFENSIVE,
                3,
                0xFFEF4444,
                "Onda de choque frontal escalada por Cultivo Corporal que quebra guarda de escudos e lança inimigos.",
                15.0F, 35.0F, 0.0F, 200, false,
                new SkillUnlockRequirements(3, 1, 1, 1, id("jade_bone_guard"), false, Set.of(), Set.of(), 0, 0, 80.0F),
                true
        ));
        register(map, new CultivationSkillDefinition(
                id("golden_core_aegis"),
                "Égide do Núcleo Dourado",
                SkillPathType.SPIRIT,
                CultivationSkillCategory.DEFENSIVE,
                3,
                0xFFFBBF24,
                "Barreira de 6s que eleva a absorção da Integridade Espiritual para 75% e reflete 20% do dano em energia.",
                40.0F, 0.0F, 0.0F, 360, false,
                new SkillUnlockRequirements(1, 1, 3, 1, id("inner_breath_meditation"), false, Set.of(), Set.of(), 0, 0, 80.0F),
                true
        ));

        // R4 — Ressonância
        register(map, new CultivationSkillDefinition(
                id("blood_furnace_surge"),
                "Fornalha de Sangue & Jade",
                SkillPathType.BODY,
                CultivationSkillCategory.SUPPORT,
                4,
                0xFFDC2626,
                "Converte Vigor em recuperação de 15% da Vida Corporal máxima em 4s + 20% de velocidade de ataque.",
                0.0F, 30.0F, 0.0F, 480, false,
                new SkillUnlockRequirements(4, 1, 1, 1, id("seismic_palm"), false, Set.of(), Set.of(), 0, 0, 110.0F),
                true
        ));
        register(map, new CultivationSkillDefinition(
                id("nascent_soul_lance"),
                "Lança da Alma Nascente",
                SkillPathType.SPIRIT,
                CultivationSkillCategory.OFFENSIVE,
                4,
                0xFF38BDF8,
                "Projeta lança de Qi a 18m causando dano espiritual puro que ignora armadura física.",
                45.0F, 0.0F, 10.0F, 180, false,
                new SkillUnlockRequirements(1, 1, 4, 1, id("golden_core_aegis"), false, Set.of(), Set.of(), 0, 0, 110.0F),
                true
        ));

        // R5 — Domínio
        register(map, new CultivationSkillDefinition(
                id("ether_flash_strike"),
                "Lampejo do Vácuo Etéreo",
                SkillPathType.HYBRID,
                CultivationSkillCategory.MOBILITY,
                5,
                0xFF22D3EE,
                "Avanço relâmpago até o alvo (<= 10m) desferindo golpe instantâneo imbuído com 100% da Infusão [V].",
                30.0F, 30.0F, 0.0F, 240, false,
                new SkillUnlockRequirements(5, 1, 4, 1, id("gale_meridian_step"), false, Set.of(), Set.of(), 0, 0, 140.0F),
                true
        ));
        register(map, new CultivationSkillDefinition(
                id("spirit_domain_suppression"),
                "Domínio de Pressão Espiritual",
                SkillPathType.SPIRIT,
                CultivationSkillCategory.SUPPORT,
                5,
                0xFF9333EA,
                "Aura de 8m por 8s que reduz ataque/velocidade de inimigos menores (-25%) e fortalece servos subjugados (+25%).",
                60.0F, 0.0F, 0.0F, 560, false,
                new SkillUnlockRequirements(1, 1, 5, 1, id("soul_subjugation"), true, Set.of(), Set.of(), 1, 0, true, 150.0F),
                true
        ));

        // R6 — Transcendência
        register(map, new CultivationSkillDefinition(
                id("undying_meridian_reversal"),
                "Reversão dos Meridianos Imortais",
                SkillPathType.HYBRID,
                CultivationSkillCategory.DEFENSIVE,
                6,
                0xFFE879F9,
                "Usável mesmo em Colapso Espiritual: purga a fratura da alma, restaura 40% de Integridade e concede imunidade a repulsão por 3s.",
                50.0F, 50.0F, 0.0F, 900, true,
                new SkillUnlockRequirements(6, 1, 6, 1, id("golden_core_aegis"), false, Set.of(), Set.of(), 0, 0, 180.0F),
                true
        ));

        // R7 — Técnica Suprema
        register(map, new CultivationSkillDefinition(
                id("primordial_heaven_sunder"),
                "Ruptura do Céu Primordial",
                SkillPathType.HYBRID,
                CultivationSkillCategory.SUPREME,
                7,
                0xFFFBBF24,
                "Golpe supremo em cone (10m) somando 2.5x poder Corporal + 2.5x poder Espiritual, perfurando 100% da Pressão de Reino.",
                90.0F, 70.0F, 35.0F, 1200, false,
                new SkillUnlockRequirements(7, 1, 7, 1, id("ether_flash_strike"), true, Set.of(), Set.of(), 0, 50, true, 240.0F),
                true
        ));

        return Collections.unmodifiableMap(map);
    }

    private static void register(Map<ResourceLocation, CultivationSkillDefinition> map, CultivationSkillDefinition def) {
        map.put(def.id(), def);
    }

    public static void setLoadedSkills(Map<ResourceLocation, CultivationSkillDefinition> parsed) {
        Map<ResourceLocation, CultivationSkillDefinition> merged = new LinkedHashMap<>(BUILTIN_FALLBACKS);
        if (parsed != null) {
            merged.putAll(parsed);
        }
        skillsById = Collections.unmodifiableMap(merged);
    }

    public static void resetToBuiltins() {
        skillsById = BUILTIN_FALLBACKS;
    }

    public static Collection<CultivationSkillDefinition> getAllSkills() {
        return skillsById.values();
    }

    @Nullable
    public static CultivationSkillDefinition getSkill(@Nullable ResourceLocation id) {
        return id != null ? skillsById.get(id) : null;
    }

    public static String getDisplayName(@Nullable ResourceLocation id) {
        if (id == null) {
            return "";
        }
        CultivationSkillDefinition def = skillsById.get(id);
        return def != null ? def.displayName() : id.getPath();
    }
}
