package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CColorArgumentType implements ArgumentType<Formatting> {
	private static final Collection<String> EXAMPLES = Arrays.asList("red", "green");
	public static final DynamicCommandExceptionType INVALID_COLOR_EXCEPTION = new DynamicCommandExceptionType(color -> Text.stringifiedTranslatable("argument.color.invalid", color));

	private CColorArgumentType() {
	}

	public static CColorArgumentType color() {
		return new CColorArgumentType();
	}

	public static Formatting getColor(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Formatting.class);
	}

	@Override
	public Formatting parse(final StringReader stringReader) throws CommandSyntaxException {
		String string = stringReader.readUnquotedString();
		Formatting formatting = Formatting.byName(string);
        if (formatting == null || formatting.isModifier()) {
			throw INVALID_COLOR_EXCEPTION.createWithContext(stringReader, string);
		}
		return formatting;
    }

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return CommandSource.suggestMatching(Formatting.getNames(true, false), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
