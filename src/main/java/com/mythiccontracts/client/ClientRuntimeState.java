package com.mythiccontracts.client;

/**
 * Estado volátil exclusivo do cliente sincronizado via {@code SyncEnergyS2CPacket}
 * (progresso de Disputa de Subjugação e cooldown da técnica marcial ativa).
 */
public final class ClientRuntimeState {

    private static volatile int activeMartialCooldownTicks = 0;
    private static volatile boolean clashActive = false;
    private static volatile float clashProgress = 0.0F;
    private static volatile boolean clashRevoltWindow = false;
    private static volatile String clashTargetName = "";

    private ClientRuntimeState() {
    }

    public static void updateEnergyRuntime(
            int activeCooldownTicks,
            boolean isClashActive,
            float progress,
            boolean revoltWindow,
            String targetName
    ) {
        activeMartialCooldownTicks = Math.max(0, activeCooldownTicks);
        clashActive = isClashActive;
        clashProgress = isClashActive ? Math.max(0.0F, Math.min(1.0F, progress)) : 0.0F;
        clashRevoltWindow = isClashActive && revoltWindow;
        clashTargetName = isClashActive && targetName != null ? targetName : "";
    }

    public static void reset() {
        activeMartialCooldownTicks = 0;
        clashActive = false;
        clashProgress = 0.0F;
        clashRevoltWindow = false;
        clashTargetName = "";
    }

    public static int getActiveMartialCooldownTicks() {
        return activeMartialCooldownTicks;
    }

    public static boolean isClashActive() {
        return clashActive;
    }

    public static float getClashProgress() {
        return clashProgress;
    }

    public static boolean isClashRevoltWindow() {
        return clashRevoltWindow;
    }

    public static String getClashTargetName() {
        return clashTargetName;
    }
}
