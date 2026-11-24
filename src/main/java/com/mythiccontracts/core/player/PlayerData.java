package com.mythiccontracts.core.player;

/**
 * Agregador central modular dos dados de progressão RPG de cada jogador.
 * Contém apenas estado puro, organizado em cinco subdomínios independentes.
 */
public class PlayerData {

    private final CultivationData cultivation = new CultivationData();
    private final FactionData faction = new FactionData();
    private final ContractData contracts = new ContractData();
    private final SkillData skills = new SkillData();
    private final SubjugationData subjugation = new SubjugationData();

    public CultivationData getCultivation() {
        return this.cultivation;
    }

    public FactionData getFaction() {
        return this.faction;
    }

    public ContractData getContracts() {
        return this.contracts;
    }

    public SkillData getSkills() {
        return this.skills;
    }

    public SubjugationData getSubjugation() {
        return this.subjugation;
    }

    public void copyFrom(PlayerData other, boolean wasDeath) {
        if (other == null) {
            return;
        }
        this.cultivation.copyFrom(other.cultivation, wasDeath);
        this.faction.copyFrom(other.faction);
        this.contracts.copyFrom(other.contracts);
        this.skills.copyFrom(other.skills, wasDeath);
        this.subjugation.copyFrom(other.subjugation);
    }
}
