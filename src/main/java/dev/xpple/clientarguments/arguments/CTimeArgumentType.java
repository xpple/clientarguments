package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.TimeArgumentType;
import net.minecraft.text.TranslatableText;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CTimeArgumentType implements ArgumentType<Integer> {

    private static final Collection<String> EXAMPLES = Arrays.asList("0d", "0s", "0t", "0");
    private static final SimpleCommandExceptionType INVALID_UNIT_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("cargument.time.invalid_unit"));
    private static final DynamicCommandExceptionType INVALID_COUNT_EXCEPTION = new DynamicCommandExceptionType(arg -> new TranslatableText("cargument.time.invalid_tick_count", arg));
    private static final Object2IntMap<String> UNITS = new Object2IntOpenHashMap<>();

    static {
        // ticks
        UNITS.put("d", 24000);
        UNITS.put("s", 20);
        UNITS.put("t", 1);
        UNITS.put("", 1);
    }

    public static TimeArgumentType time() {
        return new TimeArgumentType();
    }

    public static Integer getTime(final CommandContext<FabricClientCommandSource> context, final String name) {
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
        if (ticks < 0) {
            throw INVALID_COUNT_EXCEPTION.create(ticks);
        }
        return ticks;
    }

    @Override
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
