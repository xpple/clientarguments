package dev.xpple.clientarguments.arguments;

import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CEnumArgument<T extends Enum<T> & StringRepresentable> implements ArgumentType<T> {

    private static final DynamicCommandExceptionType INVALID_ENUM_EXCEPTION = new DynamicCommandExceptionType((value) -> Component.translatable("argument.enum.invalid", value));
    private final Codec<T> codec;
    private final Supplier<T[]> valuesSupplier;

    private CEnumArgument(Codec<T> codec, Supplier<T[]> valuesSupplier) {
        this.codec = codec;
        this.valuesSupplier = valuesSupplier;
    }

    public static <T extends Enum<T> & StringRepresentable> CEnumArgument<T> enumArg(Class<T> enumClass) {
        return new CEnumArgument<>(StringRepresentable.fromEnum(enumClass::getEnumConstants), enumClass::getEnumConstants);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & StringRepresentable> T getEnum(CommandContext<FabricClientCommandSource> context, String name) {
        return (T) context.getArgument(name, Enum.class);
    }

    public T parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        return this.codec.parse(JsonOps.INSTANCE, new JsonPrimitive(string)).result().orElseThrow(() -> INVALID_ENUM_EXCEPTION.create(string));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(this.valuesSupplier.get()).map(StringRepresentable::getSerializedName).map(this::transformValueName).collect(Collectors.toList()), builder);
    }

    public Collection<String> getExamples() {
        return Arrays.stream(this.valuesSupplier.get()).map(StringRepresentable::getSerializedName).map(this::transformValueName).limit(2L).collect(Collectors.toList());
    }

    protected String transformValueName(String name) {
        return name;
    }
}
