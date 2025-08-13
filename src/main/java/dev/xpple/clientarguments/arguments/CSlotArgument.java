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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CSlotArgument implements ArgumentType<Integer> {
	private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "weapon");
	private static final DynamicCommandExceptionType UNKNOWN_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("slot.unknown", name));
	private static final DynamicCommandExceptionType ONLY_SINGLE_ALLOWED_EXCEPTION = new DynamicCommandExceptionType(name -> Component.translatableEscape("slot.only_single_allowed", name));

	public static CSlotArgument itemSlot() {
		return new CSlotArgument();
	}

	public static int getItemSlot(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Integer.class);
	}

	@Override
	public Integer parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = ParserUtils.readWhile(stringReader, c -> c != ' ');
		SlotRange slotRange = SlotRanges.nameToIds(string);
		if (slotRange == null) {
			throw UNKNOWN_SLOT_EXCEPTION.createWithContext(stringReader, string);
		}
		if (slotRange.size() != 1) {
			throw ONLY_SINGLE_ALLOWED_EXCEPTION.createWithContext(stringReader, string);
		}
		return slotRange.slots().getInt(0);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return SharedSuggestionProvider.suggest(SlotRanges.singleSlotNames(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
