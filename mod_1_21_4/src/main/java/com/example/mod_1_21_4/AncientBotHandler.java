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
 * AncientBot v2.0 - Hit & Run система для видобування ancient_debris
 * Алгоритм: Сканування → Підготовка (баф) → Підрив → Евакуація → Повернення → Збір → Цикл
 */
public class AncientBotHandler {
    private static final int SCAN_RADIUS = 17;
    private static final int TOLERANCE = 5;
    private static final long POTION_WARNING_TIME = 10000; // 10 сек перед кінцем
    private static final long HUB_DELAY = 5000; // 5 сек очікування в хабі
    private static final long PLAYER_PAUSE_TIME = 120000; // 2 хвилини паузи при виявленні гравця
    
    enum BotState {
        IDLE,                    // Неактивний
        SCANNING,                // Сканування координат
        MOVING_TO_BLAST_CENTER,  // Рух до центру підриву
        BUFFING,                 // Пиття зілля
        PLACING_EXPLOSIVE,       // Встановлення вибухівки
        IGNITING,                // Запалення вибухівки
        EVACUATING,              // Євакуація (/hub)
        WAITING_IN_HUB,          // Очікування в хабі
        RETURNING,               // Повернення на місце (/an)
        SCANNING_DEBRIS,         // Сканування обломків
        COLLECTING,              // Збір обломків
        EMERGENCY_EXIT           // Аварійна євакуація
    }
    
    // === Стан боту ===
    private static BotState currentState = BotState.IDLE;
    private static long stateStartTime = 0;
    private static long potionStartTime = 0;
    private static long pauseStartTime = 0;
    
    // === Евакуація - причини та інформація ===
    private static String lastExitReason = "Невідомо";
    private static long lastExitTime = 0;
    private static BotState lastExitState = BotState.IDLE;
    
    // === Координати та маршрути ===
    private static BlockPos blastCenter = null;
    private static List<BlockPos> debrisLocations = new ArrayList<>();
    private static List<BlockPos> collectionPath = new ArrayList<>();
    private static int collectionIndex = 0;
    
