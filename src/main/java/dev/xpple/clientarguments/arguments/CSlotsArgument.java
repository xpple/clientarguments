package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.ParserUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CSlotsArgument implements ArgumentType<SlotRange> {
    private static final Collection<String> EXAMPLES = List.of("container.*", "container.5", "weapon");
    private static final DynamicCommandExceptionType UNKNOWN_SLOT_EXCEPTION = new DynamicCommandExceptionType(slotRange -> Component.translatableEscape("slot.unknown", slotRange));

    public static CSlotsArgument slots() {
        return new CSlotsArgument();
    }

    public static SlotRange getSlots(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, SlotRange.class);
    }

    @Override
    public SlotRange parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = ParserUtils.readWhile(stringReader, c -> c != ' ');
        SlotRange slotRange = SlotRanges.nameToIds(string);
        if (slotRange == null) {
            throw UNKNOWN_SLOT_EXCEPTION.createWithContext(stringReader, string);
        }
        return slotRange;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder suggestionsBuilder) {
        return SharedSuggestionProvider.suggest(SlotRanges.allNames(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
