/*
 * BedWars2023 - A bed wars mini-game.
 * Copyright (C) 2024 Tomas Keuper
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Contact e-mail: contact@fyreblox.com
 */

package com.tomkeuper.bedwars.arena.spectator;

import com.tomkeuper.bedwars.BedWars;
import com.tomkeuper.bedwars.api.arena.IArena;
import com.tomkeuper.bedwars.api.arena.team.ITeam;
import com.tomkeuper.bedwars.api.configuration.ConfigPath;
import com.tomkeuper.bedwars.api.language.Language;
import com.tomkeuper.bedwars.api.language.Messages;
import com.tomkeuper.bedwars.arena.Arena;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tomkeuper.bedwars.BedWars.nms;
import static com.tomkeuper.bedwars.api.language.Language.getList;
import static com.tomkeuper.bedwars.api.language.Language.getMsg;

@SuppressWarnings("WeakerAccess")
public class TeleporterGUI {

    //Don't remove "_" because it's used as a separator somewhere
    public static final String NBT_SPECTATOR_TELEPORTER_GUI_HEAD = "spectatorTeleporterGUIhead_";

    /**
     * -- GETTER --
     *  Get a HashMap of players with Teleporter GUI opened
     */
    @Getter
    private static final HashMap<Player, Inventory> refresh = new HashMap<>();

    /**
     * Refresh the Teleporter GUI for a player
     */
    public static void refreshInv(Player p, Inventory inv) {
        if (p.getOpenInventory() == null) return;
        IArena arena = Arena.getArenaByPlayer(p);
        if (arena == null) {
            p.closeInventory();
            return;
        }

        List<Player> players = arena.getPlayers();
        String[] slotStrings = BedWars.config.getYml().getString(ConfigPath.GENERAL_CONFIGURATION_TELEPORTER_SLOTS).split(",");
        List<Integer> slots = new ArrayList<>();
        for (String slot : slotStrings) {
            try {
                slots.add(Integer.parseInt(slot));
            } catch (NumberFormatException ignored) {
                // Ignore invalid slot numbers
            }
        }

        for (int i = 0; i < slots.size(); i++) {
            if (i < players.size()) {
                inv.setItem(slots.get(i), createHead(players.get(i), p));
            } else {
                inv.setItem(slots.get(i), new ItemStack(Material.AIR));
            }
        }
    }

    /**
     * Opens the Teleporter GUI to a Player
     */
    public static void openGUI(Player p) {
        IArena arena = Arena.getArenaByPlayer(p);
        if (arena == null) return;

        int size = BedWars.config.getYml().getInt(ConfigPath.GENERAL_CONFIGURATION_TELEPORTER_GUI_SIZE);
        if (size % 9 != 0) size = 27; // Ensure size is a multiple of 9 otherwise set to 27
        if (size > 54) size = 54; // Limit size to maximum 54

        Inventory inv = Bukkit.createInventory(p, size, getMsg(p, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_NAME));
        refreshInv(p, inv);
        refresh.put(p, inv);
        p.openInventory(inv);
    }

    /**
     * Refresh the Teleporter GUI for all players with it opened
     */
    public static void refreshAllGUIs() {
        for (Map.Entry<Player, Inventory> e : new HashMap<>(getRefresh()).entrySet()) {
            refreshInv(e.getKey(), e.getValue());
        }
    }

    /**
     * Create a player head
     */
    private static ItemStack createHead(Player targetPlayer, Player GUIholder) {
        ItemStack i = nms.getPlayerHead(targetPlayer, null);
        ItemMeta im = i.getItemMeta();
        assert im != null;

        IArena currentArena = Arena.getArenaByPlayer(targetPlayer);
        ITeam targetPlayerTeam = currentArena.getTeam(targetPlayer);
        im.setDisplayName(getMsg(targetPlayer, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_HEAD_NAME)
                .replace("%bw_v_prefix%", BedWars.getChatSupport().getPrefix(targetPlayer))
                .replace("%bw_v_suffix%", BedWars.getChatSupport().getSuffix(targetPlayer))
                .replace("%bw_player%", targetPlayer.getDisplayName())
                .replace("%bw_team_color%", String.valueOf(targetPlayerTeam.getColor().chat()))
                .replace("%bw_team%", targetPlayerTeam.getDisplayName(Language.getPlayerLanguage(GUIholder)))
                .replace("%bw_playername%", targetPlayer.getName()));
        List<String> lore = new ArrayList<>();
        String health = String.valueOf((int)targetPlayer.getHealth() * 100 / targetPlayer.getHealthScale());
        for (String s : getList(GUIholder, Messages.ARENA_SPECTATOR_TELEPORTER_GUI_HEAD_LORE)) {
            lore.add(s.replace("%bw_player_health%", health).replace("%bw_player_food%", String.valueOf(targetPlayer.getFoodLevel())));
        }
        im.setLore(lore);
        i.setItemMeta(im);
        return nms.addCustomData(i, NBT_SPECTATOR_TELEPORTER_GUI_HEAD + targetPlayer.getName());
    }

    /**
     * Remove a player from the refresh list and close gui
     */
    public static void closeGUI(Player p) {
        if (getRefresh().containsKey(p)) {
            refresh.remove(p);
            p.closeInventory();
        }
    }
}
