package com.gmail.val59000mc.scenarios;

import com.gmail.val59000mc.UhcCore;
import com.gmail.val59000mc.game.GameManager;
import com.gmail.val59000mc.languages.Lang;
import com.gmail.val59000mc.players.UhcPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScenarioManager {

    private Map<Scenario, ScenarioListener> activeScenarios;

    public ScenarioManager(){
        activeScenarios = new HashMap<>();
    }

    public void addScenario(Scenario scenario){
        if (isActivated(scenario)){
            return;
        }

        Class<? extends ScenarioListener> listenerClass = scenario.getListener();

        try {
            ScenarioListener scenarioListener = null;
            if (listenerClass != null) {
                scenarioListener = listenerClass.newInstance();
            }

            activeScenarios.put(scenario, scenarioListener);

            if (scenarioListener != null) {
                scenarioListener.onEnable();
                Bukkit.getServer().getPluginManager().registerEvents(scenarioListener, UhcCore.getPlugin());
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void removeScenario(Scenario scenario){
        ScenarioListener scenarioListener = activeScenarios.get(scenario);
        activeScenarios.remove(scenario);

        if (scenarioListener != null) {
            HandlerList.unregisterAll(scenarioListener);
            scenarioListener.onDisable();
        }
    }

    public boolean toggleScenario(Scenario scenario){
        if (isActivated(scenario)){
            removeScenario(scenario);
            return false;
        }

        addScenario(scenario);
        return true;
    }

    public synchronized Set<Scenario> getActiveScenarios(){
        return activeScenarios.keySet();
    }

    public boolean isActivated(Scenario scenario){
        return activeScenarios.containsKey(scenario);
    }

    public ScenarioListener getScenarioListener(Scenario scenario){
        return activeScenarios.get(scenario);
    }

    public Inventory getScenarioMainInventory(boolean editItem){

        Inventory inv = Bukkit.createInventory(null,3*9, Lang.SCENARIO_GLOBAL_INVENTORY);

        for (Scenario scenario : getActiveScenarios()) {
            ItemStack item = scenario.getScenarioItem();
            if (item != null) {
                inv.addItem(item);
            }
        }

        if (editItem){
            // add edit item
            ItemStack edit = new ItemStack(Material.BARRIER);
            ItemMeta itemMeta = edit.getItemMeta();
            itemMeta.setDisplayName(Lang.SCENARIO_GLOBAL_ITEM_EDIT);
            edit.setItemMeta(itemMeta);

            inv.setItem(26,edit);
        }
        return inv;
    }

    public Inventory getScenarioEditInventory(){

        Inventory inv = Bukkit.createInventory(null,5*9, Lang.SCENARIO_GLOBAL_INVENTORY_EDIT);

        // add edit item
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta itemMeta = back.getItemMeta();
        itemMeta.setDisplayName(Lang.SCENARIO_GLOBAL_ITEM_BACK);
        back.setItemMeta(itemMeta);
        inv.setItem(36,back);

        for (Scenario scenario : Scenario.values()){
            ItemStack scenarioItem = scenario.getScenarioItem();

            if (scenarioItem == null){
                continue;
            }

            if (isActivated(scenario)){
                scenarioItem.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                scenarioItem.setAmount(2);
            }
            inv.addItem(scenarioItem);
        }

        return inv;
    }

    public Inventory getScenarioVoteInventory(UhcPlayer uhcPlayer){
        Set<Scenario> playerVotes = uhcPlayer.getScenarioVotes();
        Set<Scenario> blacklist = GameManager.getGameManager().getConfiguration().getScenarioBlackList();
        Inventory inv = Bukkit.createInventory(null,4*9, Lang.SCENARIO_GLOBAL_INVENTORY_VOTE);

        for (Scenario scenario : Scenario.values()){
            if (blacklist.contains(scenario)){
                continue;
            }

            ItemStack item = scenario.getScenarioItem();

            if (item == null){
                continue;
            }

            if (playerVotes.contains(scenario)) {
                item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
                item.setAmount(2);
            }
            inv.addItem(item);
        }
        return inv;
    }

    public void loadActiveScenarios(List<String> scenarios){
        for (String string : scenarios){
            try {
                Scenario scenario = Scenario.valueOf(string);
                Bukkit.getLogger().info("[UhcCore] Loading " + scenario.getName());
                addScenario(scenario);
            }catch (Exception ex){
                Bukkit.getLogger().severe("[UhcCore] Invalid scenario: " + string);
            }
        }
    }

    public void countVotes(){
        Map<Scenario, Integer> votes = new HashMap<>();

        for (Scenario scenario : Scenario.values()){
            votes.put(scenario, 0);
        }

        for (UhcPlayer uhcPlayer : GameManager.getGameManager().getPlayersManager().getPlayersList()){
            for (Scenario scenario : uhcPlayer.getScenarioVotes()){
                int totalVotes = votes.get(scenario) + 1;
                votes.put(scenario, totalVotes);
            }
        }

        int scenarioCount = GameManager.getGameManager().getConfiguration().getElectedScenaroCount();
        while (scenarioCount > 0){
            // get scenario with most votes
            Scenario scenario = null;
            int scenarioVotes = 0;

            for (Scenario s : votes.keySet()){
                if (scenario == null || votes.get(s) > scenarioVotes){
                    scenario = s;
                    scenarioVotes = votes.get(s);
                }
            }

            addScenario(scenario);
            votes.remove(scenario);
            scenarioCount--;
        }
    }

}