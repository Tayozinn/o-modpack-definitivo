package com.mythiccontracts.client;

import com.mythiccontracts.client.screen.HudConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/**
 * Isolamento explícito de registros exclusivos do cliente para impedir o carregamento
 * de classes de UI/Screen na JVM de servidor dedicado.
 */
public final class ClientBootstrap {

    private ClientBootstrap() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new HudConfigScreen(parent))
        );
    }
}
