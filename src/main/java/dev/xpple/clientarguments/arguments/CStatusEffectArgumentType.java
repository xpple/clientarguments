package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.effect.StatusEffect;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class CStatusEffectArgumentType implements ArgumentType<StatusEffect> {

	private static final Collection<String> EXAMPLES = Arrays.asList("spooky", "effect");
	public static final DynamicCommandExceptionType INVALID_EFFECT_EXCEPTION = new DynamicCommandExceptionType((id) -> Text.translatable("effect.effectNotFound", id));

	public static CStatusEffectArgumentType statusEffect() {
		return new CStatusEffectArgumentType();
	}

	public static StatusEffect getCStatusEffect(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, StatusEffect.class);
	}

	@Override
	public StatusEffect parse(final StringReader stringReader) throws CommandSyntaxException {
		Identifier identifier = Identifier.fromCommandInput(stringReader);
		return Registries.STATUS_EFFECT.getOrEmpty(identifier).orElseThrow(() -> INVALID_EFFECT_EXCEPTION.create(identifier));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
		return CommandSource.suggestIdentifiers(Registries.STATUS_EFFECT.getIds(), builder);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
