package dev.xpple.clientarguments.arguments;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;

public class CTextArgumentType implements ArgumentType<Text> {

	private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
	public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(text -> Text.translatable("cargument.component.invalid", text));

	public static Text getCTextArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Text.class);
	}

	public static CTextArgumentType text() {
		return new CTextArgumentType();
	}

	@Override
	public Text parse(final StringReader stringReader) throws CommandSyntaxException {
		try {
			Text text = Text.Serializer.fromJson(stringReader);
			if (text == null) {
				throw INVALID_COMPONENT_EXCEPTION.createWithContext(stringReader, "empty");
			}
			return text;
		} catch (JsonParseException var4) {
			String string = var4.getCause() != null ? var4.getCause().getMessage() : var4.getMessage();
			throw INVALID_COMPONENT_EXCEPTION.createWithContext(stringReader, string);
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
