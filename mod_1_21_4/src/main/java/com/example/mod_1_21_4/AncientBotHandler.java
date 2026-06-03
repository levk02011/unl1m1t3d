package com.example.mod_1_21_4;

// Імпортуємо Baritone API
import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.utils.Rotation;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * AncientBot v2.2 - Інтегровано з Baritone API
 */
public class AncientBotHandler {
    private static final int SCAN_RADIUS = 17;
    private static final long HUB_DELAY = 5000; 
    
    enum BotState {
        IDLE, SCANNING, MOVING_TO_BLAST_CENTER, BUFFING, 
        PLACING_EXPLOSIVE, IGNITING, EVACUATING, WAITING_IN_HUB, 
        RETURNING, SCANNING_DEBRIS, COLLECTING, EMERGENCY_EXIT
    }
    
    private static BotState currentState = BotState.IDLE;
    private static long stateStartTime = 0;
    private static String lastExitReason = "Невідомо";
    private static long lastExitTime = 0;
    
    private static BlockPos blastCenter = null;
    private static List<BlockPos> debrisLocations = new ArrayList<>();
    private static List<BlockPos> collectionPath = new ArrayList<>();
    private static int collectionIndex = 0;
    private static int scanProgress = 0;
    private static Map<BlockPos, Integer> blockDensityMap = new HashMap<>();
    
    // Отримуємо головний інстанс барітона для нашого гравця
    private static final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        if (shouldEmergencyExit(player)) {
            if (currentState != BotState.EMERGENCY_EXIT) {
                emergencyExit(player, "Критична умова (відсутність бафу/ресурсів)");
            }
            return;
        }
        
