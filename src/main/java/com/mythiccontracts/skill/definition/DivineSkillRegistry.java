package com.mythiccontracts.skill.definition;

import com.mythiccontracts.skill.remnant.DivineRemnant;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registro central data-driven dos Resquícios Divinos com fallbacks built-in idênticos ao balanceamento base.
 */
public final class DivineSkillRegistry {

    private static final Map<ResourceLocation, DivineSkillDefinition> BUILTIN_FALLBACKS = createBuiltinFallbacks();
    private static volatile Map<ResourceLocation, DivineSkillDefinition> skillsById = BUILTIN_FALLBACKS;

    private DivineSkillRegistry() {
    }

    private static Map<ResourceLocation, DivineSkillDefinition> createBuiltinFallbacks() {
        Map<ResourceLocation, DivineSkillDefinition> map = new LinkedHashMap<>();

        map.put(DivineRemnant.EYE_OF_RA.getId(), new DivineSkillDefinition(
                DivineRemnant.EYE_OF_RA.getId(),
                DivineRemnant.EYE_OF_RA.getDisplayName(),
                DivineRemnant.EYE_OF_RA.getDeityOrigin(),
                DivineRemnant.EYE_OF_RA.getHudColor(),
                "Julgamento Solar · Purificação & Defesa",
                "Dano mágico em área, Brilho + Fraqueza + repulsão, cura/absorção pessoal (fogo na Camada IV).",
                6.0F, 2.5F, 1.0F, 5.5F,
                1.0F, 1.0F, 1.0F,
                1, 1, true
        ));

        map.put(DivineRemnant.MORRIGAN_REAP.getId(), new DivineSkillDefinition(
                DivineRemnant.MORRIGAN_REAP.getId(),
                DivineRemnant.MORRIGAN_REAP.getDisplayName(),
                DivineRemnant.MORRIGAN_REAP.getDeityOrigin(),
                DivineRemnant.MORRIGAN_REAP.getHudColor(),
                "Colheita Sombria · Roubo de Vida & Decomposição",
                "Corte físico em área com Decomposição (Wither) e cura proporcional ao dano causado.",
                7.0F, 2.5F, 1.5F, 6.0F,
                1.0F, 1.0F, 1.0F,
                1, 1, true
        ));

        map.put(DivineRemnant.INDRAS_VAJRA.getId(), new DivineSkillDefinition(
                DivineRemnant.INDRAS_VAJRA.getId(),
                DivineRemnant.INDRAS_VAJRA.getDisplayName(),
                DivineRemnant.INDRAS_VAJRA.getDeityOrigin(),
                DivineRemnant.INDRAS_VAJRA.getHudColor(),
                "Trovão Soberano · Quebra de Chefes",
                "Explosão sônica em área, bônus contra Chefes de Alto Reino, Lentidão + Fraqueza e forte repulsão.",
                6.5F, 3.0F, 1.0F, 6.5F,
                1.0F, 1.0F, 1.0F,
                1, 1, true
        ));

        map.put(DivineRemnant.TEZCATLIPOCA_STEP.getId(), new DivineSkillDefinition(
                DivineRemnant.TEZCATLIPOCA_STEP.getId(),
                DivineRemnant.TEZCATLIPOCA_STEP.getDisplayName(),
                DivineRemnant.TEZCATLIPOCA_STEP.getDeityOrigin(),
                DivineRemnant.TEZCATLIPOCA_STEP.getHudColor(),
                "Espelho Fumegante · Impulso & Mobilidade",
                "Corte ao redor seguido de investida frontal, Velocidade + Dano (Invisibilidade breve na Camada IV).",
                5.0F, 1.5F, 1.5F, 4.5F,
                1.0F, 1.0F, 1.0F,
                1, 1, true
        ));

        return Collections.unmodifiableMap(map);
    }

    public static void setLoadedSkills(Map<ResourceLocation, DivineSkillDefinition> parsed) {
        Map<ResourceLocation, DivineSkillDefinition> merged = new LinkedHashMap<>(BUILTIN_FALLBACKS);
        if (parsed != null) {
            merged.putAll(parsed);
        }
        skillsById = Collections.unmodifiableMap(merged);
    }

    public static void resetToBuiltins() {
        skillsById = BUILTIN_FALLBACKS;
    }

    public static Collection<DivineSkillDefinition> getAllSkills() {
        return skillsById.values();
    }

    public static DivineSkillDefinition getSkill(@Nullable ResourceLocation id) {
        DivineRemnant remnant = DivineRemnant.fromSkillId(id);
        DivineSkillDefinition def = id != null ? skillsById.get(id) : null;
        if (def != null) {
            return def;
        }
        DivineSkillDefinition byRemnant = skillsById.get(remnant.getId());
        return byRemnant != null ? byRemnant : BUILTIN_FALLBACKS.get(DivineRemnant.TEZCATLIPOCA_STEP.getId());
    }

    public static DivineSkillDefinition getForRemnant(@Nullable DivineRemnant remnant) {
        DivineRemnant safe = remnant != null ? remnant : DivineRemnant.TEZCATLIPOCA_STEP;
        DivineSkillDefinition def = skillsById.get(safe.getId());
        return def != null ? def : BUILTIN_FALLBACKS.get(safe.getId());
    }
}
