package dev.xpple.clientarguments.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// the argument type was deleted in vanilla, it's kept here for compatibility
public class CColorArgument implements ArgumentType<ChatFormatting> {
    private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
    public static final DynamicCommandExceptionType INVALID_COLOR_EXCEPTION = new DynamicCommandExceptionType(color -> Component.translatableEscape("argument.color.invalid", color));

    private static final Map<String, ChatFormatting> FORMATTING_BY_NAME = Arrays.stream(ChatFormatting.values())
        .collect(Collectors.toMap(format -> cleanName(format.name()), f -> f));

    private CColorArgument() {
    }

    public static CColorArgument color() {
        return new CColorArgument();
    }

    public static ChatFormatting getColor(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, ChatFormatting.class);
    }

    @Override
    public ChatFormatting parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        ChatFormatting formatting = getByName(string);
        if (formatting == null || isFormat(formatting)) {
            throw INVALID_COLOR_EXCEPTION.createWithContext(stringReader, string);
        }
        return formatting;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(getNames(true, false), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static String getName(ChatFormatting formatting) {
        return formatting.name().toLowerCase(Locale.ROOT);
    }

    private static String cleanName(final String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    private static @Nullable ChatFormatting getByName(final @Nullable String name) {
        return name == null ? null : FORMATTING_BY_NAME.get(cleanName(name));
    }

    private static boolean isFormat(ChatFormatting formatting) {
        return switch (formatting) {
            case OBFUSCATED, BOLD, STRIKETHROUGH, UNDERLINE, ITALIC -> true;
            default -> false;
        };
    }

    private static boolean isColor(ChatFormatting formatting) {
        return !isFormat(formatting) && formatting != ChatFormatting.RESET;
    }

    private static Collection<String> getNames(final boolean getColors, final boolean getFormats) {
        List<String> result = Lists.newArrayList();

        for (ChatFormatting format : ChatFormatting.values()) {
            if ((!isColor(format) || getColors) && (!isFormat(format) || getFormats)) {
                result.add(getName(format));
            }
        }

        return result;
    }
}
