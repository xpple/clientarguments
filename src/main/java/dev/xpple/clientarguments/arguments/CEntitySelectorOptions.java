package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.advancements.critereon.WrappedMinMaxBounds;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects; 
import java.util.function.Predicate;

public class CEntitySelectorOptions {
    private static final Logger LOGGER = LogUtils.getLogger();
	private static final Map<String, SelectorOption> OPTIONS = Maps.newHashMap();
	public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Component.translatableEscape("argument.entity.options.unknown", option));
	public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Component.translatableEscape("argument.entity.options.inapplicable", option));
	public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.options.distance.negative"));
	public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.options.level.negative"));
	public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.entity.options.limit.toosmall"));
	public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(sortType -> Component.translatableEscape("argument.entity.options.sort.irreversible", sortType));
	public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(gameMode -> Component.translatableEscape("argument.entity.options.mode.invalid", gameMode));
	public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(entity -> Component.translatableEscape("argument.entity.options.type.invalid", entity));

	private static void putOption(String id, SelectorHandler handler, Predicate<CEntitySelectorParser> condition, Component description) {
		OPTIONS.put(id, new SelectorOption(handler, condition, description));
	}

	public static void register() {
        if (!OPTIONS.isEmpty()) {
            return;
        }
        putOption("name", reader -> {
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.shouldInvertValue();
            String string = reader.getReader().readString();
            if (reader.hasNameNotEquals() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "name");
            }
			if (bl) {
				reader.setHasNameNotEquals(true);
			} else {
				reader.setHasNameEquals(true);
			}

			reader.addPredicate(entity -> entity.getName().getString().equals(string) != bl);
        }, reader -> !reader.hasNameEquals(), Component.translatable("argument.entity.options.name.description"));
        putOption("distance", reader -> {
            int cursor = reader.getReader().getCursor();
            MinMaxBounds.Doubles doubleRange = MinMaxBounds.Doubles.fromReader(reader.getReader());
            if ((doubleRange.min().isEmpty() || !(doubleRange.min().get() < 0.0)) && (doubleRange.max().isEmpty() || !(doubleRange.max().get() < 0.0))) {
                reader.setDistance(doubleRange);
            } else {
                reader.getReader().setCursor(cursor);
                throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(reader.getReader());
            }
        }, reader -> reader.getDistance().isAny(), Component.translatable("argument.entity.options.distance.description"));
        putOption("level", reader -> {
            int cursor = reader.getReader().getCursor();
            MinMaxBounds.Ints intRange = MinMaxBounds.Ints.fromReader(reader.getReader());
            if ((intRange.min().isEmpty() || intRange.min().get() >= 0) && (intRange.max().isEmpty() || intRange.max().get() >= 0)) {
                reader.setLevel(intRange);
                reader.setIncludesEntities(false);
            } else {
                reader.getReader().setCursor(cursor);
                throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(reader.getReader());
            }
        }, reader -> reader.getLevel().isAny(), Component.translatable("argument.entity.options.level.description"));
        putOption("x", reader -> reader.setX(reader.getReader().readDouble()), reader -> reader.getX() == null, Component.translatable("argument.entity.options.x.description"));
        putOption("y", reader -> reader.setY(reader.getReader().readDouble()), reader -> reader.getY() == null, Component.translatable("argument.entity.options.y.description"));
        putOption("z", reader -> reader.setZ(reader.getReader().readDouble()), reader -> reader.getZ() == null, Component.translatable("argument.entity.options.z.description"));
        putOption("dx", reader -> reader.setDeltaX(reader.getReader().readDouble()), reader -> reader.getDeltaX() == null, Component.translatable("argument.entity.options.dx.description"));
        putOption("dy", reader -> reader.setDeltaY(reader.getReader().readDouble()), reader -> reader.getDeltaY() == null, Component.translatable("argument.entity.options.dy.description"));
        putOption("dz", reader -> reader.setDeltaZ(reader.getReader().readDouble()), reader -> reader.getDeltaZ() == null, Component.translatable("argument.entity.options.dz.description"));
        putOption("x_rotation", reader -> reader.setRotX(WrappedMinMaxBounds.fromReader(reader.getReader(), true, Mth::wrapDegrees)), reader -> reader.getRotX() == WrappedMinMaxBounds.ANY, Component.translatable("argument.entity.options.x_rotation.description"));
        putOption("y_rotation", reader -> reader.setRotY(WrappedMinMaxBounds.fromReader(reader.getReader(), true, Mth::wrapDegrees)), reader -> reader.getRotY() == WrappedMinMaxBounds.ANY, Component.translatable("argument.entity.options.y_rotation.description"));
        putOption("limit", reader -> {
            int cursor = reader.getReader().getCursor();
            int j = reader.getReader().readInt();
            if (j < 1) {
                reader.getReader().setCursor(cursor);
                throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(reader.getReader());
            }
			reader.setMaxResults(j);
			reader.setLimited(true);
        }, reader -> !reader.isCurrentEntity() && !reader.isLimited(), Component.translatable("argument.entity.options.limit.description"));
        putOption("sort", reader -> {
            int cursor = reader.getReader().getCursor();
            String string = reader.getReader().readUnquotedString();
            reader.setSuggestions((builder, consumer) -> SharedSuggestionProvider.suggest(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder));

            reader.setOrder(switch (string) {
				case "nearest" -> CEntitySelectorParser.ORDER_NEAREST;
				case "furthest" -> CEntitySelectorParser.ORDER_FURTHEST;
				case "random" -> CEntitySelectorParser.ORDER_RANDOM;
				case "arbitrary" -> CEntitySelector.ORDER_ARBITRARY;
				default -> {
					reader.getReader().setCursor(cursor);
					throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(reader.getReader(), string);
				}
			});
            reader.setSorted(true);
        }, reader -> !reader.isCurrentEntity() && !reader.isSorted(), Component.translatable("argument.entity.options.sort.description"));
        putOption("gamemode", reader -> {
            reader.setSuggestions((builder, consumer) -> {
                String stringxx = builder.getRemaining().toLowerCase(Locale.ROOT);
                boolean blxx = !reader.hasGamemodeNotEquals();
                boolean bl2 = true;
                if (!stringxx.isEmpty()) {
                    if (stringxx.charAt(0) == CEntitySelectorParser.SYNTAX_NOT) {
                        blxx = false;
                        stringxx = stringxx.substring(1);
                    } else {
                        bl2 = false;
                    }
                }

                for (GameType gameType : GameType.values()) {
                    if (gameType.getName().toLowerCase(Locale.ROOT).startsWith(stringxx)) {
                        if (bl2) {
                            builder.suggest("!" + gameType.getName());
                        }

                        if (blxx) {
                            builder.suggest(gameType.getName());
                        }
                    }
                }

                return builder.buildFuture();
            });
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.shouldInvertValue();
            if (reader.hasGamemodeNotEquals() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "gamemode");
            }
			String string = reader.getReader().readUnquotedString();
			GameType gameType = GameType.byName(string, null);
			if (gameType == null) {
				reader.getReader().setCursor(cursor);
				throw INVALID_MODE_EXCEPTION.createWithContext(reader.getReader(), string);
			}
			reader.setIncludesEntities(false);
			reader.addPredicate(entity -> {
				if (!(entity instanceof AbstractClientPlayer abstractClientPlayerEntity)) {
					return false;
				}
                PlayerInfo playerListEntry = Minecraft.getInstance().player.connection.getPlayerInfo(abstractClientPlayerEntity.getUUID());
                if (playerListEntry == null) {
                    return false;
                }
                GameType gameType2 = playerListEntry.getGameMode();
                return bl == (gameType2 != gameType);
			});
			if (bl) {
				reader.setHasGamemodeNotEquals(true);
			} else {
				reader.setHasGamemodeEquals(true);
			}
        }, reader -> !reader.hasGamemodeEquals(), Component.translatable("argument.entity.options.gamemode.description"));
        putOption("team", reader -> {
            boolean bl = reader.shouldInvertValue();
            String string = reader.getReader().readUnquotedString();
            reader.addPredicate(entity -> {
                if (!(entity instanceof LivingEntity)) {
                    return false;
                }
				Team team = entity.getTeam();
				String string2 = team == null ? "" : team.getName();
				return string2.equals(string) != bl;
            });
            if (bl) {
                reader.setHasTeamNotEquals(true);
            } else {
                reader.setHasTeamEquals(true);
            }
        }, reader -> !reader.hasTeamEquals(), Component.translatable("argument.entity.options.team.description"));
        putOption("type", reader -> {
            reader.setSuggestions((builder, consumer) -> {
                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder, String.valueOf(CEntitySelectorParser.SYNTAX_NOT));
                SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTags().map(named -> named.key().location()), builder, "!#");
                if (!reader.isTypeLimitedInversely()) {
                    SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.keySet(), builder);
                    SharedSuggestionProvider.suggestResource(BuiltInRegistries.ENTITY_TYPE.getTags().map(named -> named.key().location()), builder, String.valueOf(CEntitySelectorParser.SYNTAX_TAG));
                }

                return builder.buildFuture();
            });
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.shouldInvertValue();
            if (reader.isTypeLimitedInversely() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "type");
            }
			if (bl) {
				reader.setTypeLimitedInversely();
			}

			if (reader.isTag()) {
				TagKey<EntityType<?>> tagKey = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.read(reader.getReader()));
				reader.addPredicate(entity -> entity.getType().is(tagKey) != bl);
			} else {
				ResourceLocation resourceLocation = ResourceLocation.read(reader.getReader());
				EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(resourceLocation).orElseThrow(() -> {
					reader.getReader().setCursor(cursor);
					return INVALID_TYPE_EXCEPTION.createWithContext(reader.getReader(), resourceLocation.toString());
				});
				if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
					reader.setIncludesEntities(false);
				}

				reader.addPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
				if (!bl) {
					reader.limitToType(entityType);
				}
			}
        }, reader -> !reader.isTypeLimited(), Component.translatable("argument.entity.options.type.description"));
        putOption("tag", reader -> {
            boolean bl = reader.shouldInvertValue();
            String string = reader.getReader().readUnquotedString();
            reader.addPredicate(entity -> {
                if ("".equals(string)) {
                    return entity.getTags().isEmpty() != bl;
                } else {
                    return entity.getTags().contains(string) != bl;
                }
            });
        }, reader -> true, Component.translatable("argument.entity.options.tag.description"));
        putOption("nbt", reader -> {
            boolean bl = reader.shouldInvertValue();
            CompoundTag compoundTag = TagParser.parseCompoundAsArgument(reader.getReader());
            reader.addPredicate(entity -> {
                try (ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
                    TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, entity.registryAccess());
                    entity.saveWithoutId(tagValueOutput);
                    if (entity instanceof AbstractClientPlayer abstractClientPlayer) {
                        ItemStack itemStack = abstractClientPlayer.getInventory().getSelectedItem();
                        if (!itemStack.isEmpty()) {
                            tagValueOutput.store("SelectedItem", ItemStack.CODEC, itemStack);
                        }
                    }

                    return NbtUtils.compareNbt(compoundTag, tagValueOutput.buildResult(), true) != bl;
                }
            });
        }, reader -> true, Component.translatable("argument.entity.options.nbt.description"));
        putOption("scores", reader -> {
            StringReader stringReader = reader.getReader();
            Map<String, MinMaxBounds.Ints> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
                stringReader.skipWhitespace();
                String string = stringReader.readUnquotedString();
                stringReader.skipWhitespace();
                stringReader.expect(CEntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                stringReader.skipWhitespace();
                MinMaxBounds.Ints intRange = MinMaxBounds.Ints.fromReader(stringReader);
                map.put(string, intRange);
                stringReader.skipWhitespace();
                if (stringReader.canRead() && stringReader.peek() == ',') {
                    stringReader.skip();
                }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
                reader.addPredicate(entity -> {
                    try (Level level = entity.level()) {
                        Scoreboard scoreboard = level.getScoreboard();

                        for (Map.Entry<String, MinMaxBounds.Ints> entry : map.entrySet()) {
                            Objective objective = scoreboard.getObjective(entry.getKey());
                            if (objective == null) {
                                return false;
                            }

                            ReadOnlyScoreInfo readOnlyScoreInfo = scoreboard.getPlayerScoreInfo(entity, objective);
                            if (readOnlyScoreInfo == null) {
                                return false;
                            }

                            if (!entry.getValue().matches(readOnlyScoreInfo.value())) {
                                return false;
                            }
                        }

                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                });
            }

            reader.setHasScores(true);
        }, reader -> !reader.hasScores(), Component.translatable("argument.entity.options.scores.description"));
        putOption("advancements", reader -> {
            StringReader stringReader = reader.getReader();
            Map<ResourceLocation, Predicate<AdvancementProgress>> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
                stringReader.skipWhitespace();
                ResourceLocation resourceLocation = ResourceLocation.read(stringReader);
                stringReader.skipWhitespace();
                stringReader.expect(CEntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                stringReader.skipWhitespace();
                if (stringReader.canRead() && stringReader.peek() == '{') {
                    Map<String, Predicate<CriterionProgress>> map2 = Maps.newHashMap();
                    stringReader.skipWhitespace();
                    stringReader.expect('{');
                    stringReader.skipWhitespace();

                    while (stringReader.canRead() && stringReader.peek() != '}') {
                        stringReader.skipWhitespace();
                        String string = stringReader.readUnquotedString();
                        stringReader.skipWhitespace();
                        stringReader.expect(CEntitySelectorParser.SYNTAX_OPTIONS_KEY_VALUE_SEPARATOR);
                        stringReader.skipWhitespace();
                        boolean bl = stringReader.readBoolean();
                        map2.put(string, criterionProgress -> criterionProgress.isDone() == bl);
                        stringReader.skipWhitespace();
                        if (stringReader.canRead() && stringReader.peek() == ',') {
                            stringReader.skip();
                        }
                    }

                    stringReader.skipWhitespace();
                    stringReader.expect('}');
                    stringReader.skipWhitespace();
                    map.put(resourceLocation, advancementProgress -> {
                        for (Map.Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                            CriterionProgress criterionProgress = advancementProgress.getCriterion(entry.getKey());
                            if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                                return false;
                            }
                        }

                        return true;
                    });
                } else {
                    boolean bl2 = stringReader.readBoolean();
                    map.put(resourceLocation, advancementProgress -> advancementProgress.isDone() == bl2);
                }

                stringReader.skipWhitespace();
                if (stringReader.canRead() && stringReader.peek() == ',') {
                    stringReader.skip();
                }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
                reader.addPredicate(entity -> false);
                reader.setIncludesEntities(false);
            }

            reader.setHasAdvancements(true);
        }, reader -> !reader.hasAdvancements(), Component.translatable("argument.entity.options.advancements.description"));
        putOption("predicate", reader -> reader.addPredicate(entity -> false), reader -> true, Component.translatable("argument.entity.options.predicate.description"));
    }

	public static SelectorHandler getHandler(CEntitySelectorParser reader, String option, int restoreCursor) throws CommandSyntaxException {
		SelectorOption selectorOption = OPTIONS.get(option);
		if (selectorOption != null) {
			if (selectorOption.condition.test(reader)) {
				return selectorOption.handler;
			}
			throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
		}
		reader.getReader().setCursor(restoreCursor);
		throw UNKNOWN_OPTION_EXCEPTION.createWithContext(reader.getReader(), option);
	}

	public static void suggestOptions(CEntitySelectorParser reader, SuggestionsBuilder suggestionBuilder) {
		String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

		for (Map.Entry<String, SelectorOption> entry : OPTIONS.entrySet()) {
			if (entry.getValue().condition.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
				suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
			}
		}
	}

	public interface SelectorHandler {
		void handle(CEntitySelectorParser reader) throws CommandSyntaxException;
	}

	record SelectorOption(SelectorHandler handler, Predicate<CEntitySelectorParser> condition, Component description) {
	}
}
