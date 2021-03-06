package net.pl3x.bukkit.ridables.listener;

import net.pl3x.bukkit.ridables.Ridables;
import net.pl3x.bukkit.ridables.configuration.Lang;
import net.pl3x.bukkit.ridables.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class WaterBucketListener implements Listener {
    private final Ridables plugin;

    public WaterBucketListener(Ridables plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCollectCreature(PlayerInteractAtEntityEvent event) {
        Entity creature = event.getRightClicked();
        if (creature.isDead() || !creature.isValid()) {
            return; // creature already removed from world
        }

        ItemStack bucket = plugin.getBuckets().getBucket(creature.getType());
        if (bucket == null) {
            return; // not a supported creature
        }

        if (!creature.getPassengers().isEmpty()) {
            return; // creature has a rider
        }

        Player player = event.getPlayer();
        ItemStack hand = Utils.getItem(player, event.getHand());
        if (hand == null || hand.getType() != Material.WATER_BUCKET) {
            return; // not a water bucket
        }

        Entity vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getUniqueId().equals(creature.getUniqueId())) {
            return; // player is riding this creature
        }

        if (!player.hasPermission("allow.collect." + creature.getType().name().toLowerCase())) {
            Lang.send(player, Lang.COLLECT_NO_PERMISSION);
            return;
        }

        // remove creature
        creature.remove();

        // give player creature's bucket
        Utils.setItem(player, bucket.clone(), event.getHand());

        // prevent water from placing
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlaceCreature(PlayerBucketEmptyEvent event) {
        if (event.getBucket() != Material.COD_BUCKET) {
            return; // not a valid creature bucket
        }

        // get the bucket used
        Player player = event.getPlayer();
        ItemStack bucket = player.getInventory().getItemInMainHand();
        EntityType entityType = plugin.getBuckets().getEntityType(bucket);
        EquipmentSlot hand = EquipmentSlot.HAND;
        if (entityType == null) {
            bucket = player.getInventory().getItemInOffHand();
            entityType = plugin.getBuckets().getEntityType(bucket);
            hand = EquipmentSlot.OFF_HAND;
            if (entityType == null) {
                return; // not a valid creature bucket
            }
        }

        // spawn the creature
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (plugin.creatures().spawn(entityType, Utils.buildLocation(block.getLocation(), player.getLocation()))) {
            // handle the bucket in hand
            if (player.getGameMode() != GameMode.CREATIVE) {
                Utils.setItem(player, Utils.subtract(bucket), hand);
            }

            // place water at location
            block.setType(Material.WATER, true);
        }

        // do not spawn a cod!
        event.setCancelled(true);
    }
}
