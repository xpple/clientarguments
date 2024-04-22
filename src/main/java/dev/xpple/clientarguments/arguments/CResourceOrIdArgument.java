package dev.xpple.clientarguments.arguments;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditions;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CResourceOrIdArgument<T> implements ArgumentType<Holder<T>> {
    private static final Collection<String> EXAMPLES = List.of("foo", "foo:bar", "012", "{}", "true");
    public static final DynamicCommandExceptionType FAILED_TO_PARSE_EXCEPTION = new DynamicCommandExceptionType(argument -> Component.translatableEscape("argument.resource_or_id.failed_to_parse", argument));
    private static final SimpleCommandExceptionType INVALID_EXCEPTION = new SimpleCommandExceptionType(Component.translatable("argument.resource_or_id.invalid"));
    private final HolderLookup.Provider holderLookupProvider;
    private final boolean canLookupRegistry;
    private final Codec<Holder<T>> entryCodec;

    public CResourceOrIdArgument(CommandBuildContext buildContext, ResourceKey<Registry<T>> registry, Codec<Holder<T>> entryCodec) {
        this.holderLookupProvider = buildContext;
        this.canLookupRegistry = buildContext.lookup(registry).isPresent();
        this.entryCodec = entryCodec;
    }

    public static LootTableArgument lootTable(CommandBuildContext buildContext) {
        return new LootTableArgument(buildContext);
    }

    public static Holder<LootTable> getLootTable(final CommandContext<FabricClientCommandSource> context, final String argument) throws CommandSyntaxException {
        return getArgument(context, argument);
    }

    public static LootModifierArgument lootModifier(CommandBuildContext buildContext) {
        return new LootModifierArgument(buildContext);
    }

    public static Holder<LootItemFunction> getLootModifier(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return getArgument(context, argument);
    }

    public static LootPredicateArgument lootPredicate(CommandBuildContext buildContext) {
        return new LootPredicateArgument(buildContext);
    }

    public static Holder<LootItemCondition> getLootPredicate(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return getArgument(context, argument);
    }

    @SuppressWarnings("unchecked")
    private static <T> Holder<T> getArgument(final CommandContext<FabricClientCommandSource> context, final String argument) {
        return (Holder<T>) context.getArgument(argument, Holder.class);
    }

    @Nullable
    public Holder<T> parse(final StringReader stringReader) throws CommandSyntaxException {
        Tag nbtElement = parseAsNbt(stringReader);
        if (!this.canLookupRegistry) {
            return null;
        }
        RegistryOps<Tag> registryOps = this.holderLookupProvider.createSerializationContext(NbtOps.INSTANCE);
        return this.entryCodec.parse(registryOps, nbtElement).getOrThrow(argument -> FAILED_TO_PARSE_EXCEPTION.createWithContext(stringReader, argument));
    }

    @VisibleForTesting
    static Tag parseAsNbt(StringReader stringReader) throws CommandSyntaxException {
        int i = stringReader.getCursor();
        Tag nbtElement = new TagParser(stringReader).readValue();
        if (hasFinishedReading(stringReader)) {
            return nbtElement;
        }
        stringReader.setCursor(i);
        ResourceLocation id = ResourceLocation.read(stringReader);
        if (!hasFinishedReading(stringReader)) {
            stringReader.setCursor(i);
            throw INVALID_EXCEPTION.createWithContext(stringReader);
        }
        return StringTag.valueOf(id.toString());
    }

    private static boolean hasFinishedReading(StringReader stringReader) {
        return !stringReader.canRead() || stringReader.peek() == ' ';
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class LootPredicateArgument extends CResourceOrIdArgument<LootItemCondition> {
        protected LootPredicateArgument(CommandBuildContext buildContext) {
            super(buildContext, Registries.PREDICATE, LootItemConditions.CODEC);
        }
    }

    public static class LootModifierArgument extends CResourceOrIdArgument<LootItemFunction> {
        protected LootModifierArgument(CommandBuildContext buildContext) {
            super(buildContext, Registries.ITEM_MODIFIER, LootItemFunctions.CODEC);
        }
    }

    public static class LootTableArgument extends CResourceOrIdArgument<LootTable> {
        protected LootTableArgument(CommandBuildContext buildContext) {
            super(buildContext, Registries.LOOT_TABLE, LootTable.CODEC);
        }
    }
}
