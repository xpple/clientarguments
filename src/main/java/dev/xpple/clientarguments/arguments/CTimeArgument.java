package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTimeArgument implements ArgumentType<Integer> {
    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType INVALID_UNIT_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.time.invalid_unit"));
    private static final Dynamic2CommandExceptionType TICK_COUNT_TOO_LOW_EXCEPTION = new Dynamic2CommandExceptionType((value, minimum) -> Component.translatableEscape("argument.time.tick_count_too_low", minimum, value));
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();
    final int minimum;

    static {
        // ticks
        UNITS.put("d", 24000);
        UNITS.put("s", 20);
        UNITS.put("t", 1);
        UNITS.put("", 1);
    }

    private CTimeArgument(int minimum) {
        this.minimum = minimum;
    }

    public static CTimeArgument time() {
        return new CTimeArgument(0);
    }

    public static CTimeArgument time(int minimum) {
        return new CTimeArgument(minimum);
    }

    public static Integer getTime(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(final StringReader stringReader) throws CommandSyntaxException {
        float f = stringReader.readFloat();
        String string = stringReader.readUnquotedString();
        int i = UNITS.getOrDefault(string, 0);
        if (i == 0) {
            throw INVALID_UNIT_EXCEPTION.createWithContext(stringReader);
        }
        int j = Math.round(f * (float) i);
        if (j < this.minimum) {
            throw TICK_COUNT_TOO_LOW_EXCEPTION.createWithContext(stringReader, j, this.minimum);
        }
        return j;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getRemaining());

        try {
            stringReader.readFloat();
        } catch (CommandSyntaxException var5) {
            return builder.buildFuture();
        }

        return SharedSuggestionProvider.suggest(UNITS.keySet(), builder.createOffset(builder.getStart() + stringReader.getCursor()));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
