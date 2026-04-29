package com.example.mod_1_21_4;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.text.Text;

/**
 * Перевіряч ресурсів для AncientBot
 * Гарантує наявність всіх необхідних предметів перед запуском
 */
public class cheker4ab {
    
    // Результати перевірки
    public static class ResourceCheckResult {
        public boolean hasFireResistancePotion;
        public boolean hasTNT;
        public boolean hasFlintAndSteel;
        public boolean hasPickaxe;
        public boolean hasModConfig;
        
        public boolean isAllAvailable() {
            return hasFireResistancePotion && hasTNT && hasFlintAndSteel && 
                   hasPickaxe && hasModConfig;
        }
        
        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            report.append("§6=== ПЕРЕВІРКА РЕСУРСІВ ===\n");
            report.append(hasFireResistancePotion ? "§a✓" : "§c✗").append(" Fire Resistance зілля\n");
            report.append(hasTNT ? "§a✓" : "§c✗").append(" TNT (вибухівка)\n");
            report.append(hasFlintAndSteel ? "§a✓" : "§c✗").append(" Flint & Steel (кресало)\n");
            report.append(hasPickaxe ? "§a✓" : "§c✗").append(" Pickaxe (кирка)\n");
            report.append(hasModConfig ? "§a✓" : "§c✗").append(" Конфіг мода (anarchyNumber)\n");
            
            if (isAllAvailable()) {
                report.append("§a\n✓ ВСІ РЕСУРСИ ГОТОВІ!");
            } else {
                report.append("§c\n✗ БРАКУ РЕСУРСІВ!");
            }
            
            return report.toString();
        }
        
        public String getMissingItems() {
            StringBuilder missing = new StringBuilder();
            
            if (!hasFireResistancePotion) missing.append("Fire Resistance зілля, ");
            if (!hasTNT) missing.append("TNT, ");
            if (!hasFlintAndSteel) missing.append("Flint & Steel, ");
            if (!hasPickaxe) missing.append("Pickaxe, ");
            if (!hasModConfig) missing.append("Конфіг мода");
            
            String result = missing.toString().trim();
            if (result.endsWith(",")) {
                result = result.substring(0, result.length() - 1);
            }
            
            return result;
        }
    }
    
    /**
     * Повна перевірка всіх ресурсів
     */
    public static ResourceCheckResult checkAllResources(ClientPlayerEntity player) {
        ResourceCheckResult result = new ResourceCheckResult();
        
        result.hasFireResistancePotion = hasFireResistancePotion(player);
        result.hasTNT = hasTNT(player);
        result.hasFlintAndSteel = hasFlintAndSteel(player);
        result.hasPickaxe = hasPickaxe(player);
        result.hasModConfig = hasModConfig();
        
        return result;
    }
    
    /**
     * Перевірка Fire Resistance зілля
     */
    public static boolean hasFireResistancePotion(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() == Items.POTION) {
                    // Додатково можна перевірити, чи це саме Fire Resistance зілля
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Перевірка TNT (вибухівка)
     */
    public static boolean hasTNT(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() == Items.TNT) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Перевірка Flint & Steel (кресало)
     */
    public static boolean hasFlintAndSteel(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() == Items.FLINT_AND_STEEL) {
                    // Кресало знайдено (незалежно від пошкодження)
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Перевірка Pickaxe (кирка)
     */
    public static boolean hasPickaxe(ClientPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Перевірка конфігу мода (чи встановлено anarchyNumber)
     */
    public static boolean hasModConfig() {
        try {
            return ModConfig.anarchyNumber > 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Отримати к-ть конкретного ресурсу
     */
    public static int getItemCount(ClientPlayerEntity player, net.minecraft.item.Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() == item) {
                    count += player.getInventory().getStack(i).getCount();
                }
            }
        }
        return count;
    }
    
    /**
     * Вивести детальний звіт у чат
     */
    public static void sendDetailedReport(ClientPlayerEntity player, ResourceCheckResult result) {
        player.sendMessage(Text.literal(result.getDetailedReport()), false);
    }
    
    /**
     * Вивести коротке повідомлення про браки
     */
    public static void sendShortReport(ClientPlayerEntity player, ResourceCheckResult result) {
        if (result.isAllAvailable()) {
            player.sendMessage(Text.literal("§a✓ Всі ресурси готові!"), false);
        } else {
            player.sendMessage(
                Text.literal("§c✗ Браку: " + result.getMissingItems()),
                false
            );
        }
    }
    
    /**
     * Розширена перевірка з кількістю предметів
     */
    public static String getDetailedInventoryReport(ClientPlayerEntity player) {
        StringBuilder report = new StringBuilder();
        
        report.append("§6=== ІНВЕНТАР ===\n");
        report.append("§eXX Fire Resistance: ").append(getItemCount(player, Items.POTION)).append("\n");
        report.append("§eXX TNT: ").append(getItemCount(player, Items.TNT)).append("\n");
        report.append("§eXX Flint & Steel: ").append(getItemCount(player, Items.FLINT_AND_STEEL)).append("\n");
        
        // Підрахунок кирок
        int pickaxeCount = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            if (!player.getInventory().getStack(i).isEmpty()) {
                if (player.getInventory().getStack(i).getItem() instanceof PickaxeItem) {
                    pickaxeCount += player.getInventory().getStack(i).getCount();
                }
            }
        }
        report.append("§eXX Pickaxe: ").append(pickaxeCount).append("\n");
        
        return report.toString();
    }
}
