package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.JsonReaderUtils;

import java.util.Arrays;
import java.util.Collection;

public class CTextArgumentType implements ArgumentType<Text> {
	private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
	public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(text -> Text.stringifiedTranslatable("argument.component.invalid", text));
	private final RegistryWrapper.WrapperLookup registryLookup;

	private CTextArgumentType(RegistryWrapper.WrapperLookup registryLookup) {
		this.registryLookup = registryLookup;
	}

	public static CTextArgumentType text(CommandRegistryAccess registryAccess) {
		return new CTextArgumentType(registryAccess);
	}

	public static Text getTextArgument(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Text.class);
	}

	@Override
	public Text parse(final StringReader stringReader) throws CommandSyntaxException {
		try {
			return JsonReaderUtils.parse(this.registryLookup, stringReader, TextCodecs.CODEC);
		} catch (Exception var4) {
			String string = var4.getCause() != null ? var4.getCause().getMessage() : var4.getMessage();
			throw INVALID_COMPONENT_EXCEPTION.createWithContext(stringReader, string);
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
