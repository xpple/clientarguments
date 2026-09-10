package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;
import net.minecraft.world.item.slot.RangeSlotSource;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CSlotSourceArgument implements ArgumentType<CSlotSourceArgument.Result> {
    private static final Collection<String> EXAMPLES = Util.join(CResourceOrIdArgument.EXAMPLES, CSlotsArgument.EXAMPLES);
    private final ArgumentType<Holder<SlotSource>> holderArgument;

    private CSlotSourceArgument(final CommandBuildContext context) {
        this.holderArgument = new CResourceOrIdArgument<>(context, Registries.SLOT_SOURCE, SlotSources.DIRECT_CODEC);
    }

    public static CSlotSourceArgument slotSource(final CommandBuildContext context) {
        return new CSlotSourceArgument(context);
    }

    public static CSlotSourceArgument.Result getSlotSource(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, Result.class);
    }

    public CSlotSourceArgument.Result parse(final StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        SlotRange slotRange = SlotRanges.tryRead(reader);
        if (slotRange != null) {
            return new CSlotSourceArgument.LiteralResult(slotRange);
        }

        reader.setCursor(start);
        return new CSlotSourceArgument.HolderResult(this.holderArgument.parse(reader));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> contextBuilder, final SuggestionsBuilder builder) {
        SuggestionsBuilder sub = builder.restart();
        SharedSuggestionProvider.suggest(SlotRanges.allNames(), sub);
        builder.add(sub);
        return this.holderArgument.listSuggestions(contextBuilder, builder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public record HolderResult(Holder<SlotSource> holder) implements CSlotSourceArgument.Result {
        @Override
        public SlotSource value() {
            return this.holder.value();
        }

        @Override
        public Optional<String> name() {
            return this.holder.getRegisteredNameIfPresent();
        }
    }

    public record LiteralResult(SlotRange slotRange) implements CSlotSourceArgument.Result {
        @Override
        public SlotSource value() {
            return RangeSlotSource.slotRange(this.slotRange);
        }

        @Override
        public Optional<String> name() {
            return Optional.of(this.slotRange.getSerializedName());
        }
    }

    public sealed interface Result permits CSlotSourceArgument.HolderResult, CSlotSourceArgument.LiteralResult {
        SlotSource value();

        Optional<String> name();
    }
}
