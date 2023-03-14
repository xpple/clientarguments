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
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.StringIdentifiable;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CEnumArgumentType<T extends Enum<T> & StringIdentifiable> implements ArgumentType<T> {

    private static final DynamicCommandExceptionType INVALID_ENUM_EXCEPTION = new DynamicCommandExceptionType((value) -> Text.translatable("argument.enum.invalid", value));
    private final Codec<T> codec;
    private final Supplier<T[]> valuesSupplier;

    public CEnumArgumentType(Codec<T> codec, Supplier<T[]> valuesSupplier) {
        this.codec = codec;
        this.valuesSupplier = valuesSupplier;
    }

    public T parse(final StringReader stringReader) throws CommandSyntaxException {
        String string = stringReader.readUnquotedString();
        return this.codec.parse(JsonOps.INSTANCE, new JsonPrimitive(string)).result().orElseThrow(() -> INVALID_ENUM_EXCEPTION.create(string));
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(Arrays.stream(this.valuesSupplier.get()).map(StringIdentifiable::asString).map(this::transformValueName).collect(Collectors.toList()), builder);
    }

    public Collection<String> getExamples() {
        return Arrays.stream(this.valuesSupplier.get()).map(StringIdentifiable::asString).map(this::transformValueName).limit(2L).collect(Collectors.toList());
    }

    protected String transformValueName(String name) {
        return name;
    }
}
