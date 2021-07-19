package com.redspeaks.potstacker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class PotStacker extends JavaPlugin implements Listener {

    private int maxStackSize = 16;
    private List<PotionType> whiteListed = new ArrayList<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        maxStackSize = getConfig().getInt("size");

        whiteListed = getConfig().getStringList("WhiteList").stream().map(PotionType::valueOf).collect(Collectors.toList());

        getServer().getPluginManager().registerEvents(this, this);

        getCommand("potion").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(args.length < 3) {
            sender.sendMessage(ChatColor.RED + "/potion give <player> <amount> <type>");
            sender.sendMessage(ChatColor.RED + "/potion give RedSpeaks 1 LINGERING_POTION_STRENGTH");
            sender.sendMessage(ChatColor.RED + "/potion give RedSpeaks 1 SPLASH_POTION_STRENGTH");
            sender.sendMessage(ChatColor.RED + "/potion give RedSpeaks 1 POTION_STRENGTH");
            return true;
        }
        if(args[0].equalsIgnoreCase("give")) {
            Player target = Bukkit.getPlayer(args[0]);
            if(target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found!");
                return true;
            }
            int amount = 0;
            try {
                Integer.parseInt(args[1]);
            }catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Type correct number format please.");
                return true;
            }
            amount = Integer.parseInt(args[1]);
            Material type = args[2].split("_").length > 2 ? Material.getMaterial(args[2].split("_")[0] + "_" + args[2].split("_")[1]) : Material.getMaterial(args[2].split("_")[0]);
            PotionType potionType = PotionType.valueOf(args[2].split("_").length > 2 ? args[2].split("_")[2] : args[2].split("_")[1]);

            if(giveCommand(target, type, amount, potionType)) {
                sender.sendMessage(ChatColor.GREEN + "Successfully given " + target.getDisplayName() + " potion type of " + potionType.name() + " with amount of: " + amount);
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Something went wrong");
        }
        return false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public boolean giveCommand(Player player, Material type, int amount, PotionType potionType) {
        if(type == null) {
            return false;
        }
        if(potionType == null) {
            return false;
        }
        ItemStack stack = amount > 1 ?  new ItemStack(type, amount) :  new ItemStack(type);
        if(stack.getType() != Material.POTION && stack.getType() != Material.SPLASH_POTION && stack.getType() != Material.LINGERING_POTION) {
            return false;
        }
        PotionMeta stackMeta = (PotionMeta) stack.getItemMeta();
        stackMeta.setBasePotionData(new PotionData(potionType));
        stack.setItemMeta(stackMeta);
        if(!whiteListed.contains(stackMeta.getBasePotionData().getType())) {
            return false;
        }
        boolean pickup = false;
        for(int i = 0; i < player.getInventory().getStorageContents().length; i++) {
            if(player.getInventory().getItem(i) == null) continue;
            ItemStack st = player.getInventory().getItem(i);
            if(st.getType() != Material.POTION && st.getType() != Material.SPLASH_POTION && st.getType() != Material.LINGERING_POTION) {
                continue;
            }
            PotionMeta pm = (PotionMeta)st.getItemMeta();
            if(!whiteListed.contains(pm.getBasePotionData().getType())) continue;
            if(st.getAmount() >= maxStackSize) continue;
            if((st.getAmount() + stack.getAmount()) >= maxStackSize) {
                int remaining = maxStackSize - (st.getAmount() + stack.getAmount());
                st.setAmount(stack.getAmount() - remaining);
                return giveCommand(player, stack.getType(), remaining, potionType);
            }
            if(st.isSimilar(stack)) {
                st.setAmount(st.getAmount() + stack.getAmount());
                pickup = true;
            }
        }
        return pickup;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerPicksUpItem(EntityPickupItemEvent event) {
        LivingEntity entity = event.getEntity();

        if (!(entity instanceof Player)) {
            return;
        }

        Player player = (Player) entity;
        Item item = event.getItem();
        ItemStack stack = item.getItemStack();

        if(stack.getType() != Material.POTION && stack.getType() != Material.SPLASH_POTION && stack.getType() != Material.LINGERING_POTION) {
            return;
        }

        PotionMeta stackMeta = (PotionMeta) stack.getItemMeta();

        if(!whiteListed.contains(stackMeta.getBasePotionData().getType())) {
            return;
        }
        boolean pickup = false;
        for(int i = 0; i < player.getInventory().getStorageContents().length; i++) {
            if(player.getInventory().getItem(i) == null) continue;
            ItemStack st = player.getInventory().getItem(i);
            if(st.getType() != Material.POTION && st.getType() != Material.SPLASH_POTION && st.getType() != Material.LINGERING_POTION) {
                continue;
            }
            PotionMeta pm = (PotionMeta)st.getItemMeta();
            if(!whiteListed.contains(pm.getBasePotionData().getType())) continue;
            if(st.getAmount() >= maxStackSize) continue;
            if(st.isSimilar(stack)) {
                st.setAmount(st.getAmount() + stack.getAmount());
                pickup = true;
            }
        }
        if(!pickup) return;


        player.updateInventory();

        Random random = new Random();
        Sound pickupSound = Sound.ENTITY_ITEM_PICKUP;
        player.playSound(item.getLocation(), pickupSound, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);

        item.remove();

        event.setCancelled(true);
    }
}
