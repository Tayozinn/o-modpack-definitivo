package com.mythiccontracts.core.player;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Armazena contratos ativos, condições/estados individuais e histórico de quebras.
 */
public class ContractData {

    private final Map<ResourceLocation, ActiveContractState> activeContracts = new LinkedHashMap<>();
    private final Set<ResourceLocation> brokenContractsHistory = new LinkedHashSet<>();
    private boolean vowBroken = false;

    public boolean hasContract(ResourceLocation contractId) {
        return contractId != null && this.activeContracts.containsKey(contractId);
    }

    public boolean addContract(ResourceLocation contractId) {
        return addContract(contractId, 0L, -1);
    }

    public boolean addContract(ResourceLocation contractId, long signedAtTick, int durationTicks) {
        if (contractId == null) {
            return false;
        }
        boolean isNew = !this.activeContracts.containsKey(contractId);
        this.activeContracts.put(contractId, new ActiveContractState(contractId, signedAtTick, durationTicks, 0));
        return isNew;
    }

    public void putContractState(ActiveContractState state) {
        if (state != null && state.contractId() != null) {
            this.activeContracts.put(state.contractId(), state);
        }
    }

    public boolean removeContract(ResourceLocation contractId) {
        if (contractId == null) {
            return false;
        }
        return this.activeContracts.remove(contractId) != null;
    }

    public void clearContracts() {
        this.activeContracts.clear();
    }

    public Set<ResourceLocation> getActiveContractIds() {
        return Collections.unmodifiableSet(this.activeContracts.keySet());
    }

    public Map<ResourceLocation, ActiveContractState> getActiveContractStates() {
        return Collections.unmodifiableMap(this.activeContracts);
    }

    public Set<ResourceLocation> getBrokenContractsHistory() {
        return Collections.unmodifiableSet(this.brokenContractsHistory);
    }

    public void recordBrokenContract(ResourceLocation contractId) {
        if (contractId != null) {
            this.brokenContractsHistory.add(contractId);
        }
    }

    public void clearBrokenHistory(ResourceLocation contractId) {
        if (contractId != null) {
            this.brokenContractsHistory.remove(contractId);
        }
    }

    public boolean isVowBroken() {
        return this.vowBroken;
    }

    public void setVowBroken(boolean vowBroken) {
        this.vowBroken = vowBroken;
    }

    public void copyFrom(ContractData other) {
        if (other == null) {
            return;
        }
        this.activeContracts.clear();
        this.activeContracts.putAll(other.activeContracts);
        this.brokenContractsHistory.clear();
        this.brokenContractsHistory.addAll(other.brokenContractsHistory);
        this.vowBroken = other.vowBroken;
    }
}