    // === Сканування ===
    private static int scanProgress = 0;
    private static Map<BlockPos, Integer> blockDensityMap = new HashMap<>();
    
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null) return;
        
        // Перевіра для аварійної евакуації
        if (shouldEmergencyExit(player)) {
            if (currentState != BotState.EMERGENCY_EXIT) {
                emergencyExit(player, "Критична умова!");
            }
            return;
        }
        
        // Основна статус-машина
        switch (currentState) {
            case IDLE:
                // Чекаємо на активацію
                break;
                
            case SCANNING:
                scanningTick(player);
                break;
                
            case MOVING_TO_BLAST_CENTER:
                moveToBlastCenterTick(player);
                break;
                
            case BUFFING:
                bufferingTick(player);
                break;
                
            case PLACING_EXPLOSIVE:
                placingExplosiveTick(player);
                break;
                
            case IGNITING:
                ignitingTick(player);
                break;
                
            case EVACUATING:
                evacuatingTick(player);
                break;
                
            case WAITING_IN_HUB:
                waitingInHubTick(player);
                break;
                
            case RETURNING:
                returningTick(player);
                break;
                
            case SCANNING_DEBRIS:
                scanningDebrisTick(player);
                break;
                
            case COLLECTING:
                collectingTick(player);
                break;
                
            case EMERGENCY_EXIT:
                // Чекаємо на рішення гравця
                break;
        }
    }
    
    public static void activate(ClientPlayerEntity player) {
        if (currentState != BotState.IDLE) {
            player.sendMessage(Text.literal("§cAncient Bot вже активний!"), false);
            return;
        }
        
        // Перевірка всіх необхідних ресурсів
        cheker4ab.ResourceCheckResult checkResult = cheker4ab.checkAllResources(player);
        
        if (!checkResult.isAllAvailable()) {
            player.sendMessage(Text.literal("§c⚠ БРАКУ РЕСУРСІВ!"), false);
            cheker4ab.sendDetailedReport(player, checkResult);
            return;
        }
        
        player.sendMessage(Text.literal("§a➤ Ancient Bot активований. Починаю сканування..."), false);
        
        currentState = BotState.SCANNING;
        stateStartTime = System.currentTimeMillis();
        blockDensityMap.clear();
        scanProgress = 0;
        blastCenter = null;
        debrisLocations.clear();
        collectionPath.clear();
        collectionIndex = 0;
    }
    
    public static void deactivate(ClientPlayerEntity player) {
        currentState = BotState.IDLE;
        stateStartTime = 0;
        potionStartTime = 0;
        pauseStartTime = 0;
        blockDensityMap.clear();
        debrisLocations.clear();
        collectionPath.clear();
        
        player.sendMessage(Text.literal("§cAncient Bot деактивований."), false);
    }
    
    // ==================== ФАЗА 1: СКАНУВАННЯ ====================
    
    private static void scanningTick(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        // Сканувати по одному Y-слою за тик
        int y = -SCAN_RADIUS + scanProgress;
        
        if (y > SCAN_RADIUS) {
            // Сканування завершено
            selectBlastCenter(player);
            return;
        }
        
        // Сканувати куб на висоті y
        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                double dist = Math.sqrt(x*x + y*y + z*z);
                
                if (dist <= SCAN_RADIUS + TOLERANCE) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (!world.getBlockState(checkPos).isAir()) {
                        // Зберігати гріді 5x5x5
                        BlockPos gridPos = new BlockPos(
                            (checkPos.getX() / 5) * 5,
                            (checkPos.getY() / 5) * 5,
                            (checkPos.getZ() / 5) * 5
                        );
                        blockDensityMap.put(gridPos, blockDensityMap.getOrDefault(gridPos, 0) + 1);
                    }
                }
            }
        }
        
        scanProgress++;
    }
    
    private static void selectBlastCenter(ClientPlayerEntity player) {
        BlockPos best = null;
        int maxDensity = 0;
        
        for (Map.Entry<BlockPos, Integer> entry : blockDensityMap.entrySet()) {
            if (entry.getValue() > maxDensity) {
                maxDensity = entry.getValue();
                best = entry.getKey();
            }
        }
        
        if (best != null) {
            blastCenter = best;
            player.sendMessage(
                Text.literal("§a✓ Центр підриву обраний: " + best.toShortString() + " (щільність: " + maxDensity + ")"),
                false
            );
            
            currentState = BotState.MOVING_TO_BLAST_CENTER;
            stateStartTime = System.currentTimeMillis();
        } else {
            player.sendMessage(Text.literal("§cПомилка: не знайдено подходящої точки!"), false);
            currentState = BotState.IDLE;
        }
    }
    
    // ==================== ФАЗА 2: РУХ ДО ЦЕНТРУ ====================
    
    private static void moveToBlastCenterTick(ClientPlayerEntity player) {
        if (blastCenter == null) return;
        
        Vec3d playerPos = player.getPos();
        Vec3d targetPos = Vec3d.ofCenter(blastCenter);
        double distance = playerPos.distanceTo(targetPos);
        
        if (distance < 1.5) {
            // Прибули на місце
            player.sendMessage(Text.literal("§a✓ Прибув до центру."), false);
            currentState = BotState.BUFFING;
            stateStartTime = System.currentTimeMillis();
            return;
        }
        
        // Рух до центру
        Vec3d direction = targetPos.subtract(playerPos).normalize();
        player.setVelocity(direction.multiply(0.15));
        player.velocityDirty = true;
    }
    
    // ==================== ФАЗА 3: БУФУВАННЯ (ЗІЛЛЯ) ====================
    
    private static void bufferingTick(ClientPlayerEntity player) {
        // Перевірка наявності Fire Resistance
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) != null) {
            // Вже є баф, переходимо далі
            player.sendMessage(Text.literal("§a✓ Fire Resistance активний."), false);
            currentState = BotState.PLACING_EXPLOSIVE;
            stateStartTime = System.currentTimeMillis();
            return;
        }
        
        // Шукаємо зілля у інвентарі
        ItemStack potion = findPotionInInventory(player);
        if (potion == null || potion.isEmpty()) {
            player.sendMessage(Text.literal("§c⚠ Fire Resistance зілля не знайдено!"), false);
            emergencyExit(player, "Немає бафу");
            return;
        }
        
        // Пиємо зілля
        int slot = findInventorySlot(player, potion);
        if (slot != -1) {
            player.getInventory().selectedSlot = slot;
            player.setCurrentHand(net.minecraft.util.Hand.MAIN_HAND);
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            
            potionStartTime = System.currentTimeMillis();
            player.sendMessage(Text.literal("§6⚗ Випиваю Fire Resistance..."), false);
        }
    }
    
    // ==================== ФАЗА 4: ВСТАНОВЛЕННЯ ВИБУХІВКИ ====================
    
    private static void placingExplosiveTick(ClientPlayerEntity player) {
        if (blastCenter == null) return;
        
        // Перевірка наявності вибухівки
        ItemStack explosive = findExplosiveInInventory(player);
        if (explosive == null || explosive.isEmpty()) {
            player.sendMessage(Text.literal("§c⚠ Вибухівка не знайдена!"), false);
            emergencyExit(player, "Немає боєприпасів");
            return;
        }
        
        // Встановлюємо вибухівку у центр
        int slot = findInventorySlot(player, explosive);
        if (slot != -1) {
            player.getInventory().selectedSlot = slot;
            
            // Ставимо TNT у центр
            net.minecraft.util.math.Direction direction = net.minecraft.util.math.Direction.UP;
            Vec3d hitVec = Vec3d.ofCenter(blastCenter);
            
            MinecraftClient.getInstance().interactionManager.interactBlock(
                player,
                net.minecraft.util.Hand.MAIN_HAND,
                new net.minecraft.util.hit.BlockHitResult(hitVec, direction, blastCenter, false)
            );
            
            player.sendMessage(Text.literal("§6💣 Встановив вибухівку."), false);
            currentState = BotState.IGNITING;
            stateStartTime = System.currentTimeMillis();
        }
    }
    
    // ==================== ФАЗА 5: ЗАПАЛЕННЯ ====================
    
    private static void ignitingTick(ClientPlayerEntity player) {
        // Перевірка наявності кресала
        ItemStack flintAndSteel = findFlintAndSteelInInventory(player);
        if (flintAndSteel == null || flintAndSteel.isEmpty() || flintAndSteel.isDamaged()) {
            player.sendMessage(Text.literal("§c⚠ Кресало зламалось або відсутнє!"), false);
            emergencyExit(player, "Зламалось кресало");
            return;
        }
        
        // Запалюємо
        int slot = findInventorySlot(player, flintAndSteel);
        if (slot != -1) {
            player.getInventory().selectedSlot = slot;
            
            net.minecraft.util.math.Direction direction = net.minecraft.util.math.Direction.UP;
            Vec3d hitVec = Vec3d.ofCenter(blastCenter);
            
            MinecraftClient.getInstance().interactionManager.interactBlock(
                player,
                net.minecraft.util.Hand.MAIN_HAND,
                new net.minecraft.util.hit.BlockHitResult(hitVec, direction, blastCenter, false)
            );
            
            player.sendMessage(Text.literal("§6🔥 Запалив вибухівку!"), false);
            currentState = BotState.EVACUATING;
            stateStartTime = System.currentTimeMillis();
        }
    }
    
    // ==================== ФАЗА 6: ЕВАКУАЦІЯ ====================
    
    private static void evacuatingTick(ClientPlayerEntity player) {
        // Зберігаємо інформацію про евакуацію
        lastExitReason = "Нормальна евакуація після підриву";
        lastExitTime = System.currentTimeMillis();
        lastExitState = BotState.EVACUATING;
        
        // Виконуємо /hub команду
        player.networkHandler.sendCommand("hub");
        player.sendMessage(Text.literal("§c🏃 ЕВАКУАЦІЯ!"), false);
        
        currentState = BotState.WAITING_IN_HUB;
        stateStartTime = System.currentTimeMillis();
    }
    
    // ==================== ФАЗА 7: ОЧІКУВАННЯ В ХАБІ ====================
    
    private static void waitingInHubTick(ClientPlayerEntity player) {
        long elapsed = System.currentTimeMillis() - stateStartTime;
        
        if (elapsed >= HUB_DELAY) {
            // Повертаємось
            player.networkHandler.sendCommand("an " + ModConfig.anarchyNumber);
            player.sendMessage(Text.literal("§a🔙 Повертаюсь на місце вибуху..."), false);
            
            currentState = BotState.RETURNING;
            stateStartTime = System.currentTimeMillis();
        }
    }
    
    // ==================== ФАЗА 8: ПОВЕРНЕННЯ ====================
    
    private static void returningTick(ClientPlayerEntity player) {
        // Просто чекаємо, поки телепортація завершиться (~2-3 тика)
        long elapsed = System.currentTimeMillis() - stateStartTime;
        
        if (elapsed >= 1000) { // 1 сек для телепортації
            currentState = BotState.SCANNING_DEBRIS;
            stateStartTime = System.currentTimeMillis();
        }
    }
    
    // ==================== ФАЗА 9: СКАНУВАННЯ ОБЛОМКІВ ====================
    
    private static void scanningDebrisTick(ClientPlayerEntity player) {
        World world = player.getWorld();
        BlockPos playerPos = player.getBlockPos();
        
        debrisLocations.clear();
        
        // Шукаємо всі ancient_debris у радіусі 50 блоків
        for (int x = -50; x <= 50; x++) {
            for (int z = -50; z <= 50; z++) {
                for (int y = 0; y < 256; y++) {
                    BlockPos checkPos = playerPos.add(x, y, z);
                    
                    if (world.getBlockState(checkPos).getBlock() == Blocks.ANCIENT_DEBRIS) {
                        // Перевірка, чи блок видно
                        if (isExposedBlock(world, checkPos)) {
                            debrisLocations.add(checkPos);
                        }
                    }
                }
            }
        }
        
        if (debrisLocations.isEmpty()) {
            player.sendMessage(Text.literal("§6✓ Всі обломки зібрані! Перезапускаю..."), false);
            currentState = BotState.SCANNING;
            stateStartTime = System.currentTimeMillis();
            blockDensityMap.clear();
            scanProgress = 0;
        } else {
            // Сортуємо по відстані
            debrisLocations.sort((a, b) -> 
                Double.compare(a.getSquaredDistance(playerPos), b.getSquaredDistance(playerPos))
            );
            
            // Обчислюємо TSP маршрут
            collectionPath = calculateTSPPath(debrisLocations);
            collectionIndex = 0;
            
            player.sendMessage(
                Text.literal("§a✓ Знайдено " + debrisLocations.size() + " обломків. Збираю..."),
                false
            );
            
            currentState = BotState.COLLECTING;
            stateStartTime = System.currentTimeMillis();
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
    
    // ==================== ФАЗА 10: ЗБІР ОБЛОМКІВ ====================
    
    private static void collectingTick(ClientPlayerEntity player) {
        if (collectionIndex >= collectionPath.size()) {
            // Збір завершен, скануємо знову
            player.sendMessage(Text.literal("§6✓ Цикл завершен. Сканую нове місце..."), false);
            currentState = BotState.SCANNING;
            stateStartTime = System.currentTimeMillis();
            blockDensityMap.clear();
            scanProgress = 0;
            return;
        }
        
        BlockPos target = collectionPath.get(collectionIndex);
        Vec3d targetVec = Vec3d.ofCenter(target);
        Vec3d playerVec = player.getPos();
        double distance = playerVec.distanceTo(targetVec);
        
        if (distance < 1.5) {
            // Прибули до блоку, розбиваємо його
            net.minecraft.util.math.Direction direction = net.minecraft.util.math.Direction.UP;
            MinecraftClient.getInstance().interactionManager.attackBlock(target, direction);
            player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            
            collectionIndex++;
        } else {
            // Рухаємось до блоку
            Vec3d direction = targetVec.subtract(playerVec).normalize();
            player.setVelocity(direction.multiply(0.15));
            player.velocityDirty = true;
        }
    }
    
    // ==================== УТИЛІТИ ====================
    
    private static ItemStack findPotionInInventory(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.POTION) {
                return stack;
            }
        }
        return null;
    }
    
    private static ItemStack findExplosiveInInventory(ClientPlayerEntity player) {
        // Шукаємо TNT, ліжка, або інші вибухівки
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TNT) {
                return stack;
            }
        }
        return null;
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
    
    private static int findInventorySlot(ClientPlayerEntity player, ItemStack target) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                return i;
            }
        }
        return -1;
    }
    
    private static List<BlockPos> calculateTSPPath(List<BlockPos> debris) {
        // Простий жадібний алгоритм (not perfect TSP, но швидко)
        List<BlockPos> path = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        
        if (debris.isEmpty()) return path;
        
        // Почати з першого
        int current = 0;
        path.add(debris.get(current));
        visited.add(current);
        
        // Жадібно додавати найближчий
        while (visited.size() < debris.size()) {
            BlockPos currentPos = debris.get(current);
            int nearest = -1;
            double minDist = Double.MAX_VALUE;
            
            for (int i = 0; i < debris.size(); i++) {
                if (!visited.contains(i)) {
                    double dist = currentPos.getSquaredDistance(debris.get(i));
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = i;
                    }
                }
            }
            
            if (nearest != -1) {
                path.add(debris.get(nearest));
                visited.add(nearest);
                current = nearest;
            }
        }
        
        return path;
    }
    
    // ==================== ЗАХИСНІ ТРИГЕРИ ====================
    
    private static boolean shouldEmergencyExit(ClientPlayerEntity player) {
        // Умова 1: Немає Fire Resistance
        if (player.getStatusEffect(StatusEffects.FIRE_RESISTANCE) == null &&
            currentState != BotState.IDLE &&
            currentState != BotState.SCANNING &&
            currentState != BotState.EVACUATING &&
            currentState != BotState.WAITING_IN_HUB) {
            return true;
        }
        
        // Умова 2: Зламалось кресало
        if (currentState == BotState.IGNITING) {
            ItemStack fs = findFlintAndSteelInInventory(player);
            if (fs == null || fs.isDamaged()) {
                return true;
            }
        }
        
        // Умова 3: Немає кирки
        if (currentState == BotState.COLLECTING) {
            boolean hasPickaxe = false;
            for (int i = 0; i < player.getInventory().size(); i++) {
                if (player.getInventory().getStack(i).getItem() instanceof net.minecraft.item.PickaxeItem) {
                    hasPickaxe = true;
                    break;
                }
            }
            if (!hasPickaxe) {
                return true;
            }
        }
        
        // Умова 4: Немає вибухівки
        if (currentState == BotState.PLACING_EXPLOSIVE) {
            if (findExplosiveInInventory(player) == null) {
                return true;
            }
        }
        
        return false;
    }
    
    private static void emergencyExit(ClientPlayerEntity player, String reason) {
        // Зберігаємо інформацію про евакуацію
        lastExitReason = reason;
        lastExitTime = System.currentTimeMillis();
        lastExitState = currentState;
        
        currentState = BotState.EMERGENCY_EXIT;
        player.networkHandler.sendCommand("hub");
        player.sendMessage(Text.literal("§c🚨 АВАРІЙНА ЕВАКУАЦІЯ: " + reason), false);
        
        // Пауза на 2 хвилини
        pauseStartTime = System.currentTimeMillis();
    }
    
    public static boolean isActive() {
        return currentState != BotState.IDLE && currentState != BotState.EMERGENCY_EXIT;
    }
    
    public static BotState getCurrentState() {
        return currentState;
    }
    
    /**
     * Показує інформацію про останню евакуацію
     */
    public static void whyexit(ClientPlayerEntity player) {
        if (lastExitTime == 0) {
            player.sendMessage(Text.literal("§6Немає інформації про евакуацію"), false);
            return;
        }
        
        // Форматуємо час
        long timeSinceExit = System.currentTimeMillis() - lastExitTime;
        String timeStr = formatTime(timeSinceExit);
        
        // Виводимо детальну інформацію
        player.sendMessage(Text.literal("§6=== ІНФОРМАЦІЯ ПРО ОСТАННЮ ЕВАКУАЦІЮ ==="), false);
        player.sendMessage(Text.literal("§aPричина: §f" + lastExitReason), false);
        player.sendMessage(Text.literal("§aЗ якого стану: §f" + lastExitState.toString()), false);
        player.sendMessage(Text.literal("§aЧас назад: §f" + timeStr), false);
        player.sendMessage(Text.literal("§6===================================="), false);
    }
    
    /**
     * Форматує час в читаний вигляд
     */
    private static String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + " сек назад";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " хв назад";
        } else {
            long hours = seconds / 3600;
            return hours + " год назад";
        }
    }
}
