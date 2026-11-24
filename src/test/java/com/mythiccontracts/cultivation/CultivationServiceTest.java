package com.mythiccontracts.cultivation;

import com.google.gson.JsonParser;
import com.mythiccontracts.core.player.PlayerData;
import com.mythiccontracts.core.service.EnergyService;
import com.mythiccontracts.cultivation.progression.CultivationService;
import com.mythiccontracts.cultivation.realm.CultivationPath;
import com.mythiccontracts.cultivation.realm.RealmDefinition;
import com.mythiccontracts.cultivation.realm.RealmRegistry;
import com.mythiccontracts.cultivation.realm.RealmReloadListener;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CultivationServiceTest {

    @AfterEach
    void restoreBuiltins() {
        RealmRegistry.resetToBuiltins();
    }

    @Test
    void builtinRealmsPreserveExactV1BalanceCurves() {
        assertEquals(0.5D, CultivationBalance.calculateBodyPassiveAttackBonus(1, 1), 0.001D);
        assertEquals(31.5D, CultivationBalance.calculateBodyPassiveAttackBonus(7, 3), 0.001D);
        assertEquals(0.05F, CultivationBalance.calculateBodyInfusionMultiplier(1, 1), 0.0001F);
        assertEquals(1.35F, CultivationBalance.calculateBodyInfusionMultiplier(7, 3), 0.0001F);
        assertEquals(1.0F, CultivationBalance.calculateSpiritInfusionFlatBonus(1, 1), 0.001F);
        assertEquals(63.0F, CultivationBalance.calculateSpiritInfusionFlatBonus(7, 3), 0.001F);
        assertEquals(100.0F, CultivationBalance.calculateMaxQi(1, 1, 1), 0.01F);
        assertEquals(390.0F, CultivationBalance.calculateMaxQi(7, 3, 7), 0.01F);
    }

    @Test
    void supportsCustomJsonRealmWithFourStagesAndRequirements() {
        String customRealmJson = """
                {
                  "id": "mythic_contracts:reino_da_fortificacao",
                  "display_name": "Reino da Fortificação",
                  "type": "body",
                  "order": 8,
                  "stages": [
                    "base",
                    "intermediario",
                    "avancado",
                    "perfeicao"
                  ],
                  "requirements": {
                    "previous_realm": "mythic_contracts:body_realm_7",
                    "min_opposite_realm_order": 5,
                    "qi_cost": 50.0
                  },
                  "benefits": {
                    "health": 4.0,
                    "armor": 2.0
                  }
                }
                """;

        RealmDefinition customDef = RealmReloadListener.parseRealm(
                new ResourceLocation("mythic_contracts", "body/reino_da_fortificacao"),
                JsonParser.parseString(customRealmJson).getAsJsonObject()
        );
        RealmRegistry.setLoadedRealms(Map.of(customDef.id(), customDef));

        assertEquals(4, customDef.getStageCount());
        assertEquals(8, RealmRegistry.getMaxOrder(CultivationPath.BODY));
        assertEquals(2, customDef.getStageByNumber(1).attributes().size());

        PlayerData data = new PlayerData();
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.BODY, 7, 3);
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 3, 1);
        EnergyService.recalculateMaxQi(data);

        // Falha sem espírito ordem >= 5
        CultivationService.AdvancementResult failedOpp = CultivationService.attemptBreakthrough(data, CultivationPath.BODY);
        assertEquals(CultivationService.AdvancementOutcome.MISSING_OPPOSITE_REALM, failedOpp.outcome());

        // Agora atende espírito ordem 5, mas zera Qi -> falha por Qi insuficiente
        CultivationService.setRealmAndStageByOrder(data, CultivationPath.SPIRIT, 5, 1);
        EnergyService.recalculateMaxQi(data);
        data.getCultivation().setCurrentQi(10.0F);
        CultivationService.AdvancementResult failedQi = CultivationService.attemptBreakthrough(data, CultivationPath.BODY);
        assertEquals(CultivationService.AdvancementOutcome.INSUFFICIENT_QI, failedQi.outcome());

        // Com Qi suficiente -> rompe para o Reino 8 customizado
        data.getCultivation().setCurrentQi(100.0F);
        CultivationService.AdvancementResult success = CultivationService.attemptBreakthrough(data, CultivationPath.BODY);
        assertTrue(success.isSuccess());
        assertEquals(customDef.id(), data.getCultivation().getBodyRealmId());
        assertEquals(8, data.getCultivation().getBodyRealm());
        assertEquals(1, data.getCultivation().getBodyStage());
        assertEquals(50.0F, data.getCultivation().getCurrentQi(), 0.01F);

        // Avança os 4 estágios até Perfeição
        assertTrue(CultivationService.advanceStage(data, CultivationPath.BODY).isSuccess()); // 2
        assertTrue(CultivationService.advanceStage(data, CultivationPath.BODY).isSuccess()); // 3
        assertTrue(CultivationService.advanceStage(data, CultivationPath.BODY).isSuccess()); // 4 (perfeicao)
        assertEquals(4, data.getCultivation().getBodyStage());

        // Após estágio 4 sem Reino 9 -> MAX_REALM_REACHED
        CultivationService.AdvancementResult maxReached = CultivationService.advanceStage(data, CultivationPath.BODY);
        assertEquals(CultivationService.AdvancementOutcome.MAX_REALM_REACHED, maxReached.outcome());
    }

    @Test
    void stageAdvanceStopsAtFinalStageAndSignalsBreakthroughNeeded() {
        PlayerData data = new PlayerData();
        assertTrue(CultivationService.advanceStage(data, CultivationPath.SPIRIT).isSuccess());
        assertTrue(CultivationService.advanceStage(data, CultivationPath.SPIRIT).isSuccess());
        assertEquals(3, data.getCultivation().getSpiritStage());

        CultivationService.AdvancementResult needsBreak = CultivationService.advanceStage(data, CultivationPath.SPIRIT);
        assertEquals(CultivationService.AdvancementOutcome.NEEDS_BREAKTHROUGH, needsBreak.outcome());
    }
}
