package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CDimensionArgumentType implements ArgumentType<Identifier> {

	private static final Collection<String> EXAMPLES = Arrays.stream(DimensionArgument.values()).map(DimensionArgument::getName).collect(Collectors.toSet());
	private static final DynamicCommandExceptionType INVALID_DIMENSION_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("cargument.dimension.invalid", id));

	public static CDimensionArgumentType dimension() {
		return new CDimensionArgumentType();
	}

	public static DimensionArgument getCDimensionArgument(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(name, Identifier.class);
		RegistryKey<World> registryKey = RegistryKey.of(Registry.WORLD_KEY, identifier);
		return Arrays.stream(DimensionArgument.values()).filter(dimension -> dimension.registryKey.equals(registryKey)).findAny().orElseThrow(() -> INVALID_DIMENSION_EXCEPTION.create(identifier));
	}

	@Override
	public Identifier parse(final StringReader stringReader) throws CommandSyntaxException {
		return Identifier.fromCommandInput(stringReader);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestIdentifiers(Arrays.stream(DimensionArgument.values()).map(dimension -> dimension.registryKey.getValue()), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	public enum DimensionArgument {
		OVERWORLD("overworld", World.OVERWORLD),
		NETHER("the_nether", World.NETHER),
		END("the_end", World.END);

		private final String name;
		private final RegistryKey<World> registryKey;

		DimensionArgument(String name, RegistryKey<World> registryKey) {
			this.name = name;
			this.registryKey = registryKey;
		}

		public String getName() {
			return this.name;
		}

		public RegistryKey<World> getRegistryKey() {
			return this.registryKey;
		}
	}
}
