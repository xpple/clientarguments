package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;

public class CSuggestionProviders {
    public static final SuggestionProvider<FabricClientCommandSource> AVAILABLE_SOUNDS = (context, builder) -> SharedSuggestionProvider.suggestResource(context.getSource().getAvailableSounds(), builder);
    public static final SuggestionProvider<FabricClientCommandSource> SUMMONABLE_ENTITIES = (context, builder) -> SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.stream().filter(EntityType::canSummon), builder, EntityType::getKey, EntityType::getDescription);
}
