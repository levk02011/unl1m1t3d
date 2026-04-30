package com.example.mod_1_21_4;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * AncientBot v2.1 - Оптимізована версія
 */
public class AncientBotHandler {
    private static final int SCAN_RADIUS = 17;
    private static final int TOLERANCE = 5;
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
    private static BotState lastExitState = BotState.IDLE;
    
    private static BlockPos blastCenter = null;
    private static List<BlockPos> debrisLocations = new ArrayList<>();
    private static List<BlockPos> collectionPath = new ArrayList<>();
    private static int collectionIndex = 0;
    private static int scanProgress = 0;
    private static Map<BlockPos, Integer> blockDensityMap = new HashMap<>();
    
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        // Перевірка на критичні умови (евакуація)
        if (shouldEmergencyExit(player)) {
            if (currentState != BotState.EMERGENCY_EXIT) {
                emergencyExit(player, "Критична умова (відсутність бафу/ресурсів)");[cite: 12]
            }
            return;
        }
        
        switch (currentState) {
            case SCANNING -> scanningTick(player);
            case MOVING_TO_BLAST_CENTER -> moveToBlastCenterTick(player);[cite: 12]
            case BUFFING -> buffingTick(player);[cite: 12]
            case PLACING_EXPLOSIVE -> placingExplosiveTick(player);
            case IGNITING -> ignitingTick(player);
            case EVACUATING -> evacuatingTick(player);
            case WAITING_IN_HUB -> waitingInHubTick(player);
            case RETURNING -> returningTick(player);
            case SCANNING_DEBRIS -> scanningDebrisTick(player);
            case COLLECTING -> collectingTick(player);[cite: 12]
            default -> {}
        }
    }

    // ==================== ЛОГІКА РУХУ (БЕЗ ТРЯСКИ) ====================
    
    private static void moveToBlastCenterTick(ClientPlayerEntity player) {
        if (blastCenter == null) return;
    
        Vec3d targetPos = Vec3d.ofCenter(blastCenter);
        double distanceSq = player.getPos().squaredDistanceTo(targetPos);[cite: 12]

        if (distanceSq < 0.7) { // Радіус зупинки[cite: 12]
            stopMovement();[cite: 12]
            player.sendMessage(Text.literal("§a✓ На місці. Готуюсь до підриву."), false);
            currentState = BotState.BUFFING;
            stateStartTime = System.currentTimeMillis();
            return;
        }
    
        lookAt(targetPos);[cite: 12]
        
        // Пробиваємо шлях, якщо попереду блоки
        BlockPos headPos = player.getBlockPos().offset(player.getHorizontalFacing());[cite: 12]
        if (!player.getWorld().getBlockState(headPos).isAir() && !player.getWorld().getBlockState(headPos).isOf(Blocks.BEDROCK)) {[cite: 12]
            MinecraftClient.getInstance().interactionManager.attackBlock(headPos, Direction.UP);[cite: 12]
            player.swingHand(Hand.MAIN_HAND);[cite: 12]
        }
    
        MinecraftClient.getInstance().options.forwardKey.setPressed(true);[cite: 12]
    }

    private static void buffingTick(ClientPlayerEntity player) {
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) != null) {[cite: 12]
            currentState = BotState.PLACING_EXPLOSIVE;
            stateStartTime = System.currentTimeMillis();
            return;
        }
        
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed > 1000 && elapsed < 4000) { // Затримка на пиття[cite: 12]
            ItemStack potion = findPotionInInventory(player);[cite: 12]
            if (potion != null) {
                int slot = findInventorySlot(player, potion);[cite: 12]
                if (slot != -1) {
                    player.getInventory().selectedSlot = slot;[cite: 12]
                    MinecraftClient.getInstance().options.useKey.setPressed(true);[cite: 12]
                }
            }
        } else if (elapsed >= 4000) {
            MinecraftClient.getInstance().options.useKey.setPressed(false);[cite: 12]
        }
    }

    private static void collectingTick(ClientPlayerEntity player) {
        if (collectionIndex >= collectionPath.size()) {
            player.sendMessage(Text.literal("§6✓ Всі обломки зібрані."), false);
            currentState = BotState.SCANNING;
            return;
        }
    
        BlockPos target = collectionPath.get(collectionIndex);
        Vec3d targetVec = Vec3d.ofCenter(target);
        double distanceSq = player.getPos().squaredDistanceTo(targetVec);[cite: 12]
    
        if (distanceSq < 4.5) {[cite: 12]
            stopMovement();[cite: 12]
            lookAt(targetVec);[cite: 12]
            MinecraftClient.getInstance().interactionManager.attackBlock(target, Direction.UP);[cite: 12]
            player.swingHand(Hand.MAIN_HAND);[cite: 12]
            
            if (player.getWorld().getBlockState(target).isAir()) collectionIndex++;[cite: 12]
        } else {
            lookAt(targetVec);[cite: 12]
            MinecraftClient.getInstance().options.forwardKey.setPressed(true);[cite: 12]
        }
    }

    // ==================== ДОПОМІЖНІ МЕТОДИ ====================

    private static void lookAt(Vec3d target) {[cite: 12]
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        double diffX = target.x - player.getX();
        double diffY = target.y - (player.getY() + player.getEyeHeight(player.getPose()));
        double diffZ = target.z - player.getZ();
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        player.setYaw(yaw);[cite: 12]
        player.setPitch(pitch);[cite: 12]
    }

    private static void stopMovement() {[cite: 12]
        MinecraftClient.getInstance().options.forwardKey.setPressed(false);[cite: 12]
        if (MinecraftClient.getInstance().player != null) {
            Vec3d v = MinecraftClient.getInstance().player.getVelocity();
            MinecraftClient.getInstance().player.setVelocity(0, v.y, 0);[cite: 12]
        }
    }

    private static boolean shouldEmergencyExit(ClientPlayerEntity player) {
        // Додано стани, в яких відсутність ефекту — це нормально[cite: 12]
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) == null &&
            currentState != BotState.IDLE &&
            currentState != BotState.SCANNING &&
            currentState != BotState.MOVING_TO_BLAST_CENTER &&[cite: 12]
            currentState != BotState.BUFFING &&[cite: 12]
            currentState != BotState.EVACUATING &&
            currentState != BotState.WAITING_IN_HUB &&
            currentState != BotState.RETURNING) {[cite: 12]
            return true;
        }
        return false;
    }

    // Логіка сканування, пошуку інвентаря та інше (залишено з версії 2.0)
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
            currentState = BotState.MOVING_TO_BLAST_CENTER;[cite: 12]
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
            player.networkHandler.sendCommand("an 1"); // Анархія номер 1 (зміни під свій конфиг)
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
            currentState = BotState.COLLECTING;[cite: 12]
        }
    }

    private static void emergencyExit(ClientPlayerEntity player, String reason) {
        lastExitReason = reason;
        currentState = BotState.EMERGENCY_EXIT;
        player.networkHandler.sendCommand("hub");
        player.sendMessage(Text.literal("§c🚨 ЕВАКУАЦІЯ: " + reason), false);
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
}