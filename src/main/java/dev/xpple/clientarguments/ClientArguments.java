package dev.xpple.clientarguments;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.AmbiguityConsumer;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import dev.xpple.clientarguments.arguments.CDimensionArgumentType.DimensionArgument;
import dev.xpple.clientarguments.arguments.CEntityAnchorArgumentType.EntityAnchor;
import dev.xpple.clientarguments.arguments.CEntitySelectorOptions;
import dev.xpple.clientarguments.arguments.CNbtPathArgumentType.NbtPath;
import dev.xpple.clientarguments.arguments.COperationArgumentType.Operation;
import dev.xpple.clientarguments.arguments.CPosArgument;
import dev.xpple.clientarguments.arguments.CRegistryPredicateArgumentType;
import dev.xpple.clientarguments.arguments.ClientBlockArgument;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.test.TestFunction;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.xpple.clientarguments.arguments.CAngleArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockPosArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CBlockStateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CColorArgumentType.*;
import static dev.xpple.clientarguments.arguments.CColumnPosArgumentType.*;
import static dev.xpple.clientarguments.arguments.CDimensionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEntityAnchorArgumentType.*;
import static dev.xpple.clientarguments.arguments.CEntityArgumentType.*;
import static dev.xpple.clientarguments.arguments.CGameProfileArgumentType.*;
import static dev.xpple.clientarguments.arguments.CIdentifierArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemSlotArgumentType.*;
import static dev.xpple.clientarguments.arguments.CItemStackArgumentType.*;
import static dev.xpple.clientarguments.arguments.CMessageArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtCompoundArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtElementArgumentType.*;
import static dev.xpple.clientarguments.arguments.CNbtPathArgumentType.*;
import static dev.xpple.clientarguments.arguments.COperationArgumentType.*;
import static dev.xpple.clientarguments.arguments.CParticleEffectArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryKeyArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRegistryPredicateArgumentType.*;
import static dev.xpple.clientarguments.arguments.CRotationArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreHolderArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardCriterionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardObjectiveArgumentType.*;
import static dev.xpple.clientarguments.arguments.CScoreboardSlotArgumentType.*;
import static dev.xpple.clientarguments.arguments.CSwizzleArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTeamArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTestClassArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTestFunctionArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTextArgumentType.*;
import static dev.xpple.clientarguments.arguments.CTimeArgumentType.*;
import static dev.xpple.clientarguments.arguments.CUuidArgumentType.*;
import static dev.xpple.clientarguments.arguments.CVec2ArgumentType.*;
import static dev.xpple.clientarguments.arguments.CVec3ArgumentType.*;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ClientArguments implements ClientModInitializer {
    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("commands.locate.invalid"));

    @Override
    public void onInitializeClient() {
        CEntitySelectorOptions.register();

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            ClientCommandRegistrationCallback.EVENT.register(ClientArguments::registerTestCommand);
        }
    }

    /**
     * <p>
     * Registering this test command will trigger {@link com.mojang.brigadier.tree.CommandNode#findAmbiguities(AmbiguityConsumer)},
     * which checks the validity of the example inputs - and with that also the validity of the argument in question.
     * <p>
     * The command feedback doesn't always indicate something meaningful. The arguments of this command may be extended
     * to be more concise.
     */
    private static void registerTestCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("clientarguments:test")
                .then(literal("angle").then(argument("angle", angle())
                        .executes(ctx -> {
                            float angle = getCAngle(ctx, "angle");
                            ctx.getSource().sendFeedback(of(angle));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("blockpos").then(argument("blockpos", blockPos())
                        .executes(ctx -> {
                            BlockPos blockpos = getCBlockPos(ctx, "blockpos");
                            ctx.getSource().sendFeedback(of(blockpos.getX(), blockpos.getY(), blockpos.getZ()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("blockpredicate").then(argument("blockpredicate", blockPredicate(registryAccess))
                        .executes(ctx -> {
                            Predicate<CachedBlockPosition> clientBlockPredicate = getCBlockPredicate(ctx, "blockpredicate");
                            ctx.getSource().sendFeedback(of(clientBlockPredicate.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("blockstate").then(argument("blockstate", blockState(registryAccess))
                        .executes(ctx -> {
                            ClientBlockArgument blockArgument = getCBlockState(ctx, "blockstate");
                            String[] arr = new String[4];
                            arr[0] = blockArgument.getBlock() == null ? "null" : blockArgument.getBlock().toString();
                            arr[1] = blockArgument.getBlockState() == null ? "null" : blockArgument.getBlockState().toString();
                            arr[2] = blockArgument.getNbt() == null ? "null" : blockArgument.getNbt().toString();
                            arr[3] = blockArgument.getProperties() == null ? "null" : blockArgument.getProperties().entrySet().stream()
                                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                                    .collect(Collectors.joining("; "));
                            ctx.getSource().sendFeedback(of(arr));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("color").then(argument("color", color())
                        .executes(ctx -> {
                            Formatting color = getCColor(ctx, "color");
                            ctx.getSource().sendFeedback(of(color.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("columnpos").then(argument("columnpos", columnPos())
                        .executes(ctx -> {
                            ColumnPos columnPos = getCColumnPos(ctx, "columnpos");
                            ctx.getSource().sendFeedback(of(columnPos.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("dimension").then(argument("dimension", dimension())
                        .executes(ctx -> {
                            DimensionArgument dimension = getCDimensionArgument(ctx, "dimension");
                            ctx.getSource().sendFeedback(of(dimension.getName()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("entityanchor").then(argument("entityanchor", entityAnchor())
                        .executes(ctx -> {
                            EntityAnchor entityAnchor = getCEntityAnchor(ctx, "entityanchor");
                            ctx.getSource().sendFeedback(of(entityAnchor.positionAt(ctx.getSource()).toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("entity").then(argument("entity", entity())
                        .executes(ctx -> {
                            Entity entity = getCEntity(ctx, "entity");
                            ctx.getSource().sendFeedback(of(entity.getNameForScoreboard()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("gameprofile").then(argument("gameprofile", gameProfile())
                        .executes(ctx -> {
                            Collection<GameProfile> gameProfiles = getCProfileArgument(ctx, "gameprofile");
                            ctx.getSource().sendFeedback(of(gameProfiles.stream().map(GameProfile::getName).toArray(String[]::new)));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("identifier").then(argument("identifier", identifier())
                        .executes(ctx -> {
                            Identifier identifier = getCIdentifier(ctx, "identifier");
                            ctx.getSource().sendFeedback(of(identifier.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("itempredicate").then(argument("itempredicate", itemPredicate(registryAccess))
                        .executes(ctx -> {
                            Predicate<ItemStack> itemPredicate = getCItemPredicate(ctx, "itempredicate");
                            ctx.getSource().sendFeedback(of(itemPredicate.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("itemslot").then(argument("itemslot", itemSlot())
                        .executes(ctx -> {
                            Integer itemSlot = getCItemSlot(ctx, "itemslot");
                            ctx.getSource().sendFeedback(of(itemSlot.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("itemstack").then(argument("itemstack", itemStack(registryAccess))
                        .executes(ctx -> {
                            ItemStackArgument itemStackArgument = getCItemStackArgument(ctx, "itemstack");
                            ctx.getSource().sendFeedback(of(itemStackArgument.asString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("message").then(argument("message", message())
                        .executes(ctx -> {
                            Text message = getCMessage(ctx, "message");
                            ctx.getSource().sendFeedback(message);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("nbtcompound").then(argument("nbtcompound", nbtCompound())
                        .executes(ctx -> {
                            NbtCompound nbtCompound = getCNbtCompound(ctx, "nbtcompound");
                            ctx.getSource().sendFeedback(of(nbtCompound.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("nbtelement").then(argument("nbtelement", nbtElement())
                        .executes(ctx -> {
                            NbtElement nbtElement = getCNbtElement(ctx, "nbtelement");
                            ctx.getSource().sendFeedback(of(nbtElement.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("nbtpath").then(argument("nbtpath", nbtPath())
                        .executes(ctx -> {
                            NbtPath nbtPath = getCNbtPath(ctx, "nbtpath");
                            ctx.getSource().sendFeedback(of(nbtPath.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("operation").then(argument("operation", operation())
                        .executes(ctx -> {
                            Operation operation = getCOperation(ctx, "operation");
                            ctx.getSource().sendFeedback(of(operation.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("particleeffect").then(argument("particleeffect", particleEffect())
                        .executes(ctx -> {
                            ParticleEffect particleEffect = getCParticle(ctx, "particleeffect");
                            ctx.getSource().sendFeedback(of(particleEffect.asString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("rotation").then(argument("rotation", rotation())
                        .executes(ctx -> {
                            CPosArgument rotation = getCRotation(ctx, "rotation");
                            Vec2f vec2f = rotation.toAbsoluteRotation(ctx.getSource());
                            ctx.getSource().sendFeedback(of(vec2f.x, vec2f.y));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("scoreboardcriterion").then(argument("scoreboardcriterion", scoreboardCriterion())
                        .executes(ctx -> {
                            ScoreboardCriterion scoreboardCriterion = getCCriterion(ctx, "scoreboardcriterion");
                            ctx.getSource().sendFeedback(of(scoreboardCriterion.getName()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("scoreboardobjective").then(argument("scoreboardobjective", scoreboardObjective())
                        .executes(ctx -> {
                            ScoreboardObjective scoreboardObjective = getCObjective(ctx, "scoreboardobjective");
                            ctx.getSource().sendFeedback(of(scoreboardObjective.getName()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("scoreboardslot").then(argument("scoreboardslot", scoreboardSlot())
                        .executes(ctx -> {
                            ScoreboardDisplaySlot scoreboardSlot = getCScoreboardSlot(ctx, "scoreboardslot");
                            ctx.getSource().sendFeedback(of(scoreboardSlot.asString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("scoreholder").then(argument("scoreholder", scoreHolder())
                        .executes(ctx -> {
                            ScoreHolder scoreHolder = getCScoreHolder(ctx, "scoreholder");
                            ctx.getSource().sendFeedback(scoreHolder.getDisplayName());
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("swizzle").then(argument("swizzle", swizzle())
                        .executes(ctx -> {
                            EnumSet<Direction.Axis> swizzle = getCSwizzle(ctx, "swizzle");
                            Set<Direction.Axis> axes = new HashSet<>(swizzle);
                            String[] arr = axes.stream().map(Direction.Axis::toString).toArray(String[]::new);
                            ctx.getSource().sendFeedback(of(arr));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("team").then(argument("team", team())
                        .executes(ctx -> {
                            Team team = getCTeam(ctx, "team");
                            ctx.getSource().sendFeedback(team.getDisplayName());
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("testclass").then(argument("testclass", testClass())
                        .executes(ctx -> {
                            String testClass = getCTestClass(ctx, "testclass");
                            ctx.getSource().sendFeedback(of(testClass));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("testfunction").then(argument("testfunction", testFunction())
                        .executes(ctx -> {
                            TestFunction testFunction = getCFunction(ctx, "testfunction");
                            ctx.getSource().sendFeedback(of(testFunction.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("text").then(argument("text", text())
                        .executes(ctx -> {
                            Text text = getCTextArgument(ctx, "text");
                            ctx.getSource().sendFeedback(text);
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("time").then(argument("time", time())
                        .executes(ctx -> {
                            Integer integer = getCTime(ctx, "time");
                            ctx.getSource().sendFeedback(of(integer));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("uuid").then(argument("uuid", uuid())
                        .executes(ctx -> {
                            UUID uuid = getCUuid(ctx, "uuid");
                            ctx.getSource().sendFeedback(of(uuid.toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("vec2").then(argument("vec2", vec2())
                        .executes(ctx -> {
                            Vec2f vec2f = getCVec2(ctx, "vec2");
                            ctx.getSource().sendFeedback(of(vec2f.x, vec2f.y));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("vec3").then(argument("vec3", vec3())
                        .executes(ctx -> {
                            Vec3d vec3d = getCVec3(ctx, "vec3");
                            ctx.getSource().sendFeedback(of(vec3d.x, vec3d.y, vec3d.z));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("registrykey").then(argument("registrykey", registryKey(RegistryKeys.CONFIGURED_FEATURE))
                        .executes(ctx -> {
                            RegistryEntry<ConfiguredFeature<?, ?>> registryKey = getCConfiguredFeatureEntry(ctx, "registrykey");
                            ctx.getSource().sendFeedback(of(registryKey.value().toString()));
                            return Command.SINGLE_SUCCESS;
                        })))
                .then(literal("registrypredicate").then(argument("registrypredicate", registryPredicate(RegistryKeys.STRUCTURE))
                        .executes(ctx -> {
                            CRegistryPredicateArgumentType.RegistryPredicate<Structure> registryPredicate = getCPredicate(ctx, "registrypredicate", RegistryKeys.STRUCTURE, INVALID_STRUCTURE_EXCEPTION);
                            ctx.getSource().sendFeedback(of(registryPredicate.asString()));
                            return Command.SINGLE_SUCCESS;
                        })))
        );
    }

    private static MutableText of(String... s) {
        return Text.literal(String.join(", ", s));
    }

    private static MutableText of(Number... numbers) {
        return of(Stream.of(numbers).map(String::valueOf).toArray(String[]::new));
    }
}
