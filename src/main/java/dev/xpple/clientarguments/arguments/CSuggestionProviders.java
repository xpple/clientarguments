package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

public class CSuggestionProviders {
    public static final SuggestionProvider<FabricClientCommandSource> ALL_RECIPES = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getRecipeIds(), builder);
    public static final SuggestionProvider<FabricClientCommandSource> AVAILABLE_SOUNDS = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getSoundIds(), builder);
    public static final SuggestionProvider<FabricClientCommandSource> SUMMONABLE_ENTITIES = (context, builder) -> CommandSource.suggestFromIdentifier(Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable), builder, EntityType::getId, EntityType::getName);
}
