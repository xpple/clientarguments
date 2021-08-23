package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.serialize.ArgumentSerializer;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.TranslatableText;

import java.util.*;
import java.util.function.Supplier;

public class CScoreHolderArgumentType implements ArgumentType<CScoreHolderArgumentType.ScoreHolder> {

	public static final SuggestionProvider<FabricClientCommandSource> SUGGESTION_PROVIDER = (context, builder) -> {
		StringReader stringReader = new StringReader(builder.getInput());
		stringReader.setCursor(builder.getStart());
		CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);

		try {
			entitySelectorReader.read();
		} catch (CommandSyntaxException ignored) {
		}

		return entitySelectorReader.listSuggestions(builder, (builderx) -> CommandSource.suggestMatching(context.getSource().getPlayerNames(), builderx));
	};

	private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
	private static final SimpleCommandExceptionType EMPTY_SCORE_HOLDER_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.scoreHolder.empty"));
	final boolean multiple;

	public CScoreHolderArgumentType(boolean multiple) {
		this.multiple = multiple;
	}

	public static CScoreHolderArgumentType scoreHolder() {
		return new CScoreHolderArgumentType(false);
	}

	public static CScoreHolderArgumentType scoreHolders() {
		return new CScoreHolderArgumentType(true);
	}

	public static String getScoreHolder(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name).iterator().next();
	}

	public static Collection<String> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getScoreHolders(context, name, Collections::emptyList);
	}

	public static Collection<String> getScoreboardScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		Objects.requireNonNull(scoreboard);
		return getScoreHolders(context, name, scoreboard::getKnownPlayers);
	}

	public static Collection<String> getScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name, Supplier<Collection<String>> players) throws CommandSyntaxException {
		Collection<String> collection = (context.getArgument(name, ScoreHolder.class)).getNames(context.getSource(), players);
		if (collection.isEmpty()) {
			throw CEntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	@Override
	public ScoreHolder parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (!this.multiple && entitySelector.getLimit() > 1) {
				throw CEntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
			} else {
				return new SelectorScoreHolder(entitySelector);
			}
		} else {
			int cursor = stringReader.getCursor();

			while (stringReader.canRead() && stringReader.peek() != ' ') {
				stringReader.skip();
			}

			String string = stringReader.getString().substring(cursor, stringReader.getCursor());
			if (string.equals("*")) {
				return (FabricClientCommandSource, supplier) -> {
					Collection<String> collection = supplier.get();
					if (collection.isEmpty()) {
						throw EMPTY_SCORE_HOLDER_EXCEPTION.create();
					} else {
						return collection;
					}
				};
			} else {
				Collection<String> collection = Collections.singleton(string);
				return (FabricClientCommandSource, supplier) -> collection;
			}
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface ScoreHolder {
		Collection<String> getNames(FabricClientCommandSource source, Supplier<Collection<String>> supplier) throws CommandSyntaxException;
	}

	public static class SelectorScoreHolder implements ScoreHolder {
		private final CEntitySelector selector;

		public SelectorScoreHolder(CEntitySelector selector) {
			this.selector = selector;
		}

		public Collection<String> getNames(FabricClientCommandSource FabricClientCommandSource, Supplier<Collection<String>> supplier) throws CommandSyntaxException {
			List<? extends Entity> list = this.selector.getEntities(FabricClientCommandSource);
			if (list.isEmpty()) {
				throw CEntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
			} else {
				List<String> list2 = Lists.newArrayList();

				for (Entity entity : list) {
					list2.add(entity.getEntityName());
				}

				return list2;
			}
		}
	}

	public static class Serializer implements ArgumentSerializer<CScoreHolderArgumentType> {
		public void toPacket(CScoreHolderArgumentType scoreHolderArgumentType, PacketByteBuf packetByteBuf) {
			byte b = 0;
			if (scoreHolderArgumentType.multiple) {
				b = (byte)(b | 1);
			}

			packetByteBuf.writeByte(b);
		}

		public CScoreHolderArgumentType fromPacket(PacketByteBuf packetByteBuf) {
			byte b = packetByteBuf.readByte();
			boolean bl = (b & 1) != 0;
			return new CScoreHolderArgumentType(bl);
		}

		public void toJson(CScoreHolderArgumentType scoreHolderArgumentType, JsonObject jsonObject) {
			jsonObject.addProperty("amount", scoreHolderArgumentType.multiple ? "multiple" : "single");
		}
	}
}
