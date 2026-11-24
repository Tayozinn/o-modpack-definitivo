package com.mythiccontracts.core.player;

import net.minecraft.resources.ResourceLocation;

/**
 * Estado individual de um contrato ativo firmado pelo jogador.
 */
public record ActiveContractState(
        ResourceLocation contractId,
        long signedAtTick,
        int remainingDurationTicks,
        int violationCount
) {
    public ActiveContractState(ResourceLocation contractId) {
        this(contractId, 0L, -1, 0);
    }

    public ActiveContractState withRemainingDuration(int newDurationTicks) {
        return new ActiveContractState(this.contractId, this.signedAtTick, newDurationTicks, this.violationCount);
    }

    public ActiveContractState withViolationIncremented() {
        return new ActiveContractState(this.contractId, this.signedAtTick, this.remainingDurationTicks, this.violationCount + 1);
    }
}
