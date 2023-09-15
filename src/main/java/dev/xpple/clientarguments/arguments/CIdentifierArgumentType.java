package dev.xpple.clientarguments.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Collection;

public class CIdentifierArgumentType implements ArgumentType<Identifier> {

	private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
	private static final DynamicCommandExceptionType UNKNOWN_ADVANCEMENT_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("advancement.advancementNotFound", id));
	private static final DynamicCommandExceptionType UNKNOWN_RECIPE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("recipe.notFound", id));
	private static final DynamicCommandExceptionType UNKNOWN_ATTRIBUTE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("attribute.unknown", id));

	public static CIdentifierArgumentType identifier() {
		return new CIdentifierArgumentType();
	}

	public static PlacedAdvancement getCAdvancementArgument(CommandContext<FabricClientCommandSource> context, String argumentName) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(argumentName, Identifier.class);
		PlacedAdvancement advancement = context.getSource().getClient().getNetworkHandler().getAdvancementHandler().getManager().get(identifier);
		if (advancement == null) {
			throw UNKNOWN_ADVANCEMENT_EXCEPTION.create(identifier);
		}
		return advancement;
	}

	public static RecipeEntry<?> getCRecipeArgument(CommandContext<FabricClientCommandSource> context, String argumentName) throws CommandSyntaxException {
		RecipeManager recipeManager = context.getSource().getWorld().getRecipeManager();
		Identifier identifier = context.getArgument(argumentName, Identifier.class);
		return recipeManager.get(identifier).orElseThrow(() -> UNKNOWN_RECIPE_EXCEPTION.create(identifier));
	}

	/*
	public static LootCondition getCPredicateArgument(CommandContext<FabricClientCommandSource> context, String argumentName) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(argumentName, Identifier.class);
		LootConditionManager lootConditionManager = context.getSource().getServer().getPredicateManager();
		LootCondition lootCondition = lootConditionManager.get(identifier);
		if (lootCondition == null) {
			throw UNKNOWN_PREDICATE_EXCEPTION.create(identifier);
		}
		return lootCondition;
	}
	 */

	/*
	public static LootFunction getCItemModifierArgument(CommandContext<FabricClientCommandSource> context, String argumentName) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(argumentName, Identifier.class);
		LootFunctionManager lootFunctionManager = context.getSource().getServer().getItemModifierManager();
		LootFunction lootFunction = lootFunctionManager.get(identifier);
		if (lootFunction == null) {
			throw UNKNOWN_ITEM_MODIFIER_EXCEPTION.create(identifier);
		}
		return lootFunction;
	}
	 */

	public static EntityAttribute getCAttributeArgument(CommandContext<FabricClientCommandSource> context, String argumentName) throws CommandSyntaxException {
		Identifier identifier = context.getArgument(argumentName, Identifier.class);
		return Registries.ATTRIBUTE.getOrEmpty(identifier).orElseThrow(() -> UNKNOWN_ATTRIBUTE_EXCEPTION.create(identifier));
	}

	public static Identifier getCIdentifier(final CommandContext<FabricClientCommandSource> context, final String name) {
		return context.getArgument(name, Identifier.class);
	}

	@Override
	public Identifier parse(final StringReader stringReader) throws CommandSyntaxException {
		return Identifier.fromCommandInput(stringReader);
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