        switch (currentState) {
            case SCANNING -> scanningTick(player);
            case MOVING_TO_BLAST_CENTER -> moveToBlastCenterTick(player);
            case BUFFING -> buffingTick(player);
            case PLACING_EXPLOSIVE -> placingExplosiveTick(player);
            case IGNITING -> ignitingTick(player);
            case EVACUATING -> evacuatingTick(player);
            case WAITING_IN_HUB -> waitingInHubTick(player);
            case RETURNING -> returningTick(player);
            case SCANNING_DEBRIS -> scanningDebrisTick(player);
            case COLLECTING -> collectingTick(player);
            default -> {}
        }
    }

    // ==================== ОПТИМІЗОВАНИЙ РУХ ЧЕРЕЗ BARITONE ====================
    
    private static void moveToBlastCenterTick(ClientPlayerEntity player) {
        if (blastCenter == null) return;
    
        // Якщо Барітон ще не йде нікуди — даємо йому ціль
        if (!baritone.getPathingBehavior().isPathing()) {
            player.sendMessage(Text.literal("§7[AncientBot] Розрахунок шляху до центру підриву..."), false);
            // Встановлюємо ціль безпосередньо в координати blastCenter
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(blastCenter.getX(), blastCenter.getY(), blastCenter.getZ()));
        }
    
        // Перевіряємо, чи прийшли ми на місце (чи досягнута ціль)
        if (player.getBlockPos().getSquaredDistance(blastCenter) <= 2.0 && !baritone.getPathingBehavior().isPathing()) {
            player.sendMessage(Text.literal("§a✓ На місці за допомогою Baritone. Готуюсь до підриву."), false);
            currentState = BotState.BUFFING;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private static void lookAtPosition(ClientPlayerEntity player, BlockPos target) {
        double dx = target.getX() + 0.5 - player.getX();
        double dz = target.getZ() + 0.5 - player.getZ();
        double dy = target.getY() + 0.5 - player.getEyeY();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        baritone.getLookBehavior().updateTarget(new Rotation(yaw, pitch), true);
    }

    private static void collectingTick(ClientPlayerEntity player) {
        if (collectionIndex >= collectionPath.size()) {
            player.sendMessage(Text.literal("§6✓ Всі обломки зібрані."), false);
            baritone.getPathingBehavior().cancelEverything(); // Про всяк випадок стопаємо барітон
            currentState = BotState.SCANNING;
            return;
        }
    
        BlockPos target = collectionPath.get(collectionIndex);
        
        // Якщо блок уже хтось зламав (або ми підібрали), переходимо до наступного
        if (player.getWorld().getBlockState(target).isAir()) {
            collectionIndex++;
            return;
        }

        // Даємо команду йти до уламка древніх дебрисів
        if (!baritone.getPathingBehavior().isPathing()) {
            baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(target.getX(), target.getY(), target.getZ()));
        }

        // Коли підійшли впритул — зупиняємось і ламаємо рукою/киркою
        if (player.getBlockPos().getSquaredDistance(target) <= 4.5) {
            baritone.getPathingBehavior().cancelEverything(); // Тимчасово стопаємо рух для видобутку
            
            // Повертаємо голову через вбудований LookBehavior Барітона (щоб не трясло античітом)
            lookAtPosition(player, target);
            
            MinecraftClient.getInstance().interactionManager.attackBlock(target, Direction.UP);
            player.swingHand(Hand.MAIN_HAND);
        }
    }

    // ==================== ЗМІНИ В ЕВАКУАЦІЇ ТА ДЕАКТИВАЦІЇ ====================

    private static void emergencyExit(ClientPlayerEntity player, String reason) {
        lastExitReason = reason;
        currentState = BotState.EMERGENCY_EXIT;
        baritone.getPathingBehavior().cancelEverything(); // Обов'язково вимикаємо барітон при паніці!
        player.networkHandler.sendCommand("hub");
        player.sendMessage(Text.literal("§c🚨 ЕВАКУАЦІЯ: " + reason), false);
    }

    public static void deactivate(ClientPlayerEntity player) {
        if (currentState != BotState.IDLE) {
            lastExitTime = System.currentTimeMillis();
            lastExitReason = "Деактивовано користувачем";
            currentState = BotState.IDLE;
            
            baritone.getPathingBehavior().cancelEverything(); // Стопаємо автобіг барітона
            
            player.sendMessage(Text.literal("§c[AncientBot] Деактивовано! Baritone зупинено."), false);
        }
    }

    // ==================== ІНШІ МЕТОДИ (ЗАЛИШЕНІ БЕЗ ЗМІН) ====================
    
    private static void buffingTick(ClientPlayerEntity player) {
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) != null) {
            currentState = BotState.PLACING_EXPLOSIVE;
            stateStartTime = System.currentTimeMillis();
            return;
        }
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed > 1000 && elapsed < 4000) {
            ItemStack potion = findPotionInInventory(player);
            if (potion != null) {
                int slot = findInventorySlot(player, potion);
                if (slot != -1) {
                    player.getInventory().selectedSlot = slot;
                    MinecraftClient.getInstance().options.useKey.setPressed(true);
                }
            }
        } else if (elapsed >= 4000) {
            MinecraftClient.getInstance().options.useKey.setPressed(false);
        }
    }

    private static boolean shouldEmergencyExit(ClientPlayerEntity player) {
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) == null &&
            currentState != BotState.IDLE && currentState != BotState.SCANNING &&
            currentState != BotState.MOVING_TO_BLAST_CENTER && currentState != BotState.BUFFING &&
            currentState != BotState.EVACUATING && currentState != BotState.WAITING_IN_HUB &&
            currentState != BotState.RETURNING) {
            return true;
        }
        return false;
    }

    private static void scanningTick(ClientPlayerEntity player) {
        World world = player.getWorld();
        int y = -SCAN_RADIUS + scanProgress;
        if (y > SCAN_RADIUS) { selectBlastCenter(player); return; }
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                BlockPos checkPos = player.getBlockPos().add(x, y, z);
                if (!world.getBlockState(checkPos).isAir()) {
                    BlockPos gridPos = new BlockPos((checkPos.getX()/5)*5, (checkPos.getY()/5)*5, (checkPos.getZ()/5)*5);
                    blockDensityMap.put(gridPos, blockDensityMap.getOrDefault(gridPos, 0) + 1);
                }
            }
        }
        scanProgress++;
    }

    private static void selectBlastCenter(ClientPlayerEntity player) {
        BlockPos best = blockDensityMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);
        if (best != null) {
            blastCenter = best;
            currentState = BotState.MOVING_TO_BLAST_CENTER;
        } else { currentState = BotState.IDLE; }
    }

    private static void placingExplosiveTick(ClientPlayerEntity player) {
        int slot = findInventorySlot(player, new ItemStack(Items.TNT));
        if (slot != -1) {
            player.getInventory().selectedSlot = slot;
            MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, 
                new BlockHitResult(Vec3d.ofCenter(blastCenter), Direction.UP, blastCenter, false));
            currentState = BotState.IGNITING;
        } else { emergencyExit(player, "Немає ТНТ"); }
    }

    private static void ignitingTick(ClientPlayerEntity player) {
        int slot = findInventorySlot(player, new ItemStack(Items.FLINT_AND_STEEL));
        if (slot != -1) {
            player.getInventory().selectedSlot = slot;
            MinecraftClient.getInstance().interactionManager.interactBlock(player, Hand.MAIN_HAND, 
                new BlockHitResult(Vec3d.ofCenter(blastCenter), Direction.UP, blastCenter, false));
            currentState = BotState.EVACUATING;
        } else { emergencyExit(player, "Немає кресала"); }
    }

    private static void evacuatingTick(ClientPlayerEntity player) {
        player.networkHandler.sendCommand("hub");
        currentState = BotState.WAITING_IN_HUB;
        stateStartTime = System.currentTimeMillis();
    }

    private static void waitingInHubTick(ClientPlayerEntity player) {
        if (System.currentTimeMillis() - stateStartTime >= HUB_DELAY) {
            player.networkHandler.sendCommand("an 1");
            currentState = BotState.RETURNING;
            stateStartTime = System.currentTimeMillis();
        }
    }

    private static void returningTick(ClientPlayerEntity player) {
        if (System.currentTimeMillis() - stateStartTime >= 1500) {
            currentState = BotState.SCANNING_DEBRIS;
        }
    }

    private static void scanningDebrisTick(ClientPlayerEntity player) {
        debrisLocations.clear();
        BlockPos p = player.getBlockPos();
        for (BlockPos bp : BlockPos.iterate(p.add(-20, -10, -20), p.add(20, 10, 20))) {
            if (player.getWorld().getBlockState(bp).isOf(Blocks.ANCIENT_DEBRIS)) {
                debrisLocations.add(bp.toImmutable());
            }
        }
        if (debrisLocations.isEmpty()) currentState = BotState.SCANNING;
        else {
            collectionPath = new ArrayList<>(debrisLocations);
            collectionIndex = 0;
            currentState = BotState.COLLECTING;
        }
    }

    private static ItemStack findPotionInInventory(ClientPlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() == Items.POTION) return stack;
        }
        return null;
    }

    private static int findInventorySlot(ClientPlayerEntity player, ItemStack target) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == target.getItem()) return i;
        }
        return -1;
    }

    public static void activate(ClientPlayerEntity player) {
        if (currentState == BotState.IDLE) {
            currentState = BotState.SCANNING;
            stateStartTime = System.currentTimeMillis();
            lastExitReason = "Невідомо";
            scanProgress = 0;
            blockDensityMap.clear();
            debrisLocations.clear();
            collectionPath.clear();
            collectionIndex = 0;
            blastCenter = null;
            player.sendMessage(Text.literal("§a[AncientBot] Активовано!"), false);
        }
    }

    public static void whyexit(ClientPlayerEntity player) {
        player.sendMessage(Text.literal("§e[AncientBot] Остання причина виходу: " + lastExitReason), false);
    }
}