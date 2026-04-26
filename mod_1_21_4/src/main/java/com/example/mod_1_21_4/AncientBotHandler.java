package com.example.mod_1_21_4;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;

/**
 * Обробляє логіку Ancient Bot моду для автоматичного видобування давніх обломків
 */
public class AncientBotHandler {
    private static final int SEARCH_RADIUS = 17;
    private static final int MAX_ERROR = 5;
    private static final int NEARBY_PLAYER_RADIUS = 50;
    private static final long POTION_DURATION_MS = 10000;
    private static final long HUB_DELAY_MS = 5000;
    
    private static State currentState = State.IDLE;
    private static BlockPos targetCenter = null;
    private static long potionStartTime = 0;
    private static long stateStartTime = 0;
    private static List<BlockPos> ancientDebrisLocations = new ArrayList<>();
    private static int currentDebrisIndex = 0;
    
    // State for block analysis across multiple ticks
    private static Map<BlockPos, Integer> blockDensity = new HashMap<>();
    private static int analysisX = -SEARCH_RADIUS;
    private static int analysisY = -SEARCH_RADIUS;
    private static int analysisZ = -SEARCH_RADIUS;
    private static boolean analysisComplete = false;
    
    // State for debris search across multiple ticks
    private static int debrisSearchX = -50;
    private static int debrisSearchZ = -50;
    private static int debrisSearchY = 0;
    private static boolean debrisSearchComplete = false;
    
    // State for pathfinding and block breaking
    private static List<BlockPos> miningPath = new ArrayList<>();
    private static int currentMiningIndex = 0;
    private static long lastBreakTime = 0;
    private static final long BREAK_DELAY = 50; // мс між ударами (більш швидко)
    private static BlockPos currentlyMiningBlock = null;
    private static int miningTicks = 0;
    
    enum State {
        IDLE, ANALYZING_BLOCKS, SELECTING_LOCATION, DRINKING_FIRE_RESISTANCE,
        MOVING_TO_CENTER, PLACING_TNT, TNT_PLACED, GOING_TO_HUB,
        WAITING_FOR_HUB_DELAY, EXECUTING_AN_COMMAND, SEARCHING_DEBRIS,
        COLLECTING_DEBRIS, REPEATING_POTION, ERROR_HUB
    }
    
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        if (shouldReturnToHub(player)) {
            returnToHub(player, "Помилка: невідповідні умови");
            return;
        }
        
