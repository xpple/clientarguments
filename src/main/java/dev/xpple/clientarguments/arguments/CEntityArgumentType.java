package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CEntityArgumentType implements ArgumentType<CEntitySelector> {

	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
	public static final SimpleCommandExceptionType TOO_MANY_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.toomany"));
	public static final SimpleCommandExceptionType TOO_MANY_PLAYERS_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.player.toomany"));
	public static final SimpleCommandExceptionType PLAYER_SELECTOR_HAS_ENTITIES_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.player.entities"));
	public static final SimpleCommandExceptionType ENTITY_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.notfound.entity"));
	public static final SimpleCommandExceptionType PLAYER_NOT_FOUND_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.notfound.player"));
	public static final SimpleCommandExceptionType NOT_ALLOWED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.entity.selector.not_allowed"));
	final boolean singleTarget;
	final boolean playersOnly;

	protected CEntityArgumentType(boolean singleTarget, boolean playersOnly) {
		this.singleTarget = singleTarget;
		this.playersOnly = playersOnly;
	}

	public static CEntityArgumentType entity() {
		return new CEntityArgumentType(true, false);
	}

	public static Entity getCEntity(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getEntity(context.getSource());
	}

	public static CEntityArgumentType entities() {
		return new CEntityArgumentType(false, false);
	}

	public static CEntitySelector getCEntitySelector(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, CEntitySelector.class);
	}

	public static Collection<? extends Entity> getCEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Collection<? extends Entity> collection = getCOptionalEntities(context, name);
		if (collection.isEmpty()) {
			throw ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	public static Collection<? extends Entity> getCOptionalEntities(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getEntities(context.getSource());
	}

	public static Collection<AbstractClientPlayerEntity> getCOptionalPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getPlayers(context.getSource());
	}

	public static CEntityArgumentType player() {
		return new CEntityArgumentType(true, true);
	}

	public static AbstractClientPlayerEntity getCPlayer(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return context.getArgument(name, CEntitySelector.class).getPlayer(context.getSource());
	}

	public static CEntityArgumentType players() {
		return new CEntityArgumentType(false, true);
	}

	public static Collection<AbstractClientPlayerEntity> getCPlayers(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
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
		} else if (entitySelector.includesNonPlayers() && this.playersOnly && !entitySelector.isSenderOnly()) {
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

			return entitySelectorReader.listSuggestions(builder, (builderx) -> {
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

	public static class Serializer implements ArgumentSerializer<CEntityArgumentType> {
		public void toPacket(CEntityArgumentType entityArgumentType, PacketByteBuf packetByteBuf) {
			byte b = 0;
			if (entityArgumentType.singleTarget) {
				b = (byte) (b | 1);
			}

			if (entityArgumentType.playersOnly) {
				b = (byte) (b | 2);
			}

			packetByteBuf.writeByte(b);
		}

		public CEntityArgumentType fromPacket(PacketByteBuf packetByteBuf) {
			byte b = packetByteBuf.readByte();
			return new CEntityArgumentType((b & 1) != 0, (b & 2) != 0);
		}

		public void toJson(CEntityArgumentType entityArgumentType, JsonObject jsonObject) {
			jsonObject.addProperty("amount", entityArgumentType.singleTarget ? "single" : "multiple");
			jsonObject.addProperty("type", entityArgumentType.playersOnly ? "players" : "entities");
		}
	}
}
