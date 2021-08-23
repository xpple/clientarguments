package dev.xpple.clientarguments;

import dev.xpple.clientarguments.arguments.CEntitySelectorOptions;
import net.fabricmc.api.ClientModInitializer;

public class ClientArguments implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        CEntitySelectorOptions.register();
    }
}
