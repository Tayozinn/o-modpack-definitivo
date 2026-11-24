package com.mythiccontracts.skill.remnant;

import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo das 16 habilidades progressivas em 4 Fases (4 Deuses Caídos × Fases I–IV).
 * Cada fase desbloqueia uma nova forma acionável em [G] (alternada com Shift+G) além da evolução da anterior.
 */
public record DivinePhaseAbility(
        DivineRemnant remnant,
        int phaseIndex,
        String code,
        String displayName,
        String summary,
        float cooldownMultiplier
) {
    private static final Map<DivineRemnant, List<DivinePhaseAbility>> BY_REMNANT = buildCatalog();

    private static Map<DivineRemnant, List<DivinePhaseAbility>> buildCatalog() {
        Map<DivineRemnant, List<DivinePhaseAbility>> map = new EnumMap<>(DivineRemnant.class);

        map.put(DivineRemnant.EYE_OF_RA, List.of(
                new DivinePhaseAbility(DivineRemnant.EYE_OF_RA, 0, "H1", "Faísca Solar",
                        "Feixe focado no alvo na mira (<= 8m), dano mágico contido + Brilho (4s).", 1.00F),
                new DivinePhaseAbility(DivineRemnant.EYE_OF_RA, 1, "H2", "Égide do Disco Solar",
                        "Pulso protetor (3.5m): Absorção + Regeneração, purga Veneno/Wither e afasta inimigos. Passiva: -20% dano de fogo.", 1.10F),
                new DivinePhaseAbility(DivineRemnant.EYE_OF_RA, 2, "H3", "Julgamento do Olho de Rá",
                        "Área solar plena (6.5m+) com pilares solares, Brilho, Fraqueza, fogo e cura escalável.", 1.25F),
                new DivinePhaseAbility(DivineRemnant.EYE_OF_RA, 3, "H4", "Zênite do Sol Imortal",
                        "Ultimate (6s): invoca o Grande Olho disparando 3 ondas de pilares (10m, +35% em mortos-vivos) e restaura Integridade Espiritual.", 2.20F)
        ));

        map.put(DivineRemnant.MORRIGAN_REAP, List.of(
                new DivinePhaseAbility(DivineRemnant.MORRIGAN_REAP, 0, "H1", "Corte do Presságio",
                        "Meia-lua estreita frontal (3m, cone 90°), dano contido + Wither I (2.5s) e micro-cura (máx. 2 HP).", 0.95F),
                new DivinePhaseAbility(DivineRemnant.MORRIGAN_REAP, 1, "H2", "Voo dos Corvos",
                        "Enxame linear (10m) com Wither + Lentidão; abater marcados restaura Vigor e HP (passiva).", 1.10F),
                new DivinePhaseAbility(DivineRemnant.MORRIGAN_REAP, 2, "H3", "Ceifa Abissal 360°",
                        "Varredura circular plena (6.5m+) com arcos carmesim e drenagem vital proporcional ao dano.", 1.25F),
                new DivinePhaseAbility(DivineRemnant.MORRIGAN_REAP, 3, "H4", "Rainha dos Campos de Batalha",
                        "Ultimate (8s): Asas Espectrais, 2 ondas de foice (9m) e golpes melee aplicam Wither II + 18% roubo de vida.", 2.20F)
        ));

        map.put(DivineRemnant.INDRAS_VAJRA, List.of(
                new DivinePhaseAbility(DivineRemnant.INDRAS_VAJRA, 0, "H1", "Centelha do Vajra",
                        "Estalo elétrico curto (4m, até 2 alvos), dano sônico contido + Lentidão I (2s).", 1.00F),
                new DivinePhaseAbility(DivineRemnant.INDRAS_VAJRA, 1, "H2", "Lança Trovejante",
                        "Raio perfurante (12m) com salto elétrico para até 2 alvos e Fraqueza. Passiva: -10% recarga do Resquício.", 1.10F),
                new DivinePhaseAbility(DivineRemnant.INDRAS_VAJRA, 2, "H3", "Trovão Soberano do Vajra",
                        "Explosão sônica plena (7.5m+) com bônus pesado contra Bosses (#high_realm_boss) e repulsão.", 1.25F),
                new DivinePhaseAbility(DivineRemnant.INDRAS_VAJRA, 3, "H4", "Tempestade de Mil Raios",
                        "Ultimate (5s): marca 10m e invoca 5 relâmpagos consecutivos priorizando Bosses (+75% dano), quebrando escudos.", 2.20F)
        ));

        map.put(DivineRemnant.TEZCATLIPOCA_STEP, List.of(
                new DivinePhaseAbility(DivineRemnant.TEZCATLIPOCA_STEP, 0, "H1", "Fenda Fumegante",
                        "Esquiva curta (3.5m) com 1 corte leve na saída + Velocidade I (1.5s).", 0.85F),
                new DivinePhaseAbility(DivineRemnant.TEZCATLIPOCA_STEP, 1, "H2", "Reflexo do Espelho",
                        "Cria Espelho Fumegante na origem (Cegueira + Lentidão 3.5m), recuo evasivo + Invisibilidade (2s). Passiva: -25% dano de queda.", 1.05F),
                new DivinePhaseAbility(DivineRemnant.TEZCATLIPOCA_STEP, 2, "H3", "Passo de Tezcatlipoca Pleno",
                        "Corte amplo na saída + investida longa (6.5m), Velocidade II e Força I.", 1.20F),
                new DivinePhaseAbility(DivineRemnant.TEZCATLIPOCA_STEP, 3, "H4", "Eclipse do Jaguar Negro",
                        "Ultimate: 3 avanços em zigue-zague (até 10m) deixando pós-imagens que detonam em lascas de obsidiana.", 2.10F)
        ));

        return Collections.unmodifiableMap(map);
    }

    public static DivinePhaseAbility get(DivineRemnant remnant, int phaseIndex) {
        DivineRemnant safe = remnant != null ? remnant : DivineRemnant.TEZCATLIPOCA_STEP;
        List<DivinePhaseAbility> list = BY_REMNANT.get(safe);
        return list.get(Mth.clamp(phaseIndex, 0, list.size() - 1));
    }

    public static List<DivinePhaseAbility> getAllFor(DivineRemnant remnant) {
        DivineRemnant safe = remnant != null ? remnant : DivineRemnant.TEZCATLIPOCA_STEP;
        return BY_REMNANT.get(safe);
    }

    public static String getShortCode(int phaseIndex) {
        return "H" + (Mth.clamp(phaseIndex, 0, 3) + 1);
    }
}
