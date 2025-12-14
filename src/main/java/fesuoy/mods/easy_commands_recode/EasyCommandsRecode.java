package fesuoy.mods.easy_commands_recode;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class EasyCommandsRecode implements ModInitializer {
	public static final String MOD_ID = "easy_commands_recode";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final GameRule<@NotNull Integer> TREE_HEIGHT =
            GameRuleBuilder.forInteger(1).buildAndRegister(Identifier.fromNamespaceAndPath("easy_commands_recode", "tree_height"));
    public static final GameRule<@NotNull Boolean> EXPLOSIVE_PROJECTILES_ENABLED =
            GameRuleBuilder.forBoolean(false).buildAndRegister(Identifier.fromNamespaceAndPath("easy_commands_recode", "explosive_projectiles"));

    public static final GameRule<@NotNull Double> EXPLOSION_POWER =
            GameRuleBuilder.forDouble(2.5).buildAndRegister(Identifier.fromNamespaceAndPath("easy_commands_recode", "explosion_power"));

    public static Iterable<ServerLevel> worlds;

    private static int enchantSwordForPlayer(ServerPlayer player, CommandSourceStack source) {
        ItemStack stack = player.getMainHandItem();
        if (stack.is(Items.WOODEN_SWORD) || stack.is(Items.STONE_SWORD)
         || stack.is(Items.COPPER_SWORD) || stack.is(Items.IRON_SWORD)
         || stack.is(Items.GOLDEN_SWORD) || stack.is(Items.DIAMOND_SWORD)
         || stack.is(Items.NETHERITE_SWORD)) {
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:sharpness 5", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:fire_aspect 2", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:knockback 2", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:looting 3", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:sweeping_edge 3", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:mending 1", player.getScoreboardName())
            );
            source.getServer().getCommands().performPrefixedCommand(
                source.withSuppressedOutput().withPermission(PermissionSet.ALL_PERMISSIONS),
                String.format("enchant %s minecraft:unbreaking 3", player.getScoreboardName())
            );
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.translatable("easy_commands_recode.command.enchantsword.failure"));
            return 0;
        }
    }


	@Override
	public void onInitialize() {
		try {
            LOGGER.info("Easy Commands Initialized!");
            CommandRegistrationCallback.EVENT.register(this::registerCommands);
            ServerLifecycleEvents.SERVER_STARTED.register((server -> worlds = server.getAllLevels()));
            ServerLifecycleEvents.SERVER_STOPPED.register((server -> worlds = null));
        } catch (Exception e) {
            LOGGER.error("Easy Commands Failed to Initialize!", e);
        }
	}

    public static void explode(@NotNull Projectile projectile, float power) {
        if (projectile.isRemoved()) {
            return;
        }

        Level world = projectile.level();
        Vec3 pos = projectile.position();

        try {
            // Create explosion at projectile position
            world.explode(
                projectile.getOwner(),
                null,
                null,
                pos.x, pos.y, pos.z,
                power,
                false,
                Level.ExplosionInteraction.TNT  // Better explosion type for projectiles
            );
        } catch (Exception e) {
            LOGGER.error("Failed to create explosion for projectile", e);
        } finally {
            // remove the projectile after explosion so it doesn't keep exploding forever
            if (!projectile.isRemoved()) {
                projectile.discard();
            }
        }
    }

    public static Integer getTreeHeight() {
        if (worlds != null) {
            for (ServerLevel world : worlds) {
                if (world.getGameRules().get(TREE_HEIGHT) > 0) {
                    return world.getGameRules().get(TREE_HEIGHT);
                }
            }
        }
        return 1;
    }


    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildCtx, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("repair")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player != null) {
                        ItemStack stack = player.getMainHandItem();
                        if (stack.isDamageableItem()) {
                            stack.setDamageValue(0);
                            player.playSound(SoundEvents.ANVIL_USE);
                        } else {
                            ctx.getSource().sendFailure(Component.translatable("easy_commands_recode.command.repair.failure"));
                            return 0;
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(Commands.literal("enchantsword")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        ctx.getSource().sendFailure(Component.translatable("easy_commands_recode.command.enchantsword.failure"));
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player != null) {
                        return enchantSwordForPlayer(player, ctx.getSource());
                    }
                    return 0;
                })
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                            int successCount = 0;
                            for (ServerPlayer player : players) {
                                if (enchantSwordForPlayer(player, ctx.getSource()) == Command.SINGLE_SUCCESS) {
                                    successCount++;
                                }
                            }
                            return successCount > 0 ? Command.SINGLE_SUCCESS : 0;
                        })));
        dispatcher.register(Commands.literal("repairinventory")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    if (!ctx.getSource().isPlayer()) {
                        return 0;
                    }
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (player != null) {
                        boolean anyItemRepaired = false;
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.isDamageableItem()) {
                                stack.setDamageValue(0);
                                anyItemRepaired = true;
                            }
                        }
                        if (anyItemRepaired) {
                            player.playSound(SoundEvents.ANVIL_USE);
                        }
                        else {
                            ctx.getSource().sendFailure(Component.translatable("easy_commands_recode.command.repairinventory.failure"));
                            return 0;
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal("repairall")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.argument("repairInventory", BoolArgumentType.bool())
                        .executes(ctx -> {
                            if (BoolArgumentType.getBool(ctx, "repairInventory")) {
                                for (ServerLevel world : worlds) {
                                    for (ServerPlayer player : world.players()) {
                                        boolean repaired = false;
                                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                            ItemStack stack = player.getInventory().getItem(i);
                                            if (stack.isDamageableItem()) {
                                                stack.setDamageValue(0);
                                                repaired = true;
                                            }
                                        }
                                        if (repaired) {
                                            world.playSound(player, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                                        }
                                    }
                                }
                            } else {
                                // only repair item they may be holding
                                for (ServerLevel world : worlds) {
                                    for (ServerPlayer player : world.players()) {
                                        ItemStack stack = player.getMainHandItem();
                                        if (stack.isDamageableItem()) {
                                            stack.setDamageValue(0);
                                            world.playSound(player, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);
                                        }
                                    }
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("killall")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    String killCommand = "kill @e[type=!minecraft:player]";
                    ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource().withSuppressedOutput(), killCommand);
                    ctx.getSource().sendSuccess(() -> Component.translatable("easy_commands_recode.command.killall.success"), true);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("shouldAlsoKillPlayers", BoolArgumentType.bool())
                        .executes(ctx -> {
                            boolean shouldAlsoKillPlayers = BoolArgumentType.getBool(ctx, "shouldAlsoKillPlayers");
                            String killCommand = shouldAlsoKillPlayers ? "kill @e" : "kill @e[type=!minecraft:player]";
                            ctx.getSource().getServer().getCommands().performPrefixedCommand(ctx.getSource().withSuppressedOutput(), killCommand);

                            if (shouldAlsoKillPlayers) {
                                ctx.getSource().sendSuccess(() -> Component.translatable(
                                    "easy_commands_recode.command.killall.success.players"), true);
                            } else {
                                ctx.getSource().sendSuccess(() ->
                                    Component.translatable("easy_commands_recode.command.killall.success"), true);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("heal")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.heal(player.getMaxHealth() + player.getMaxAbsorption());
                    player.level().playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                            for (ServerPlayer player : players) {
                                player.heal(player.getMaxHealth() + player.getMaxAbsorption());
                                player.level().playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
                            }
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("alsoFeed", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                                    boolean alsoFeed = BoolArgumentType.getBool(ctx, "alsoFeed");
                                    for (ServerPlayer player : players) {
                                        player.heal(player.getMaxHealth() + player.getMaxAbsorption());
                                        if (alsoFeed) {
                                            player.getFoodData().eat(20, 20);
                                        }
                                        player.level().playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))));
        dispatcher.register(Commands.literal("feed")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getFoodData().eat(20, 20);
                    player.level().playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("players", EntityArgument.players())
                        .executes(ctx -> {
                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
                            for (ServerPlayer player : players) {
                                player.getFoodData().eat(20, 20);
                                player.level().playSound(player, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.0F, 1.0F);
                            }
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("day")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(1000);
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal("noon")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(6000);
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal("night")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(13000);
                    return Command.SINGLE_SUCCESS;
                }));
        dispatcher.register(Commands.literal("midnight")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .executes(ctx -> {
                    ctx.getSource().getLevel().setDayTime(18000);
                    return Command.SINGLE_SUCCESS;
                }));

        dispatcher.register(Commands.literal("setExplosionPower")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.argument("power", DoubleArgumentType.doubleArg(0.1, 5000.0))
                        .executes(ctx -> {
                            double newPower = FloatArgumentType.getFloat(ctx, "power");
                            ServerLevel level = ctx.getSource().getLevel();
                            level.getGameRules().set(EXPLOSION_POWER, newPower, level.getServer());
                            ctx.getSource().sendSuccess(() ->
                                Component.translatable("easy_commands_recode.command.setexplosionpower.success", newPower), true);
                            return Command.SINGLE_SUCCESS;
                        })));
        dispatcher.register(Commands.literal("explode")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(Commands.argument("position", Vec3Argument.vec3())
                        .executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            ServerLevel world = source.getLevel();
                            Vec3 pos = Vec3Argument.getVec3(ctx, "position");
                            double power = world.getGameRules().get(EXPLOSION_POWER);

                            world.explode(source.getEntity(), pos.x, pos.y, pos.z, (float)power, false, Level.ExplosionInteraction.TNT);
                            return Command.SINGLE_SUCCESS;
                        })
                        .then(Commands.argument("power", FloatArgumentType.floatArg(0.1f))
                                .suggests((c, b) -> {
                                    b.suggest(1);
                                    b.suggest(3);
                                    b.suggest(5);
                                    b.suggest(10);
                                    return b.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ServerLevel world = source.getLevel();
                                    float power = FloatArgumentType.getFloat(ctx, "power");
                                    Vec3 pos = Vec3Argument.getVec3(ctx, "position");

                                    world.explode(source.getEntity(), pos.x, pos.y, pos.z, power, false, Level.ExplosionInteraction.TNT);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(Commands.argument("createFire", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            ServerLevel world = source.getLevel();
                                            float power = FloatArgumentType.getFloat(ctx, "power");
                                            boolean createFire = BoolArgumentType.getBool(ctx, "createFire");
                                            Vec3 pos = Vec3Argument.getVec3(ctx, "position");

                                            world.explode(source.getEntity(), pos.x, pos.y, pos.z, power, createFire, Level.ExplosionInteraction.TNT);
                                            return Command.SINGLE_SUCCESS;
                                        })))));
        dispatcher.register(Commands.literal("rename")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
                .then(Commands.argument("newItemName", StringArgumentType.string())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ItemStack stack = player.getMainHandItem();
                            if (stack.isEmpty()) {
                                ctx.getSource().sendFailure(Component.translatable("easy_commands_recode.command.rename.failure.noitem"));
                                return 0;
                            }

                            String newName = StringArgumentType.getString(ctx, "newItemName");
                            MutableComponent component = Component.literal(newName);
                            stack.set(DataComponents.CUSTOM_NAME, component);

                            ctx.getSource().sendSuccess(() -> Component.translatable("easy_commands_recode.command.rename.success", newName), true);
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );

    }
}