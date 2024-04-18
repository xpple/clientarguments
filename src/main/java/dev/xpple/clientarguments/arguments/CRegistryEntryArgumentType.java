package dev.xpple.clientarguments.arguments;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionTypes;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CRegistryEntryArgumentType<T> implements ArgumentType<RegistryEntry<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType FAILED_TO_PARSE_EXCEPTION = new DynamicCommandExceptionType(argument -> Text.stringifiedTranslatable("argument.resource_or_id.failed_to_parse", argument));
    private static final SimpleCommandExceptionType INVALID_EXCEPTION = new SimpleCommandExceptionType(Text.translatable("argument.resource_or_id.invalid"));
    private final RegistryWrapper.WrapperLookup registryLookup;
    private final boolean canLookupRegistry;
    private final Codec<RegistryEntry<T>> entryCodec;

    public CRegistryEntryArgumentType(CommandRegistryAccess registryAccess, RegistryKey<Registry<T>> registry, Codec<RegistryEntry<T>> entryCodec) {
        this.registryLookup = registryAccess;
        this.canLookupRegistry = registryAccess.getOptionalWrapper(registry).isPresent();
        this.entryCodec = entryCodec;
    }

    public static CRegistryEntryArgumentType.LootTableArgumentType lootTable(CommandRegistryAccess registryAccess) {
        return new CRegistryEntryArgumentType.LootTableArgumentType(registryAccess);
    }

    public static RegistryEntry<LootTable> getLootTable(final CommandContext<FabricClientCommandSource> context, final String argument) throws CommandSyntaxException {
        return getArgument(context, argument);
    }

    public static CRegistryEntryArgumentType.LootFunctionArgumentType lootFunction(CommandRegistryAccess registryAccess) {
        return new CRegistryEntryArgumentType.LootFunctionArgumentType(registryAccess);
    }

    public static RegistryEntry<LootFunction> getLootFunction(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return getArgument(context, argument);
    }

    public static CRegistryEntryArgumentType.LootConditionArgumentType lootCondition(CommandRegistryAccess registryAccess) {
        return new CRegistryEntryArgumentType.LootConditionArgumentType(registryAccess);
    }

    public static RegistryEntry<LootCondition> getLootCondition(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return getArgument(context, argument);
    }

    @SuppressWarnings("unchecked")
    private static <T> RegistryEntry<T> getArgument(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return (RegistryEntry<T>) context.getArgument(argument, RegistryEntry.class);
    }

    @Nullable
    public RegistryEntry<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        NbtElement nbtElement = parseAsNbt(stringReader);
        if (!this.canLookupRegistry) {
            return null;
        }
        RegistryOps<NbtElement> registryOps = this.registryLookup.getOps(NbtOps.INSTANCE);
        return this.entryCodec.parse(registryOps, nbtElement).getOrThrow(argument -> FAILED_TO_PARSE_EXCEPTION.createWithContext(stringReader, argument));
    }

    @VisibleForTesting
    static NbtElement parseAsNbt(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        NbtElement nbtElement = new StringNbtReader(stringReader).parseElement();
        if (hasFinishedReading(stringReader)) {
            return nbtElement;
        }
        stringReader.setCursor(i);
        Identifier identifier = Identifier.fromCommandInput(stringReader);
        if (!hasFinishedReading(stringReader)) {
            stringReader.setCursor(i);
            throw INVALID_EXCEPTION.createWithContext(stringReader);
        }
        return NbtString.of(identifier.toString());
    }

    private static boolean hasFinishedReading(StringReader stringReader) {
        return !stringReader.canRead() || stringReader.peek() == ' ';
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class LootConditionArgumentType extends CRegistryEntryArgumentType<LootCondition> {
        protected LootConditionArgumentType(CommandRegistryAccess registryAccess) {
            super(registryAccess, RegistryKeys.PREDICATE, LootConditionTypes.ENTRY_CODEC);
        }
    }

    public static class LootFunctionArgumentType extends CRegistryEntryArgumentType<LootFunction> {
        protected LootFunctionArgumentType(CommandRegistryAccess registryAccess) {
            super(registryAccess, RegistryKeys.ITEM_MODIFIER, LootFunctionTypes.ENTRY_CODEC);
        }
    }

    public static class LootTableArgumentType extends CRegistryEntryArgumentType<LootTable> {
        protected LootTableArgumentType(CommandRegistryAccess registryAccess) {
            super(registryAccess, RegistryKeys.LOOT_TABLE, LootTable.ENTRY_CODEC);
        }
    }
}
