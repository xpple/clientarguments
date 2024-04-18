package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CDimensionArgumentType implements ArgumentType<Identifier> {
	private static final Collection<String> EXAMPLES = Stream.of(World.OVERWORLD, World.NETHER)
		.map(key -> key.getValue().toString())
		.collect(Collectors.toList());
	private static final DynamicCommandExceptionType INVALID_DIMENSION_EXCEPTION = new DynamicCommandExceptionType(id -> Text.stringifiedTranslatable("argument.dimension.invalid", id));


	public static CDimensionArgumentType dimension() {
		return new CDimensionArgumentType();
	}

	public static RegistryKey<World> getDimension(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(name, Identifier.class);
		RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, identifier);
		return context.getSource().getWorldKeys().stream()
			.filter(key -> key.getRegistry().equals(registryKey.getRegistry()) && key.getValue().equals(registryKey.getValue()))
			.findAny().orElseThrow(() -> INVALID_DIMENSION_EXCEPTION.create(identifier));
	}

	@Override
	public Identifier parse(final StringReader stringReader) throws CommandSyntaxException {
		return Identifier.fromCommandInput(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return context.getSource() instanceof CommandSource
			? CommandSource.suggestIdentifiers(((CommandSource) context.getSource()).getWorldKeys().stream().map(RegistryKey::getValue), builder)
			: Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
