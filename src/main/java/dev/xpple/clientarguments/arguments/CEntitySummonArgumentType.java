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
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CEntitySummonArgumentType implements ArgumentType<Identifier> {

	private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
	public static final DynamicCommandExceptionType NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("entity.notFound", id));

	public static CEntitySummonArgumentType entitySummon() {
		return new CEntitySummonArgumentType();
	}

	public static Identifier getCEntitySummon(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return validate(context.getArgument(name, Identifier.class));
	}

	private static Identifier validate(Identifier id) throws CommandSyntaxException {
		Registry.ENTITY_TYPE.getOrEmpty(id).filter(EntityType::isSummonable).orElseThrow(() -> NOT_FOUND_EXCEPTION.create(id));
		return id;
	}

	@Override
	public Identifier parse(final StringReader stringReader) throws CommandSyntaxException {
		return validate(Identifier.fromCommandInput(stringReader));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		Stream<Identifier> entities = Registry.ENTITY_TYPE.getIds().stream()
				.filter(identifier -> Registry.ENTITY_TYPE.get(identifier).isSummonable());

		return CommandSource.suggestIdentifiers(entities, builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
