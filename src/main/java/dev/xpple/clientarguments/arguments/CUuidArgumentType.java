package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.command.argument.UuidArgumentType;
import net.minecraft.text.TranslatableText;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CUuidArgumentType implements ArgumentType<UUID> {

    public static final SimpleCommandExceptionType INVALID_UUID = new SimpleCommandExceptionType(new TranslatableText("cargument.uuid.invalid"));
    private static final Collection<String> EXAMPLES = List.of("dd12be42-52a9-4a91-a8a1-11c01849e498");
    private static final Pattern VALID_CHARACTERS = Pattern.compile("^([-A-Fa-f0-9]+)");

    public static UuidArgumentType uuid() {
        return new UuidArgumentType();
    }

    public static UUID getUuid(final CommandContext<FabricClientCommandSource> context, final String name) {
        return context.getArgument(name, UUID.class);
    }

    @Override
    public UUID parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.getRemaining();
        Matcher matcher = VALID_CHARACTERS.matcher(string);
        if (matcher.find()) {
            String match = matcher.group(1);

            try {
                UUID uuid = UUID.fromString(match);
                stringReader.setCursor(stringReader.getCursor() + match.length());
                return uuid;
            } catch (IllegalArgumentException ignored) {
            }
        }

        throw INVALID_UUID.create();
    }

    @Override
    public String toString() {
        return "uuid()";
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
