package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

import java.util.Arrays;
import java.util.Collection;

public class CComponentArgument extends ParserBasedArgument<Component> {
	private static final Collection<String> EXAMPLES = Arrays.asList("\"hello world\"", "\"\"", "\"{\"text\":\"hello world\"}", "[\"\"]");
	public static final DynamicCommandExceptionType INVALID_COMPONENT_EXCEPTION = new DynamicCommandExceptionType(text -> Component.translatableEscape("argument.component.invalid", text));
	private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(NbtOps.INSTANCE);

	private CComponentArgument(HolderLookup.Provider registries) {
		super(TAG_PARSER.withCodec(registries.createSerializationContext(NbtOps.INSTANCE), TAG_PARSER, ComponentSerialization.CODEC, INVALID_COMPONENT_EXCEPTION));
	}

	public static CComponentArgument textComponent(CommandBuildContext buildContext) {
		return new CComponentArgument(buildContext);
	}

	public static Component getComponent(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Component.class);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
