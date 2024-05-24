package com.cjcrafter.weaponmechanicssignshop;

import me.deecaad.core.MechanicsCore;
import me.deecaad.core.compatibility.CompatibilityAPI;
import me.deecaad.core.compatibility.vault.IVaultCompatibility;
import me.deecaad.core.file.Configuration;
import me.deecaad.core.file.FileReader;
import me.deecaad.core.utils.Debugger;
import me.deecaad.core.utils.FileUtil;
import me.deecaad.weaponmechanics.WeaponMechanics;
import me.deecaad.weaponmechanics.weapon.info.InfoHandler;
import net.kyori.adventure.text.Component;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.Collections;

public class WeaponMechanicsSignShop extends JavaPlugin implements Listener {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");

    private Configuration config;
    private Debugger debug;
    private Metrics metrics;

    public void onEnable() {
        debug = new Debugger(getLogger(), 2); // hardcode 2 since this is a tiny plugin

        // Copy files from JAR to PLUGIN FOLDER
        if (!getDataFolder().exists() || getDataFolder().listFiles() == null || getDataFolder().listFiles().length == 0) {
            debug.info("Copying files from jar (This process may take up to 30 seconds during the first load!)");
            FileUtil.copyResourcesTo(getClassLoader().getResource("WeaponMechanicsSignShop"), getDataFolder().toPath());
        }

        FileReader reader = new FileReader(debug, null, Collections.singletonList(new SignMessageValidator()));
        config = reader.fillAllFiles(getDataFolder());

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);

        // Add permissions to the plugin
        Permission create = new Permission("weaponmechanicssignshop.create");
        create.setDescription("The ability to create new admin sign shops");
        create.setDefault(PermissionDefault.OP);
        pm.addPermission(create);

        Permission use = new Permission("weaponmechanicssignshop.use");
        use.setDescription("The ability to buy from existing sign shops");
        use.setDefault(PermissionDefault.TRUE);
        pm.addPermission(use);

        registerBStats();
    }

    @EventHandler
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if(!event.getHand().equals(EquipmentSlot.HAND))
            return;
        if (!event.hasBlock())
            return;
        if (!(event.getClickedBlock().getState() instanceof Sign))
            return;
        if (!event.getPlayer().hasPermission("weaponmechanicssignshop.use")) {
            String msg = config.getString("No_Permission_Message");
            Component component = MechanicsCore.getPlugin().message.deserialize(msg);
            MechanicsCore.getPlugin().adventure.player(event.getPlayer()).sendMessage(component);
            return;
        }

        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!isShop(sign))
            return;

        // After getting the weapon, we should make sure it still exists
        String weapon = sign.getLine(1);
        if (!WeaponMechanics.getWeaponHandler().getInfoHandler().hasWeapon(weapon))
            return;

        Player player = event.getPlayer();
        int amount = Integer.parseInt(sign.getLine(2));
        double price = Double.parseDouble(sign.getLine(3).substring(1)); // substring 1 to strip $ sign

        // Vault compatibility handles checking/taking balance away from the
        // player.
        IVaultCompatibility vault = CompatibilityAPI.getVaultCompatibility();

        // Check if the player has enough money
        if (!vault.hasBalance(player, price)) {
            String msg = config.getString("Not_Enough_Money_Message");
            Component component = MechanicsCore.getPlugin().message.deserialize(msg);
            MechanicsCore.getPlugin().adventure.player(player).sendMessage(component);
            return;
        }

        // Withdraw the money, give the weapons to the player, and send them
        // an alert that they made a purchase.
        vault.withdrawBalance(player, price);

        Bukkit.getScheduler().runTaskLater(this, new BukkitRunnable() {
            @Override
            public void run() {

                WeaponMechanics.getWeaponHandler().getInfoHandler().giveOrDropWeapon(weapon, player, amount);

                String msg = config.getString("Buy_Message");
                msg = msg.replace("%weapon%", weapon);
                msg = msg.replace("%price%", DECIMAL_FORMAT.format(price));
                msg = msg.replace("%count%", String.valueOf(amount));
                Component component = MechanicsCore.getPlugin().message.deserialize(msg);
                MechanicsCore.getPlugin().adventure.player(player).sendMessage(component);

            }
        }, 20*1);
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        if (!config.getString("Identifier").equals(event.getLine(0)))
            return;
        if (!event.getPlayer().hasPermission("weaponmechanicssignshop.create")) {
            String msg = config.getString("No_Permission_Message");
            Component component = MechanicsCore.getPlugin().message.deserialize(msg);
            MechanicsCore.getPlugin().adventure.player(event.getPlayer()).sendMessage(component);
            event.setCancelled(true);
            return;
        }

        // Now we know that the user is creating a sign for buying guns. We
        // just need to make sure they are putting proper values into the sign.
        InfoHandler info = WeaponMechanics.getWeaponHandler().getInfoHandler();
        event.setLine(1, info.getWeaponTitle(event.getLine(1)));

        String line2 = event.getLine(2);
        try {
            Integer.parseInt(line2);
        } catch (NumberFormatException ex) {
            event.getPlayer().sendMessage(ChatColor.RED + "Could not parse " + line2 + " to a number!");
            event.setCancelled(true);
            return;
        }

        String line3 = event.getLine(3);
        if (line3 == null) {
            event.getPlayer().sendMessage(ChatColor.RED + "Price line was empty?");
            event.setCancelled(true);
            return;
        }

        if (line3.startsWith("$")) line3 = line3.substring(1);
        try {
            Double.parseDouble(line3);
            event.setLine(3, "$" + line3);
        } catch (NumberFormatException ex) {
            event.getPlayer().sendMessage(ChatColor.RED + "Could not parse " + line3 + " to a price!");
            event.setCancelled(true);
            return;
        }

        event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully created Weapon Shop");
    }

    public void registerBStats() {
        if (metrics != null) return;

        debug.debug("Registering bStats");

        // See https://bstats.org/plugin/bukkit/WeaponMechanicsSignShop/16444. This is
        // the bStats plugin id used to track information.
        metrics = new Metrics(this, 16444);
    }

    public boolean isShop(Sign sign) {
        return sign.getLine(0).equals(config.getString("Identifier"));
    }
}
