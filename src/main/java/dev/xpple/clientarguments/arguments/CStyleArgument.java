package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.HolderLookup;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.ParserUtils;

import java.util.Collection;
import java.util.List;

public class CStyleArgument implements ArgumentType<Style> {
    private static final Collection<String> EXAMPLES = List.of("{\"bold\": true}\n");
    public static final DynamicCommandExceptionType INVALID_STYLE_EXCEPTION = new DynamicCommandExceptionType(style -> Component.translatableEscape("argument.style.invalid", style));
    private final HolderLookup.Provider holderLookupProvider;

    private CStyleArgument(HolderLookup.Provider holderLookupProvider) {
        this.holderLookupProvider = holderLookupProvider;
    }

    public static CStyleArgument style(CommandBuildContext buildContext) {
        return new CStyleArgument(buildContext);
    }

    public static Style getStyle(final CommandContext<FabricClientCommandSource> context, final String style) {
        return context.getArgument(style, Style.class);
    }

    @Override
    public Style parse(final StringReader stringReader) throws CommandSyntaxException {
        try {
            return ParserUtils.parseJson(this.holderLookupProvider, stringReader, Style.Serializer.CODEC);
        } catch (Exception var4) {
            String string = var4.getCause() != null ? var4.getCause().getMessage() : var4.getMessage();
            throw INVALID_STYLE_EXCEPTION.createWithContext(stringReader, string);
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
