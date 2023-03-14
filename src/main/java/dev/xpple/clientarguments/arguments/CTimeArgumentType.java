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
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTimeArgumentType implements ArgumentType<Integer> {

    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType INVALID_UNIT_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.time.invalid_unit"));
    private static final Dynamic2CommandExceptionType TICK_COUNT_TOO_LOW_EXCEPTION = new Dynamic2CommandExceptionType((value, minimum) -> Text.translatable("argument.time.tick_count_too_low", minimum, value));
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();
    final int minimum;

    static {
        // ticks
        UNITS.put("d", 24000);
        UNITS.put("s", 20);
        UNITS.put("t", 1);
        UNITS.put("", 1);
    }

    private CTimeArgumentType(int minimum) {
        this.minimum = minimum;
    }

    public static CTimeArgumentType time() {
        return new CTimeArgumentType(0);
    }

    public static CTimeArgumentType time(int minimum) {
        return new CTimeArgumentType(minimum);
    }

    public static Integer getCTime(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, Integer.class);
    }

    @Override
    public Integer parse(final StringReader stringReader) throws CommandSyntaxException {
        float time = stringReader.readFloat();
        String unit = stringReader.readUnquotedString();
        int unitFactor = UNITS.getOrDefault(unit, 0);
        if (unitFactor == 0) {
            throw INVALID_UNIT_EXCEPTION.create();
        }
        int ticks = Math.round(time * (float) unitFactor);
        if (ticks < this.minimum) {
            throw TICK_COUNT_TOO_LOW_EXCEPTION.create(ticks, this.minimum);
        }
        return ticks;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        StringReader stringReader = new StringReader(builder.getRemaining());

        try {
            stringReader.readFloat();
        } catch (CommandSyntaxException var5) {
            return builder.buildFuture();
        }

        return CommandSource.suggestMatching(UNITS.keySet(), builder.createOffset(builder.getStart() + stringReader.getCursor()));
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
