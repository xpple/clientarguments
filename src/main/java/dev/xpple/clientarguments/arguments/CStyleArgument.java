package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtGrammar;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.util.parsing.packrat.commands.CommandArgumentParser;
import net.minecraft.util.parsing.packrat.commands.ParserBasedArgument;

import java.util.Collection;
import java.util.List;

public class CStyleArgument extends ParserBasedArgument<Style> {
    private static final Collection<String> EXAMPLES = List.of("{\"bold\": true}\n");
    public static final DynamicCommandExceptionType INVALID_STYLE_EXCEPTION = new DynamicCommandExceptionType(style -> Component.translatableEscape("argument.style.invalid", style));
    private static final CommandArgumentParser<Tag> TAG_PARSER = SnbtGrammar.createParser(NbtOps.INSTANCE);

    private CStyleArgument(HolderLookup.Provider registries) {
        super(TAG_PARSER.withCodec(registries.createSerializationContext(NbtOps.INSTANCE), TAG_PARSER, Style.Serializer.CODEC, INVALID_STYLE_EXCEPTION));
    }

    public static CStyleArgument style(CommandBuildContext buildContext) {
        return new CStyleArgument(buildContext);
    }

    public static Style getStyle(final CommandContext<FabricClientCommandSource> context, final String style) {
        return context.getArgument(style, Style.class);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
