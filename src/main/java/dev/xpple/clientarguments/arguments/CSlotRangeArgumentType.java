package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.minecraft.text.Text;
import net.minecraft.util.JsonReaderUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CSlotRangeArgumentType implements ArgumentType<SlotRange> {
    private static final Collection<String> EXAMPLES = List.of("container.*", "container.5", "weapon");
    private static final DynamicCommandExceptionType UNKNOWN_SLOT_EXCEPTION = new DynamicCommandExceptionType(slotRange -> Text.stringifiedTranslatable("slot.unknown", slotRange));

    public static CSlotRangeArgumentType slotRange() {
        return new CSlotRangeArgumentType();
    }

    public static SlotRange getSlotRange(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, SlotRange.class);
    }

    @Override
    public SlotRange parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = JsonReaderUtils.readWhileMatching(stringReader, c -> c != ' ');
        SlotRange slotRange = SlotRanges.fromName(string);
        if (slotRange == null) {
            throw UNKNOWN_SLOT_EXCEPTION.createWithContext(stringReader, string);
        }
        return slotRange;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(SlotRanges.streamNames(), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
