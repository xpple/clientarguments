package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FloatRangeArgument;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.predicate.NumberRange;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;

import java.util.*;
import java.util.function.Predicate;

public class CEntitySelectorOptions {
	private static final Map<String, SelectorOption> OPTIONS = Maps.newHashMap();
	public static final DynamicCommandExceptionType UNKNOWN_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Text.stringifiedTranslatable("argument.entity.options.unknown", option));
	public static final DynamicCommandExceptionType INAPPLICABLE_OPTION_EXCEPTION = new DynamicCommandExceptionType(option -> Text.stringifiedTranslatable("argument.entity.options.inapplicable", option));
	public static final SimpleCommandExceptionType NEGATIVE_DISTANCE_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.distance.negative"));
	public static final SimpleCommandExceptionType NEGATIVE_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.level.negative"));
	public static final SimpleCommandExceptionType TOO_SMALL_LEVEL_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.entity.options.limit.toosmall"));
	public static final DynamicCommandExceptionType IRREVERSIBLE_SORT_EXCEPTION = new DynamicCommandExceptionType(sortType -> Text.stringifiedTranslatable("argument.entity.options.sort.irreversible", sortType));
	public static final DynamicCommandExceptionType INVALID_MODE_EXCEPTION = new DynamicCommandExceptionType(gameMode -> Text.stringifiedTranslatable("argument.entity.options.mode.invalid", gameMode));
	public static final DynamicCommandExceptionType INVALID_TYPE_EXCEPTION = new DynamicCommandExceptionType(entity -> Text.stringifiedTranslatable("argument.entity.options.type.invalid", entity));

	private static void putOption(String id, SelectorHandler handler, Predicate<CEntitySelectorReader> condition, Text description) {
		OPTIONS.put(id, new SelectorOption(handler, condition, description));
	}

	public static void register() {
        if (!OPTIONS.isEmpty()) {
            return;
        }
        putOption("name", reader -> {
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readString();
            if (reader.excludesName() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "name");
            }
			if (bl) {
				reader.setExcludesName(true);
			} else {
				reader.setSelectsName(true);
			}

			reader.setPredicate(entity -> entity.getName().getString().equals(string) != bl);
        }, reader -> !reader.selectsName(), Text.translatable("argument.entity.options.name.description"));
        putOption("distance", reader -> {
            int cursor = reader.getReader().getCursor();
            NumberRange.DoubleRange doubleRange = NumberRange.DoubleRange.parse(reader.getReader());
            if ((doubleRange.min().isEmpty() || !(doubleRange.min().get() < 0.0)) && (doubleRange.max().isEmpty() || !(doubleRange.max().get() < 0.0))) {
                reader.setDistance(doubleRange);
            } else {
                reader.getReader().setCursor(cursor);
                throw NEGATIVE_DISTANCE_EXCEPTION.createWithContext(reader.getReader());
            }
        }, reader -> reader.getDistance().isDummy(), Text.translatable("argument.entity.options.distance.description"));
        putOption("level", reader -> {
            int cursor = reader.getReader().getCursor();
            NumberRange.IntRange intRange = NumberRange.IntRange.parse(reader.getReader());
            if ((intRange.min().isEmpty() || intRange.min().get() >= 0) && (intRange.max().isEmpty() || intRange.max().get() >= 0)) {
                reader.setLevelRange(intRange);
                reader.setIncludesNonPlayers(false);
            } else {
                reader.getReader().setCursor(cursor);
                throw NEGATIVE_LEVEL_EXCEPTION.createWithContext(reader.getReader());
            }
        }, reader -> reader.getLevelRange().isDummy(), Text.translatable("argument.entity.options.level.description"));
        putOption("x", reader -> reader.setX(reader.getReader().readDouble()), reader -> reader.getX() == null, Text.translatable("argument.entity.options.x.description"));
        putOption("y", reader -> reader.setY(reader.getReader().readDouble()), reader -> reader.getY() == null, Text.translatable("argument.entity.options.y.description"));
        putOption("z", reader -> reader.setZ(reader.getReader().readDouble()), reader -> reader.getZ() == null, Text.translatable("argument.entity.options.z.description"));
        putOption("dx", reader -> reader.setDx(reader.getReader().readDouble()), reader -> reader.getDx() == null, Text.translatable("argument.entity.options.dx.description"));
        putOption("dy", reader -> reader.setDy(reader.getReader().readDouble()), reader -> reader.getDy() == null, Text.translatable("argument.entity.options.dy.description"));
        putOption("dz", reader -> reader.setDz(reader.getReader().readDouble()), reader -> reader.getDz() == null, Text.translatable("argument.entity.options.dz.description"));
        putOption("x_rotation", reader -> reader.setPitchRange(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getPitchRange() == FloatRangeArgument.ANY, Text.translatable("argument.entity.options.x_rotation.description"));
        putOption("y_rotation", reader -> reader.setYawRange(FloatRangeArgument.parse(reader.getReader(), true, MathHelper::wrapDegrees)), reader -> reader.getYawRange() == FloatRangeArgument.ANY, Text.translatable("argument.entity.options.y_rotation.description"));
        putOption("limit", reader -> {
            int cursor = reader.getReader().getCursor();
            int j = reader.getReader().readInt();
            if (j < 1) {
                reader.getReader().setCursor(cursor);
                throw TOO_SMALL_LEVEL_EXCEPTION.createWithContext(reader.getReader());
            }
			reader.setLimit(j);
			reader.setHasLimit(true);
        }, reader -> !reader.isSenderOnly() && !reader.hasLimit(), Text.translatable("argument.entity.options.limit.description"));
        putOption("sort", reader -> {
            int cursor = reader.getReader().getCursor();
            String string = reader.getReader().readUnquotedString();
            reader.setSuggestionProvider((builder, consumer) -> CommandSource.suggestMatching(Arrays.asList("nearest", "furthest", "random", "arbitrary"), builder));

            reader.setSorter(switch (string) {
				case "nearest" -> CEntitySelectorReader.NEAREST;
				case "furthest" -> CEntitySelectorReader.FURTHEST;
				case "random" -> CEntitySelectorReader.RANDOM;
				case "arbitrary" -> CEntitySelector.ARBITRARY;
				default -> {
					reader.getReader().setCursor(cursor);
					throw IRREVERSIBLE_SORT_EXCEPTION.createWithContext(reader.getReader(), string);
				}
			});
            reader.setHasSorter(true);
        }, reader -> !reader.isSenderOnly() && !reader.hasSorter(), Text.translatable("argument.entity.options.sort.description"));
        putOption("gamemode", reader -> {
            reader.setSuggestionProvider((builder, consumer) -> {
                String stringxx = builder.getRemaining().toLowerCase(Locale.ROOT);
                boolean blxx = !reader.excludesGameMode();
                boolean bl2 = true;
                if (!stringxx.isEmpty()) {
                    if (stringxx.charAt(0) == '!') {
                        blxx = false;
                        stringxx = stringxx.substring(1);
                    } else {
                        bl2 = false;
                    }
                }

                for (GameMode gameModexx : GameMode.values()) {
                    if (gameModexx.getName().toLowerCase(Locale.ROOT).startsWith(stringxx)) {
                        if (bl2) {
                            builder.suggest("!" + gameModexx.getName());
                        }

                        if (blxx) {
                            builder.suggest(gameModexx.getName());
                        }
                    }
                }

                return builder.buildFuture();
            });
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            if (reader.excludesGameMode() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "gamemode");
            }
			String string = reader.getReader().readUnquotedString();
			GameMode gameMode = GameMode.byName(string, null);
			if (gameMode == null) {
				reader.getReader().setCursor(cursor);
				throw INVALID_MODE_EXCEPTION.createWithContext(reader.getReader(), string);
			}
			reader.setIncludesNonPlayers(false);
			reader.setPredicate(entity -> {
				if (!(entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity)) {
					return false;
				}
                GameMode gameMode2 = MinecraftClient.getInstance().player.networkHandler.getPlayerListEntry(abstractClientPlayerEntity.getUuid()).getGameMode();
                return bl == (gameMode2 != gameMode);
			});
			if (bl) {
				reader.setExcludesGameMode(true);
			} else {
				reader.setSelectsGameMode(true);
			}
        }, reader -> !reader.selectsGameMode(), Text.translatable("argument.entity.options.gamemode.description"));
        putOption("team", reader -> {
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readUnquotedString();
            reader.setPredicate(entity -> {
                if (!(entity instanceof LivingEntity)) {
                    return false;
                }
				AbstractTeam abstractTeam = entity.getScoreboardTeam();
				String string2 = abstractTeam == null ? "" : abstractTeam.getName();
				return string2.equals(string) != bl;
            });
            if (bl) {
                reader.setExcludesTeam(true);
            } else {
                reader.setSelectsTeam(true);
            }
        }, reader -> !reader.selectsTeam(), Text.translatable("argument.entity.options.team.description"));
        putOption("type", reader -> {
            reader.setSuggestionProvider((builder, consumer) -> {
                CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder, String.valueOf('!'));
                CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(TagKey::id), builder, "!#");
                if (!reader.excludesEntityType()) {
                    CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.getIds(), builder);
                    CommandSource.suggestIdentifiers(Registries.ENTITY_TYPE.streamTags().map(TagKey::id), builder, String.valueOf('#'));
                }

                return builder.buildFuture();
            });
            int cursor = reader.getReader().getCursor();
            boolean bl = reader.readNegationCharacter();
            if (reader.excludesEntityType() && !bl) {
                reader.getReader().setCursor(cursor);
                throw INAPPLICABLE_OPTION_EXCEPTION.createWithContext(reader.getReader(), "type");
            }
			if (bl) {
				reader.setExcludesEntityType();
			}

			if (reader.readTagCharacter()) {
				TagKey<EntityType<?>> tagKey = TagKey.of(RegistryKeys.ENTITY_TYPE, Identifier.fromCommandInput(reader.getReader()));
				reader.setPredicate(entity -> entity.getType().isIn(tagKey) != bl);
			} else {
				Identifier identifier = Identifier.fromCommandInput(reader.getReader());
				EntityType<?> entityType = Registries.ENTITY_TYPE.getOrEmpty(identifier).orElseThrow(() -> {
					reader.getReader().setCursor(cursor);
					return INVALID_TYPE_EXCEPTION.createWithContext(reader.getReader(), identifier.toString());
				});
				if (Objects.equals(EntityType.PLAYER, entityType) && !bl) {
					reader.setIncludesNonPlayers(false);
				}

				reader.setPredicate(entity -> Objects.equals(entityType, entity.getType()) != bl);
				if (!bl) {
					reader.setEntityType(entityType);
				}
			}
        }, reader -> !reader.selectsEntityType(), Text.translatable("argument.entity.options.type.description"));
        putOption("tag", reader -> {
            boolean bl = reader.readNegationCharacter();
            String string = reader.getReader().readUnquotedString();
            reader.setPredicate(entity -> {
                if ("".equals(string)) {
                    return entity.getCommandTags().isEmpty() != bl;
                } else {
                    return entity.getCommandTags().contains(string) != bl;
                }
            });
        }, reader -> true, Text.translatable("argument.entity.options.tag.description"));
        putOption("nbt", reader -> {
            boolean bl = reader.readNegationCharacter();
            NbtCompound nbtCompound = new StringNbtReader(reader.getReader()).parseCompound();
            reader.setPredicate(entity -> {
                NbtCompound nbtCompound2 = entity.writeNbt(new NbtCompound());
                if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
                    ItemStack itemStack = abstractClientPlayerEntity.getInventory().getMainHandStack();
                    if (!itemStack.isEmpty()) {
                        nbtCompound2.put("SelectedItem", itemStack.encode(abstractClientPlayerEntity.getRegistryManager()));
                    }
                }

                return NbtHelper.matches(nbtCompound, nbtCompound2, true) != bl;
            });
        }, reader -> true, Text.translatable("argument.entity.options.nbt.description"));
        putOption("scores", reader -> {
            StringReader stringReader = reader.getReader();
            Map<String, NumberRange.IntRange> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
                stringReader.skipWhitespace();
                String string = stringReader.readUnquotedString();
                stringReader.skipWhitespace();
                stringReader.expect('=');
                stringReader.skipWhitespace();
                NumberRange.IntRange intRange = NumberRange.IntRange.parse(stringReader);
                map.put(string, intRange);
                stringReader.skipWhitespace();
                if (stringReader.canRead() && stringReader.peek() == ',') {
                    stringReader.skip();
                }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
                reader.setPredicate(entity -> {
                    Scoreboard scoreboard = entity.getServer().getScoreboard();

                    for (Map.Entry<String, NumberRange.IntRange> entry : map.entrySet()) {
                        ScoreboardObjective scoreboardObjective = scoreboard.getNullableObjective(entry.getKey());
                        if (scoreboardObjective == null) {
                            return false;
                        }

                        ReadableScoreboardScore readableScoreboardScore = scoreboard.getScore(entity, scoreboardObjective);
                        if (readableScoreboardScore == null) {
                            return false;
                        }

                        if (!entry.getValue().test(readableScoreboardScore.getScore())) {
                            return false;
                        }
                    }

                    return true;
                });
            }

            reader.setSelectsScores(true);
        }, reader -> !reader.selectsScores(), Text.translatable("argument.entity.options.scores.description"));
        putOption("advancements", reader -> {
            StringReader stringReader = reader.getReader();
            Map<Identifier, Predicate<AdvancementProgress>> map = Maps.newHashMap();
            stringReader.expect('{');
            stringReader.skipWhitespace();

            while (stringReader.canRead() && stringReader.peek() != '}') {
                stringReader.skipWhitespace();
                Identifier identifier = Identifier.fromCommandInput(stringReader);
                stringReader.skipWhitespace();
                stringReader.expect('=');
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
                        stringReader.expect('=');
                        stringReader.skipWhitespace();
                        boolean bl = stringReader.readBoolean();
                        map2.put(string, criterionProgress -> criterionProgress.isObtained() == bl);
                        stringReader.skipWhitespace();
                        if (stringReader.canRead() && stringReader.peek() == ',') {
                            stringReader.skip();
                        }
                    }

                    stringReader.skipWhitespace();
                    stringReader.expect('}');
                    stringReader.skipWhitespace();
                    map.put(identifier, advancementProgress -> {
                        for (Map.Entry<String, Predicate<CriterionProgress>> entry : map2.entrySet()) {
                            CriterionProgress criterionProgress = advancementProgress.getCriterionProgress(entry.getKey());
                            if (criterionProgress == null || !entry.getValue().test(criterionProgress)) {
                                return false;
                            }
                        }

                        return true;
                    });
                } else {
                    boolean bl2 = stringReader.readBoolean();
                    map.put(identifier, advancementProgress -> advancementProgress.isDone() == bl2);
                }

                stringReader.skipWhitespace();
                if (stringReader.canRead() && stringReader.peek() == ',') {
                    stringReader.skip();
                }
            }

            stringReader.expect('}');
            if (!map.isEmpty()) {
                reader.setPredicate(entity -> false);
                reader.setIncludesNonPlayers(false);
            }

            reader.setSelectsAdvancements(true);
        }, reader -> !reader.selectsAdvancements(), Text.translatable("argument.entity.options.advancements.description"));
        putOption("predicate", reader -> reader.setPredicate(entity -> false), reader -> true, Text.translatable("argument.entity.options.predicate.description"));
    }

	public static SelectorHandler getHandler(CEntitySelectorReader reader, String option, int restoreCursor) throws CommandSyntaxException {
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

	public static void suggestOptions(CEntitySelectorReader reader, SuggestionsBuilder suggestionBuilder) {
		String string = suggestionBuilder.getRemaining().toLowerCase(Locale.ROOT);

		for (Map.Entry<String, SelectorOption> entry : OPTIONS.entrySet()) {
			if (entry.getValue().condition.test(reader) && entry.getKey().toLowerCase(Locale.ROOT).startsWith(string)) {
				suggestionBuilder.suggest(entry.getKey() + "=", entry.getValue().description);
			}
		}
	}

	public interface SelectorHandler {
		void handle(CEntitySelectorReader reader) throws CommandSyntaxException;
	}

	record SelectorOption(SelectorHandler handler, Predicate<CEntitySelectorReader> condition, Text description) {
	}
}
