package xyz.champrin.stepsrecorder;


import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityTeleportEvent;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.*;

public class StepsRecorder extends PluginBase implements Listener {
//DEBUG： 飞行不记步，下坑动作只记一步
   private static StepsRecorder instance;

    public static StepsRecorder getInstance() {
        return instance;
    }

    public Config config, playerStepConfig, playerMonthStepConfig, playerYearStepConfig;

    public LinkedHashMap<String, Integer> playerStep = new LinkedHashMap<>();//day
    public LinkedHashMap<String, Integer> playerMonthStep = new LinkedHashMap<>();
    public LinkedHashMap<String, Integer> playerYearStep = new LinkedHashMap<>();
    public LinkedHashMap<String, String> playerPos = new LinkedHashMap<>();

    public List<String> OpenWorlds = new ArrayList<>();

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        LoadConfig();
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);
        int month = rightNow.get(Calendar.MONTH) + 1; //第一个月从0开始，所以得到月份＋1
        int day = rightNow.get(Calendar.DAY_OF_MONTH);
        String TimeNow24 = year + "-" + month + "-" + day;
        if (config.get("time") == null) {
            config.set("time", TimeNow24);
            config.save();
        }
        String[] oldTime = ((String) config.get("time")).split("-");
        if (year > Integer.parseInt(oldTime[0]) || month > Integer.parseInt(oldTime[1]) || day > Integer.parseInt(oldTime[2])) {
            config.set("time", TimeNow24);
            config.save();
        }
    }

    @Override
    public void onDisable() {
        for (Map.Entry<String, Integer> map : playerStep.entrySet()) {
            playerStepConfig.set(map.getKey(), map.getValue());
        }
        playerStepConfig.save();
    }

    public void LoadConfig() {
        if (!new File(this.getDataFolder() + "/config.yml").exists()) {
            this.saveResource("config.yml", false);
        }
        this.config = new Config(this.getDataFolder() + "/config.yml", Config.YAML);
        this.OpenWorlds.addAll(config.getStringList("open-worlds"));

        if (!new File(this.getDataFolder() + "/eng.yml").exists()) {
            this.saveResource("eng.yml", false);
        }

        this.playerStepConfig = new Config(this.getDataFolder() + "/playerStep.yml", Config.YAML);
        if (!new File(this.getDataFolder() + "/playerStep.yml").exists()) {
            this.playerStepConfig.save();
        }
        for (Map.Entry<String, Object> map : playerStepConfig.getAll().entrySet()) {
            playerStep.put(map.getKey(), (Integer) map.getValue());
        }

        /*this.playerMonthStepConfig = new Config(this.getDataFolder() + "/playerMonthStep.yml", Config.YAML);
        if (!new File(this.getDataFolder() + "/playerMonthStep.yml").exists()) {
            this.playerMonthStepConfig.save();
        }
        for (Map.Entry<String, Object> map : playerMonthStepConfig.getAll().entrySet()) {
            playerMonthStep.put(map.getKey(), (Integer) map.getValue());
        }

        this.playerYearStepConfig = new Config(this.getDataFolder() + "/playerYearStep.yml", Config.YAML);
        if (!new File(this.getDataFolder() + "/playerYearStep.yml").exists()) {
            this.playerYearStepConfig.save();
        }
        for (Map.Entry<String, Object> map : playerYearStepConfig.getAll().entrySet()) {
            playerYearStep.put(map.getKey(), (Integer) map.getValue());
        }*/
    }

    public Integer getPlayerStep(String playerName) {
        return playerStep.get(playerName);
    }

    public String getPlayerPos(String playerName) {
        return playerPos.get(playerName);
    }

    public void setPlayerStep(String playerName, int steps) {
        playerStep.put(playerName, steps);
    }

    public void setPlayerPos(String playerName, String pos) {
        playerPos.put(playerName, pos);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        int x = player.getFloorX();
        int y = player.getFloorY();
        int z = player.getFloorZ();
        String pos = x + "+" + y + "+" + z;
        playerPos.put(playerName, pos);
        if (!playerStep.containsKey(playerName)) {
            playerStep.put(playerName, 0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        if (OpenWorlds.contains(event.getPlayer().getLevel().getFolderName())) {
            Player player = event.getPlayer();
            String playerName = player.getName();

            int x = player.getFloorX();
            int y = player.getFloorY();
            int z = player.getFloorZ();

            int step = getPlayerStep(playerName);

            String[] oldPos = getPlayerPos(playerName).split("\\+");
            int ox = Integer.parseInt(oldPos[0]);
            int oy = Integer.parseInt(oldPos[1]);
            int oz = Integer.parseInt(oldPos[2]);

            int a = 0;
            if (x != ox) {
                a = Math.abs(x - ox);
                step = step + a;
            } else if (y != oy) {
                a = Math.abs(y - oy);
                step = step + a;
            } else if (z != oz) {
                a = Math.abs(z - oz);
                step = step + a;
            }
            setPlayerStep(playerName, step);
            setPlayerPos(playerName, x + "+" + y + "+" + z);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(EntityTeleportEvent event) {
        if (OpenWorlds.contains(event.getEntity().getLevel().getFolderName())) {
            if (event.getEntity() instanceof Player) {
                Entity player = event.getEntity();
                setPlayerPos(player.getName(), player.getFloorX() + "+" + player.getFloorY() + "+" + player.getFloorZ());
            }
        }
    }

    public void sortByValue(Map<String, Integer> map) {

        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        //升序排序
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        map.clear();

        for (Map.Entry<String, Integer> s : list) {
            map.put(s.getKey(), s.getValue());
        }
    }

    public String getRank() {
        sortByValue(playerStep);
        String rank = "";
        int i = 1;
        for (Map.Entry<String, Integer> map : playerStep.entrySet()) {
            rank = rank + "§r§f" + "第§l§c" + i + "§r§f名:§a§l " + map.getKey() + "§r§f走了§l§6" + map.getValue() + "§r§f 步"+"\n";
            i = i + 1;
        }
        return rank;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String Title = "§l§dStepsRecorder§f>§r";
        if (args.length < 1) {
            sender.sendMessage(Title + "  §c指令输入错误");
            return false;
        }
        switch (args[0]) {
            case "rank":
                if (sender instanceof Player) {
                    FormWindowSimple window = new FormWindowSimple("步数排行榜", "");
                    window.addButton(new ElementButton("查看日排行榜"));
                    //window.addButton(new ElementButton("查看本月排行榜"));
                    //window.addButton(new ElementButton("查看本年排行榜"));
                    ((Player) sender).showFormWindow(window, 2073746570);
                } else {
                    sender.sendMessage(">  请在游戏中运行");
                }
                break;
            case "addworld":
                if (!sender.isOp()) {
                    sender.sendMessage(Title + "  §a没有权限使用此指令");
                    break;
                }
                if (args.length >= 2) {
                    String level = args[1];
                    if (OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  §a地图§6" + level + "§a已经开启此功能");
                        break;
                    }
                    this.OpenWorlds.add(level);
                    config.set("open-worlds",OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  §6记步开启在世界§a" + level);
                } else {
                    sender.sendMessage(Title + "  §c未输入要添加的地图名");
                    sender.sendMessage(Title + "  §a用法: /step addworld [地图名]");
                }
                break;
            case "delworld":
                if (!sender.isOp()) {
                    sender.sendMessage(Title + "  §a没有权限使用此指令");
                    break;
                }
                if (args.length >= 2) {
                    String level = args[1];
                    if (!OpenWorlds.contains(level)) {
                        sender.sendMessage(Title + "  §a地图§6" + level + "§a未开启此功能");
                        break;
                    }
                    this.OpenWorlds.remove(level);
                    config.set("open-worlds",OpenWorlds);
                    config.save();
                    sender.sendMessage(Title + "  §6记步关闭在世界§a" + level);
                } else {
                    sender.sendMessage(Title + "  §c未输入要删除的地图名");
                    sender.sendMessage(Title + "  §a用法: /step delworld [地图名]");
                }
                break;
            default:
                sender.sendMessage("============== -=§l§dStepsRecorder§r§7=- ================");
                sender.sendMessage("/step help                         §8查看帮助");
                sender.sendMessage("/step rank                       §8查看排行榜");
                if (sender.isOp()) {
                    sender.sendMessage("/step addworld [世界名称]          §3添加§8开启记步的世界");
                    sender.sendMessage("/step delworld [世界名称]          §3移除§8开启记步的世界");
                }
                break;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (event.getFormID() == 2073746570) {
            FormResponseSimple response = (FormResponseSimple) event.getResponse();
            int clickedButtonId = response.getClickedButtonId();
            switch (clickedButtonId) {
                case 0:
                    FormWindowSimple window = new FormWindowSimple("步数排行榜", getRank());
                    player.showFormWindow(window);
                    break;
                /*case 1:
                    player.sendMessage("你摁了按钮2");
                    break;
                case 2:
                    player.sendMessage("你摁了按钮3");
                    break;
                default:
                    break;*/
            }
        }
    }
}