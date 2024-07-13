package abvgd;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class shake extends JavaPlugin implements Listener {
    List<Player> infectedList = new ArrayList<>();

    Random rand = new Random();
    public int random(int a, int b) { return rand.nextInt(b - a  + 1) + a; }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        infectedEvent();
    }

    public void infectedEvent()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (!infectedList.isEmpty())
            {
                for (int i = 0; i < infectedList.size(); i++)
                {
                    Player player = infectedList.get(i);
                    Vector direction = player.getLocation().getDirection();
                    Location particleLocation = player.getLocation().add(direction.multiply(0.3)).add(0, 1.6, 0);
                    Color startColor = Color.fromRGB(0, 255, 0);
                    Color endColor = Color.fromRGB(0, 180, 0);
                    float size = 2.0F;
                    Particle.DustTransition dustOptions = new Particle.DustTransition(startColor, endColor, size);

                    player.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLocation, 15, dustOptions);
                    player.playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_BREAK, 10, 1);

                    setPlayerSick(player);

                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (infectedList.contains(player)) setBadEffect(player);
                    }, random(300, 3600)); // 15s - 3min
                }
            }
        }, 0, random(8400, 12000)); // 7 - 10 min
    }

    public void setPlayerSick(Player player)
    {
        int radius = 3;
        Location playerLocation = player.getLocation();
        List<Player> nearbyPlayers = player.getWorld().getPlayers();

        for (Player p : nearbyPlayers) {
            if (p.equals(player)) {
                continue;
            }
            if (p.getLocation().distance(playerLocation) <= radius)
            {
                if (!infectedList.contains(p))
                {
                    if (!inSafeSuit(p)) infectedList.add(p);
                }
            }
        }
    }

    public boolean inSafeSuit(Player player)
    {
        PlayerInventory inventory = player.getInventory();

        ItemStack helmet = inventory.getHelmet();
        ItemStack chestplate = inventory.getChestplate();
        ItemStack leggings = inventory.getLeggings();
        ItemStack boots = inventory.getBoots();

        if (helmet != null && helmet.getType() == Material.GLASS &&
        chestplate != null && chestplate.getType() == Material.LEATHER_CHESTPLATE &&
        leggings != null && leggings.getType() == Material.LEATHER_LEGGINGS &&
        boots != null && boots.getType() == Material.LEATHER_BOOTS) return true;

        return false;
    }

    public void setBadEffect(Player player)
    {
        int n = random(0, 6);
        switch (n)
        {
            case 0:
                addPotionEffect(player, PotionEffectType.POISON);
                break;
            case 1:
                addPotionEffect(player, PotionEffectType.DARKNESS);
                break;
            case 2:
                addPotionEffect(player, PotionEffectType.HUNGER);
                break;
            case 3:
                addPotionEffect(player, PotionEffectType.WEAKNESS);
                break;
            case 4:
                addPotionEffect(player, PotionEffectType.CONFUSION);
                break;
            case 5:
                addPotionEffect(player, PotionEffectType.SLOW);
                break;
            case 6:
                addPotionEffect(player, PotionEffectType.SATURATION);
                break;
        }
    }

    public void addPotionEffect(Player player, PotionEffectType potion)
    {
        int x = random(200, 800); // 10s - 40s
        player.addPotionEffect(new PotionEffect(potion, x, random(0, 1)));
    }

    public void restoreInfectedPlayer(Player player)
    {
        infectedList.remove(player);
        player.setMaxHealth(20);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 10, 1);
        player.clearActivePotionEffects();
    }

    public void infectedPlayerEffect(Player player)
    {
        player.setMaxHealth(14);
        player.setFreezeTicks(500);
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 40, 2));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, String[] args)
    {
        Player player = (Player) sender;
        if (command.getName().equals("infect") && sender.isOp())
        {
            if (args.length == 1)
            {
                String name = args[0];
                infectedList.add(Bukkit.getPlayer(name));
                player.sendMessage("§dPlayer: " + name + " was infected!");
                return true;
            }
            else if (args.length == 2) {
                String name = args[1];
                if (args[0].equals("event"))
                {
                    infectedPlayerEffect(Objects.requireNonNull(Bukkit.getPlayer(name)));
                    player.sendMessage("§a" + name + "§2 get§c infect§2 event!");
                }
                else if (args[0].equals("remove"))
                {
                    restoreInfectedPlayer(Objects.requireNonNull(Bukkit.getPlayer(name)));
                    player.sendMessage("§a" + name + "§2 was recovered! (removed)");
                }

                return true;
            }
            else {
                if (!infectedList.isEmpty())
                {
                    player.sendMessage("-----INFECTED-----");
                    for (Player pl : Bukkit.getOnlinePlayers())
                    {
                        if (infectedList.contains(pl))
                        {
                            player.sendMessage(pl.getName());
                        }
                    }
                    player.sendMessage("------------------");
                }
                else player.sendMessage("{ empty }"); return true;
            }
        }
        return false;
    }
}