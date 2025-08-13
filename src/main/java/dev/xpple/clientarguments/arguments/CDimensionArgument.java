package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CDimensionArgument implements ArgumentType<ResourceLocation> {
	private static final Collection<String> EXAMPLES = Stream.of(Level.OVERWORLD, Level.NETHER)
		.map(key -> key.location().toString())
		.collect(Collectors.toList());
	private static final DynamicCommandExceptionType INVALID_DIMENSION_EXCEPTION = new DynamicCommandExceptionType(id -> Component.translatableEscape("argument.dimension.invalid", id));


	public static CDimensionArgument dimension() {
		return new CDimensionArgument();
	}

	public static ResourceKey<Level> getDimension(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		ResourceLocation identifier = context.getArgument(name, ResourceLocation.class);
		ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, identifier);
		return context.getSource().levels().stream()
			.filter(key -> key.registry().equals(resourceKey.registry()) && key.location().equals(resourceKey.location()))
			.findAny().orElseThrow(() -> INVALID_DIMENSION_EXCEPTION.create(identifier));
	}

	@Override
	public ResourceLocation parse(final StringReader stringReader) throws CommandSyntaxException {
		return ResourceLocation.read(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return context.getSource() instanceof SharedSuggestionProvider
			? SharedSuggestionProvider.suggestResource(((SharedSuggestionProvider) context.getSource()).levels().stream().map(ResourceKey::location), builder)
			: Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
