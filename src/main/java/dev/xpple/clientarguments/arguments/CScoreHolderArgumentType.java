package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Supplier;

public class CScoreHolderArgumentType implements ArgumentType<CScoreHolderArgumentType.ScoreHolderSupplier> {

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
	private static final SimpleCommandExceptionType EMPTY_SCORE_HOLDER_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.scoreHolder.empty"));
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

	public static ScoreHolder getCScoreHolder(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getCScoreHolders(context, name).iterator().next();
	}

	public static Collection<ScoreHolder> getCScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return getCScoreHolders(context, name, Collections::emptyList);
	}

	public static Collection<ScoreHolder> getCScoreboardScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		Scoreboard scoreboard = context.getSource().getWorld().getScoreboard();
		Objects.requireNonNull(scoreboard);
		return getCScoreHolders(context, name, scoreboard::getKnownScoreHolders);
	}

	public static Collection<ScoreHolder> getCScoreHolders(final CommandContext<FabricClientCommandSource> context, final String name, Supplier<Collection<ScoreHolder>> knownScoreHolders) throws CommandSyntaxException {
		Collection<ScoreHolder> collection = (context.getArgument(name, ScoreHolderSupplier.class)).getScoreHolders(context.getSource(), knownScoreHolders);
		if (collection.isEmpty()) {
			throw CEntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
		}
		return collection;
	}

	@Override
	public ScoreHolderSupplier parse(final StringReader stringReader) throws CommandSyntaxException {
		if (stringReader.canRead() && stringReader.peek() == '@') {
			CEntitySelectorReader entitySelectorReader = new CEntitySelectorReader(stringReader);
			CEntitySelector entitySelector = entitySelectorReader.read();
			if (!this.multiple && entitySelector.getLimit() > 1) {
				throw CEntityArgumentType.TOO_MANY_ENTITIES_EXCEPTION.create();
			} else {
				return new SelectorScoreHolderSupplier(entitySelector);
			}
		}

        int cursor = stringReader.getCursor();

        while (stringReader.canRead() && stringReader.peek() != ' ') {
            stringReader.skip();
        }

        String string = stringReader.getString().substring(cursor, stringReader.getCursor());
        if (string.equals("*")) {
            return (FabricClientCommandSource, supplier) -> {
                Collection<ScoreHolder> collection = supplier.get();
                if (collection.isEmpty()) {
                    throw EMPTY_SCORE_HOLDER_EXCEPTION.create();
                } else {
                    return collection;
                }
            };
        }

		Collection<ScoreHolder> namedHolder = Collections.singleton(ScoreHolder.fromName(string));
        if (string.startsWith("#")) {
            return (source, supplier) -> namedHolder;
        }

        try {
            UUID uuid = UUID.fromString(string);
            return (source, supplier) -> {
				Entity foundEntity = null;
				ClientWorld world = MinecraftClient.getInstance().world;
				if (world != null) {
					for (Entity entity : world.getEntities()) {
						if (entity.getUuid().equals(uuid)) {
							foundEntity = entity;
							break;
						}
					}
				}

				return foundEntity != null ? Collections.singleton(foundEntity) : namedHolder;
			};
        } catch (IllegalArgumentException e) {
            return (source, supplier) -> {
				ClientWorld world = MinecraftClient.getInstance().world;
				ClientPlayNetworkHandler networkHandler = MinecraftClient.getInstance().getNetworkHandler();
				if (world != null && networkHandler != null) {
					PlayerListEntry playerListEntry = networkHandler.getPlayerListEntry(string);
					if (playerListEntry != null) {
						UUID uuid = playerListEntry.getProfile().getId();
						PlayerEntity player = world.getPlayerByUuid(uuid);
						if (player != null) {
							return Collections.singleton(player);
						}
					}
				}

				return namedHolder;
			};
        }
    }

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}

	@FunctionalInterface
	public interface ScoreHolderSupplier {
		Collection<ScoreHolder> getScoreHolders(FabricClientCommandSource source, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException;
	}

	public static class SelectorScoreHolderSupplier implements ScoreHolderSupplier {
		private final CEntitySelector selector;

		public SelectorScoreHolderSupplier(CEntitySelector selector) {
			this.selector = selector;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Collection<ScoreHolder> getScoreHolders(FabricClientCommandSource FabricClientCommandSource, Supplier<Collection<ScoreHolder>> supplier) throws CommandSyntaxException {
			List<? extends Entity> list = this.selector.getEntities(FabricClientCommandSource);
			if (list.isEmpty()) {
				throw CEntityArgumentType.ENTITY_NOT_FOUND_EXCEPTION.create();
			} else {
				return (Collection<ScoreHolder>) (Collection<?>) list;
			}
		}
	}
}
