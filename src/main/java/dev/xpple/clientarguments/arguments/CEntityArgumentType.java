package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CEntityArgumentType implements ArgumentType<CEntitySelector> {
	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
	public static final SimpleCommandExceptionType TOO_MANY_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.toomany"));
	public static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player.toomany"));
	public static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.player.entities"));
	public static final SimpleCommandExceptionType ENTITY_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.entity"));
	public static final SimpleCommandExceptionType PLAYER_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.notfound.player"));
	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.selector.not_allowed"));
	final boolean singleTarget;
	final boolean playersOnly;

	protected CEntityArgumentType(boolean singleTarget, boolean playersOnly) {
		this.singleTarget = singleTarget;
		this.playersOnly = playersOnly;
	}

	public static CEntityArgumentType entity() {
		return new CEntityArgumentType(true, false);
	}

	public static Entity getEntity(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getEntity(context.getSource());
	}

	public static CEntityArgumentType entities() {
		return new CEntityArgumentType(false, false);
	}

	public static Collection<? extends Entity> getEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Collection<? extends Entity> collection = getOptionalEntities(context, name);
		if (collection.isEmpty()) {
			throw ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	public static Collection<? extends Entity> getOptionalEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getEntities(context.getSource());
	}

	public static Collection<AbstractClientPlayerEntity> getOptionalPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getPlayers(context.getSource());
	}

	public static CEntityArgumentType player() {
		return new CEntityArgumentType(true, true);
	}

	public static AbstractClientPlayerEntity getPlayer(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getPlayer(context.getSource());
	}

	public static CEntityArgumentType players() {
		return new CEntityArgumentType(false, true);
	}

	public static Collection<AbstractClientPlayerEntity> getPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		List<AbstractClientPlayerEntity> list = context.getArgument(name, CEntitySelector.class).getPlayers(context.getSource());
		if (list.isEmpty()) {
			throw PLAYER_NOT_FOUND_EXCEPTION.create();
		}
		return list;
	}

	@Override
	public CEntitySelector parse(final StringReader stringReader) throws CommandSyntaxException {
		CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
		CEntitySelector entitySelector = entitySelectorReader.read();
		if (entitySelector.getLimit() > 1 && this.singleTarget) {
			if (this.playersOnly) {
				stringReader.setCursor(0);
				throw TOO_MANY_PLAYERS_EXCEPTION.createWithContext(stringReader);
			} else {
				stringReader.setCursor(0);
				throw TOO_MANY_ENTITIES_EXCEPTION.createWithContext(stringReader);
			}
		}
		if (entitySelector.includesNonPlayers() && this.playersOnly && !entitySelector.isSenderOnly()) {
			stringReader.setCursor(0);
			throw PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION.createWithContext(stringReader);
		}
		return entitySelector;
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		if (context.getSource() instanceof CommandSource commandSource) {
			StringReader stringReader = new StringReader(builder.getInput());
			stringReader.setCursor(builder.getStart());
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader, true);

			try {
				entitySelectorReader.read();
			} catch (CommandSyntaxException ignored) {
			}

			return entitySelectorReader.listSuggestions(builder, builderx -> {
				Collection<String> collection = commandSource.getPlayerNames();
				Iterable<String> iterable = this.playersOnly ? collection : Iterables.concat(collection, commandSource.getEntitySuggestions());
				CommandSource.suggestMatching(iterable, builderx);
			});
		}
		return Suggestions.empty();
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