        switch (currentState) {
            case IDLE:
                break;
            case ANALYZING_BLOCKS:
                analyzeNearbyBlocks(player);
                break;
            case SELECTING_LOCATION:
                selectBestLocation(player);
                break;
            case DRINKING_FIRE_RESISTANCE:
                drinkFireResistancePotion(player);
                break;
            case MOVING_TO_CENTER:
                moveToCenter(player);
                break;
            case PLACING_TNT:
                placeTNT(player);
                break;
            case TNT_PLACED:
                igniteAndGoToHub(player);
                break;
            case GOING_TO_HUB:
                executeCommand(player, "/hub");
                currentState = State.WAITING_FOR_HUB_DELAY;
                stateStartTime = System.currentTimeMillis();
                break;
            case WAITING_FOR_HUB_DELAY:
                if (System.currentTimeMillis() - stateStartTime >= HUB_DELAY_MS) {
                    executeCommand(player, "/an(" + ModConfig.anarchyNumber + ")");
                    currentState = State.SEARCHING_DEBRIS;
                }
                break;
            case SEARCHING_DEBRIS:
                searchForAncientDebris(player);
                break;
            case COLLECTING_DEBRIS:
                collectDebris(player);
                break;
            case REPEATING_POTION:
                checkAndRepeatPotion(player);
                break;
            case ERROR_HUB:
                returnToHub(player, "Помилка");
                break;
        }
    }
    
    public static void activate(ClientPlayerEntity player) {
        if (currentState != State.IDLE) {
            player.sendMessage(Text.literal("§cAncient Bot вже активований!"), false);
            return;
        }
        
        player.sendMessage(Text.literal("§a➤ Ancient Bot активований. Аналізую блоки..."), false);
        currentState = State.ANALYZING_BLOCKS;
        stateStartTime = System.currentTimeMillis();
        ancientDebrisLocations.clear();
        currentDebrisIndex = 0;
        
        // Reset analysis state
        analysisX = -SEARCH_RADIUS;
        analysisY = -SEARCH_RADIUS;
        analysisZ = -SEARCH_RADIUS;
        blockDensity.clear();
        analysisComplete = false;
        
        // Reset debris search state
        debrisSearchX = -50;
        debrisSearchZ = -50;
        debrisSearchY = 0;
        debrisSearchComplete = false;
        
        // Reset mining state
        miningPath.clear();
        currentMiningIndex = 0;
        lastBreakTime = 0;
        currentlyMiningBlock = null;
        miningTicks = 0;
    }
    
    public static void deactivate(ClientPlayerEntity player) {
        currentState = State.IDLE;
        targetCenter = null;
        potionStartTime = 0;
        
        // Reset analysis state
        analysisX = -SEARCH_RADIUS;
        analysisY = -SEARCH_RADIUS;
        analysisZ = -SEARCH_RADIUS;
        blockDensity.clear();
        analysisComplete = false;
        
        // Reset debris search state
        debrisSearchX = -50;
        debrisSearchZ = -50;
        debrisSearchY = 0;
        debrisSearchComplete = false;
        
        // Reset mining state
        miningPath.clear();
        currentMiningIndex = 0;
        lastBreakTime = 0;
        currentlyMiningBlock = null;
        miningTicks = 0;
        
        player.sendMessage(Text.literal("§cAncient Bot деактивований."), false);
    }
    
    private static int findInventorySlot(ClientPlayerEntity player, ItemStack target) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }
        return -1;
    }
    
    private static void analyzeNearbyBlocks(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Process one slice per tick to prevent freezing
        if (analysisY > SEARCH_RADIUS) {
            // Analysis complete
            player.sendMessage(Text.literal("§a✓ Аналіз завершен. Знайдено " + blockDensity.size() + " кандидатів."), false);
            
            if (blockDensity.isEmpty()) {
                player.sendMessage(Text.literal("§cПомилка: не знайдено блоків для аналізу!"), false);
                currentState = State.ERROR_HUB;
            } else {
                currentState = State.SELECTING_LOCATION;
            }
            
            // Reset analysis state
            analysisX = -SEARCH_RADIUS;
            analysisY = -SEARCH_RADIUS;
            analysisZ = -SEARCH_RADIUS;
            blockDensity.clear();
            return;
        }
        
        // Process one layer this tick
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                double dist = Math.sqrt(x*x + analysisY*analysisY + z*z);
                if (dist <= SEARCH_RADIUS + MAX_ERROR) {
                    BlockPos checkPos = playerPos.add(x, analysisY, z);
                    if (!world.getBlockState(checkPos).isAir()) {
                        BlockPos gridPos = new BlockPos(
                            (checkPos.getX() / 5) * 5,
                            (checkPos.getY() / 5) * 5,
                            (checkPos.getZ() / 5) * 5
                        );
                        blockDensity.put(gridPos, blockDensity.getOrDefault(gridPos, 0) + 1);
                    }
                }
            }
        }
        
        analysisY++;
    }
    
    private static void selectBestLocation(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        BlockPos bestLocation = null;
        int maxBlocks = 0;
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    double dist = Math.sqrt(x*x + y*y + z*z);
                    if (dist <= SEARCH_RADIUS + MAX_ERROR) {
                        BlockPos checkPos = playerPos.add(x, y, z);
                        if (!world.getBlockState(checkPos).isAir()) {
                            int blockCount = countBlocksInSphere(world, checkPos, 5);
                            if (blockCount > maxBlocks) {
                                maxBlocks = blockCount;
                                bestLocation = checkPos;
                            }
                        }
                    }
                }
            }
        }
        
        if (bestLocation != null) {
            targetCenter = bestLocation;
            player.sendMessage(
                Text.literal("§a✓ Найкраще місце обрано: " + bestLocation.toShortString() + " (блоків: " + maxBlocks + ")"),
                false
            );
            currentState = State.DRINKING_FIRE_RESISTANCE;
        } else {
            player.sendMessage(Text.literal("§cПомилка: не знайдено подходящего місця!"), false);
            currentState = State.ERROR_HUB;
        }
    }
    
    private static int countBlocksInSphere(World world, BlockPos center, int radius) {
        int count = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.sqrt(x*x + y*y + z*z) <= radius) {
                        if (!world.getBlockState(center.add(x, y, z)).isAir()) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }
    
    private static void drinkFireResistancePotion(ClientPlayerEntity player) {
        ItemStack fireResistancePotion = findPotionInInventory(player);
        
        if (fireResistancePotion == null || fireResistancePotion.isEmpty()) {
            player.sendMessage(Text.literal("§cПомилка: зілля вогнестійкості не знайдено!"), false);
            currentState = State.ERROR_HUB;
            return;
        }
        
        int slotIndex = findInventorySlot(player, fireResistancePotion);
        if (slotIndex != -1) {
            player.getInventory().selectedSlot = slotIndex;
            MinecraftClient.getInstance().interactionManager.clickCreativeStack(fireResistancePotion, 1);
        }
        
        potionStartTime = System.currentTimeMillis();
        player.sendMessage(Text.literal("§a✓ Зілля вогнестійкості випито."), false);
        currentState = State.MOVING_TO_CENTER;
        stateStartTime = System.currentTimeMillis();
    }
    
    private static ItemStack findPotionInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.POTION) {
                return stack;
            }
        }
        return null;
    }
    
    private static void moveToCenter(ClientPlayerEntity player) {
        if (targetCenter == null) return;
        
        Vec3d targetVec = Vec3d.ofCenter(targetCenter);
        Vec3d playerVec = player.getPos();
        double dist = playerVec.distanceTo(targetVec);
        
        if (dist < 1.5) {
            player.sendMessage(Text.literal("§a✓ Прибув в центр."), false);
            currentState = State.PLACING_TNT;
            miningPath.clear();
            currentMiningIndex = 0;
            currentlyMiningBlock = null;
            miningTicks = 0;
        } else {
            // Build and execute mining path
            if (miningPath.isEmpty()) {
                buildMiningPath(player, targetVec);
            }
            
            if (!miningPath.isEmpty()) {
                mineThroughBlocks(player, targetVec);
            }
        }
    }
    
    private static void buildMiningPath(ClientPlayerEntity player, Vec3d targetVec) {
        Vec3d playerVec = player.getPos();
        BlockPos playerPos = player.getBlockPos();
        BlockPos targetPos = BlockPos.ofFloored(targetVec);
        
        miningPath.clear();
        
        // Сканувати яких блоків треба копати на маршруті до центру
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3d pointOnPath = playerVec.lerp(targetVec, t);
            BlockPos checkPos = BlockPos.ofFloored(pointOnPath);
            
            // Перевірити куб 3x3x3 навколо маршруту
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos blockToCheck = checkPos.add(dx, dy, dz);
                        if (!player.getWorld().getBlockState(blockToCheck).isAir() &&
                            !isSolidUnbreakable(player, blockToCheck)) {
                            // Показати блок на карті (частинкою або лог)
                            player.sendMessage(
                                Text.literal("§6[Копання] Блок на: " + blockToCheck.toShortString()),
                                true // Actionbar - миготить повідомлення
                            );
                            miningPath.add(blockToCheck);
                        }
                    }
                }
            }
        }
        
        // Видалити дублікати
        List<BlockPos> uniquePath = new ArrayList<>();
        for (BlockPos pos : miningPath) {
            if (!uniquePath.contains(pos)) {
                uniquePath.add(pos);
            }
        }
        miningPath = uniquePath;
        
        if (!miningPath.isEmpty()) {
            player.sendMessage(Text.literal("§e→ Знайдено " + miningPath.size() + " блоків для копання"), false);
        }
    }
    
    private static void mineThroughBlocks(ClientPlayerEntity player, Vec3d targetVec) {
        if (currentMiningIndex >= miningPath.size()) {
            currentMiningIndex = 0;
            miningPath.clear();
            currentlyMiningBlock = null;
            miningTicks = 0;
            
            // Продовжити рух до центру
            Vec3d playerVec = player.getPos();
            Vec3d direction = targetVec.subtract(playerVec).normalize();
            double moveSpeed = 0.08;
            player.setVelocity(direction.multiply(moveSpeed));
            return;
        }
        
        BlockPos targetBlock = miningPath.get(currentMiningIndex);
        World world = player.getWorld();
        
        // Перевірити, чи блок вже розбитий
        if (world.getBlockState(targetBlock).isAir()) {
            currentMiningIndex++;
            currentlyMiningBlock = null;
            miningTicks = 0;
            return;
        }
        
        // Якщо це новий блок, почати копати його
        if (!targetBlock.equals(currentlyMiningBlock)) {
            currentlyMiningBlock = targetBlock;
            miningTicks = 0;
        }
        
        // Вибрати кайл з інвентарю
        selectPickaxeInInventory(player);
        
        // Визначити напрямок для атаки
        net.minecraft.util.math.Direction breakDirection = getBreakDirection(player.getBlockPos(), targetBlock);
        
        // Постійно атакувати блок без затримки
        MinecraftClient.getInstance().interactionManager.attackBlock(targetBlock, breakDirection);
        player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        
        miningTicks++;
        
        // Показати гравцю, який блок копаємо (actionbar)
        player.sendMessage(
            Text.literal("§c⛏ Копаю: " + targetBlock.toShortString() + " [" + (currentMiningIndex + 1) + "/" + miningPath.size() + "] (" + miningTicks + " тиків)"),
            true
        );
        
        // Якщо копаємо занадто довго - перейти до наступного блока
        if (miningTicks > 300) { // 15 секунд на один блок (300 тиків на 20 TPS)
            currentMiningIndex++;
            currentlyMiningBlock = null;
            miningTicks = 0;
        }
    }
    
    private static net.minecraft.util.math.Direction getBreakDirection(BlockPos fromPos, BlockPos targetPos) {
        int dx = Integer.compare(targetPos.getX(), fromPos.getX());
        int dy = Integer.compare(targetPos.getY(), fromPos.getY());
        int dz = Integer.compare(targetPos.getZ(), fromPos.getZ());
        
        if (Math.abs(dy) > Math.abs(dx) && Math.abs(dy) > Math.abs(dz)) {
            return dy > 0 ? net.minecraft.util.math.Direction.UP : net.minecraft.util.math.Direction.DOWN;
        } else if (Math.abs(dz) > Math.abs(dx)) {
            return dz > 0 ? net.minecraft.util.math.Direction.SOUTH : net.minecraft.util.math.Direction.NORTH;
        } else {
            return dx > 0 ? net.minecraft.util.math.Direction.EAST : net.minecraft.util.math.Direction.WEST;
        }
    }
    
    private static void selectPickaxeInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.PickaxeItem) {
                player.getInventory().selectedSlot = i;
                return;
            }
        }
    }
    
    private static boolean isSolidUnbreakable(ClientPlayerEntity player, BlockPos pos) {
        World world = player.getWorld();
        net.minecraft.block.Block block = world.getBlockState(pos).getBlock();
        
        // Блоки, які не можна копати
        return block == Blocks.BEDROCK || 
               block == Blocks.OBSIDIAN;
    }
    
    private static void breakBlocksInDirection(ClientPlayerEntity player, Vec3d targetVec) {
        Vec3d playerVec = player.getPos();
        Vec3d direction = targetVec.subtract(playerVec).normalize();
        
        // Check and break blocks in a 3x3x3 cube in front of the player
        for (double offset = 0.5; offset <= 3.0; offset += 0.5) {
            Vec3d checkVec = playerVec.add(direction.multiply(offset));
            BlockPos checkPos = BlockPos.ofFloored(checkVec);
            
            World world = player.getWorld();
            if (!world.getBlockState(checkPos).isAir()) {
                // Try to break the block
                MinecraftClient.getInstance().interactionManager.attackBlock(checkPos, net.minecraft.util.math.Direction.UP);
            }
        }
    }
    
    private static void placeTNT(ClientPlayerEntity player) {
        if (targetCenter == null) return;
        
        ItemStack tnt = findTNTInInventory(player);
        
        if (tnt == null || tnt.isEmpty()) {
            player.sendMessage(Text.literal("§cПомилка: TNT не знайдено!"), false);
            currentState = State.ERROR_HUB;
            return;
        }
        
        int slotIndex = findInventorySlot(player, tnt);
        if (slotIndex != -1) {
            player.getInventory().selectedSlot = slotIndex;
            MinecraftClient.getInstance().interactionManager.clickCreativeStack(tnt, 0);
        }
        
        player.sendMessage(Text.literal("§a✓ TNT розміщено в центрі."), false);
        currentState = State.TNT_PLACED;
    }
    
    private static ItemStack findTNTInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TNT) {
                return stack;
            }
        }
        return null;
    }
    
    private static void igniteAndGoToHub(ClientPlayerEntity player) {
        ItemStack flintAndSteel = findFlintAndSteelInInventory(player);
        
        if (flintAndSteel == null || flintAndSteel.isEmpty()) {
            player.sendMessage(Text.literal("§cПомилка: кресало не знайдено!"), false);
            currentState = State.ERROR_HUB;
            return;
        }
        
        int slotIndex = findInventorySlot(player, flintAndSteel);
        if (slotIndex != -1) {
            player.getInventory().selectedSlot = slotIndex;
            MinecraftClient.getInstance().interactionManager.clickCreativeStack(flintAndSteel, 1);
        }
        
        player.sendMessage(Text.literal("§a✓ TNT розпалено!"), false);
        currentState = State.GOING_TO_HUB;
    }
    
    private static ItemStack findFlintAndSteelInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.FLINT_AND_STEEL) {
                return stack;
            }
        }
        return null;
    }
    
    private static void searchForAncientDebris(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Reset on first call
        if (debrisSearchX == -50 && debrisSearchZ == -50 && debrisSearchY == 0) {
            ancientDebrisLocations.clear();
        }
        
        // Search one X-column per tick to prevent lag (50 block search radius)
        int searchLimit = 50;
        
        for (int z = -searchLimit; z <= searchLimit; z++) {
            for (int y = 0; y < 256; y++) {
                BlockPos checkPos = new BlockPos(playerPos.getX() + debrisSearchX, y, playerPos.getZ() + z);
                if (world.getBlockState(checkPos).getBlock() == Blocks.ANCIENT_DEBRIS) {
                    if (isExposedBlock(world, checkPos)) {
                        ancientDebrisLocations.add(checkPos);
                    }
                }
            }
        }
        
        debrisSearchX++;
        
        // Search complete when we've covered the radius
        if (debrisSearchX > searchLimit) {
            if (ancientDebrisLocations.isEmpty()) {
                player.sendMessage(Text.literal("§cДавні обломки не знайдені. Шукаю нове місце для підриву..."), false);
                currentState = State.ANALYZING_BLOCKS;
            } else {
                ancientDebrisLocations.sort((a, b) -> 
                    Double.compare(a.getSquaredDistance(playerPos), b.getSquaredDistance(playerPos))
                );
                
                player.sendMessage(
                    Text.literal("§a✓ Знайдено " + ancientDebrisLocations.size() + " древніх обломків."),
                    false
                );
                
                currentDebrisIndex = 0;
                currentState = State.COLLECTING_DEBRIS;
            }
            
            // Reset search state
            debrisSearchX = -searchLimit;
            debrisSearchZ = -searchLimit;
            debrisSearchY = 0;
        }
    }
    
    private static boolean isExposedBlock(World world, BlockPos pos) {
        for (int i = 0; i < 6; i++) {
            BlockPos neighbor = pos.offset(switch(i) {
                case 0 -> net.minecraft.util.math.Direction.UP;
                case 1 -> net.minecraft.util.math.Direction.DOWN;
                case 2 -> net.minecraft.util.math.Direction.NORTH;
                case 3 -> net.minecraft.util.math.Direction.SOUTH;
                case 4 -> net.minecraft.util.math.Direction.EAST;
                case 5 -> net.minecraft.util.math.Direction.WEST;
                default -> net.minecraft.util.math.Direction.UP;
            });
            
            if (world.getBlockState(neighbor).isAir() || !world.getFluidState(neighbor).isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    private static void collectDebris(ClientPlayerEntity player) {
        if (currentDebrisIndex >= ancientDebrisLocations.size()) {
            player.sendMessage(Text.literal("§a✓ Всі давні обломки зібрані!"), false);
            currentState = State.ANALYZING_BLOCKS;
            miningPath.clear();
            currentMiningIndex = 0;
            currentlyMiningBlock = null;
            miningTicks = 0;
            return;
        }
        
        BlockPos target = ancientDebrisLocations.get(currentDebrisIndex);
        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d playerVec = player.getPos();
        double dist = playerVec.distanceTo(targetVec);
        
        if (dist < 1.5) {
            player.sendMessage(Text.literal("§6Збираю обломок " + (currentDebrisIndex + 1) + "/" + ancientDebrisLocations.size()), false);
            currentDebrisIndex++;
            miningPath.clear();
            currentMiningIndex = 0;
            currentlyMiningBlock = null;
            miningTicks = 0;
        } else {
            // Build and execute mining path
            if (miningPath.isEmpty()) {
                buildMiningPath(player, targetVec);
            }
            
            if (!miningPath.isEmpty()) {
                mineThroughBlocks(player, targetVec);
            }
        }
        
        checkAndRepeatPotion(player);
    }
    
    private static void checkAndRepeatPotion(ClientPlayerEntity player) {
        long elapsed = System.currentTimeMillis() - potionStartTime;
        long timeRemaining = 180000 - elapsed;
        
        if (timeRemaining <= POTION_DURATION_MS && timeRemaining > 0) {
            if (currentState == State.COLLECTING_DEBRIS) {
                drinkFireResistancePotion(player);
            }
        }
    }
    
    private static void executeCommand(ClientPlayerEntity player, String command) {
        player.networkHandler.sendChatCommand(command.substring(1));
        player.sendMessage(Text.literal("§6Команда: " + command), false);
    }
    
    private static boolean shouldReturnToHub(ClientPlayerEntity player) {
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) == null && 
            currentState != State.IDLE && currentState != State.ANALYZING_BLOCKS && 
            currentState != State.GOING_TO_HUB && currentState != State.WAITING_FOR_HUB_DELAY) {
            return true;
        }
        
        if (!hasPickaxeInInventory(player) && currentState == State.COLLECTING_DEBRIS) {
            return true;
        }
        
        ItemStack flintAndSteel = findFlintAndSteelInInventory(player);
        if ((flintAndSteel == null || flintAndSteel.isDamaged()) && currentState == State.TNT_PLACED) {
            return true;
        }
        
        if (findTNTInInventory(player) == null && currentState == State.PLACING_TNT) {
            return true;
        }
        
        if (isPlayerNearby(player, NEARBY_PLAYER_RADIUS) && currentState != State.IDLE) {
            if (System.currentTimeMillis() - stateStartTime > 120000) {
                executeCommand(player, "/an(" + ModConfig.anarchyNumber + ")");
            }
            return true;
        }
        
        return false;
    }
    
    private static boolean hasPickaxeInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.PickaxeItem) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean isPlayerNearby(ClientPlayerEntity player, int radius) {
        return false;
    }
    
    private static void returnToHub(ClientPlayerEntity player, String reason) {
        player.sendMessage(Text.literal("§c" + reason + " - повертаюсь у /hub"), false);
        executeCommand(player, "/hub");
        currentState = State.IDLE;
    }
    
    public static boolean isActive() {
        return currentState != State.IDLE;
    }
    
    public static State getCurrentState() {
        return currentState;
    }
}
