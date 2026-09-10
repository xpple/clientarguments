package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.SwingAnimationType;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class CSwingAnimationArgument implements ArgumentType<SwingAnimationType> {
    private static final Collection<String> EXAMPLES = Stream.of(SwingAnimationType.WHACK, SwingAnimationType.STAB)
        .map(SwingAnimationType::getSerializedName)
        .toList();
    private static final SwingAnimationType[] VALUES = SwingAnimationType.values();
    private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(
        value -> Component.translatableEscape("argument.swing_animation.invalid", value)
    );

    public SwingAnimationType parse(final StringReader reader) throws CommandSyntaxException {
        String swingAnimationString = reader.readUnquotedString();
        SwingAnimationType swingAnimation = SwingAnimationType.byName(swingAnimationString, null);
        if (swingAnimation == null) {
            throw ERROR_INVALID.createWithContext(reader, swingAnimationString);
        } else {
            return swingAnimation;
        }
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return context.getSource() instanceof SharedSuggestionProvider
            ? SharedSuggestionProvider.suggest(Arrays.stream(VALUES).map(SwingAnimationType::getSerializedName), builder)
            : Suggestions.empty();
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static CSwingAnimationArgument swingAnimationType() {
        return new CSwingAnimationArgument();
    }

    public static SwingAnimationType getSwingAnimationType(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
        return context.getArgument(name, SwingAnimationType.class);
    }
}
