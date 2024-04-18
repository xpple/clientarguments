package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.inventory.SlotRange;
import net.minecraft.inventory.SlotRanges;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.JsonReaderUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CItemSlotArgumentType implements ArgumentType<Integer> {
	private static final Collection<String> EXAMPLES = Arrays.asList("container.5", "weapon");
	private static final DynamicCommandExceptionType UNKNOWN_SLOT_EXCEPTION = new DynamicCommandExceptionType(name -> Text.stringifiedTranslatable("slot.unknown", name));
	private static final DynamicCommandExceptionType ONLY_SINGLE_ALLOWED_EXCEPTION = new DynamicCommandExceptionType(name -> Text.stringifiedTranslatable("slot.only_single_allowed", name));

	public static CItemSlotArgumentType itemSlot() {
		return new CItemSlotArgumentType();
	}

	public static int getItemSlot(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Integer.class);
	}

	@Override
	public Integer parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = JsonReaderUtils.readWhileMatching(stringReader, c -> c != ' ');
		SlotRange slotRange = SlotRanges.fromName(string);
		if (slotRange == null) {
			throw UNKNOWN_SLOT_EXCEPTION.createWithContext(stringReader, string);
		}
		if (slotRange.getSlotCount() != 1) {
			throw ONLY_SINGLE_ALLOWED_EXCEPTION.createWithContext(stringReader, string);
		}
		return slotRange.getSlotIds().getInt(0);
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(SlotRanges.streamSingleSlotNames(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
