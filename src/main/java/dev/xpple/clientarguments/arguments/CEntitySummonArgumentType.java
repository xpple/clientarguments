package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.entity.EntityType;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Arrays;
import java.util.Collection;

public class CEntitySummonArgumentType implements ArgumentType<Identifier> {

	private static final Collection<String> EXAMPLES = Arrays.asList("minecraft:pig", "cow");
	public static final DynamicCommandExceptionType NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(id -> new TranslatableText("entity.notFound", id));

	public static CEntitySummonArgumentType entitySummon() {
		return new CEntitySummonArgumentType();
	}

	public static Identifier getEntitySummon(final CommandContext<FabricClientCommandSource> context, final String name) throws CommandSyntaxException {
		return validate(context.getArgument(name, Identifier.class));
	}

	@Override
	public Identifier parse(final StringReader stringReader) throws CommandSyntaxException {
		return validate(Identifier.fromCommandInput(stringReader));
	}

	private static Identifier validate(Identifier id) throws CommandSyntaxException {
		Registry.ENTITY_TYPE.getOrEmpty(id).filter(EntityType::isSummonable).orElseThrow(() -> NOT_FOUND_EXCEPTION.create(id));
		return id;
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
