package co.mchost.gitbroadcast;

import co.mchost.gitbroadcast.util.GitUtil;
import co.mchost.gitbroadcast.util.JsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

/**
 * Created by luke on 2/8/16.
 */
public class GitBroadcast extends JavaPlugin {
    private JsonObject config;

    private ArrayList<String> messages = new ArrayList<String>();
    private String prefix = "";
    private Integer interval = 30;

    private int index = 0;
    private int runnableID;

    @Override
    public void onEnable() {

        if (!new File("plugins/GitBroadcast/config.json").exists()) {
            createConfigFile();
        }

        config = JsonUtil.convertFileToJSON("plugins/GitBroadcast/config.json");
        if (!(config.has("profile")
                && config.has("repository")
                && config.has("file")
                && config.has("interval")
                && config.has("border"))) {
            Bukkit.getServer().getLogger().log(Level.SEVERE, "Unable to load plugin!" +
                    " Please make sure the config.json is filled out.");
            return;
        }

        this.interval = config.get("interval").getAsInt();

        if (loadMessages()) {
            Bukkit.getServer().getLogger().log(Level.FINEST, "Successfully pulled from Github repo.");
        } else {
            Bukkit.getServer().getLogger().log(Level.SEVERE, "Failed to load messages. Please check your Github repo " +
                    "and make sure files are correctly formatted.");
        }

        this.runnableID = startScheduler();

    }

    public int startScheduler() {
        return this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                broadcast();
            }
        }, interval * 20, interval * 20);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("gitbroadcast")) {

            if (args.length == 1 && args[0].equals("reload")) {
                if (sender.hasPermission("GitBroadcast.reload")) {

                    if (!loadMessages()) {
                        sender.sendMessage(ChatColor.RED + "Failed to load messages. Please check your Github repo " +
                                "and make sure files are correctly formatted.");
                    }

                    config = JsonUtil.convertFileToJSON("plugins/GitBroadcast/config.json");
                    if (!(config.has("profile")
                            && config.has("repository")
                            && config.has("file")
                            && config.has("interval")
                            && config.has("border"))) {
                        sender.sendMessage(ChatColor.RED + "Unable to load plugin!" +
                                " Please make sure the config.json is filled out.");

                        return true;
                    }

                    this.interval = config.get("interval").getAsInt();

                    this.getServer().getScheduler().cancelTask(this.runnableID);
                    this.runnableID = this.startScheduler();

                    sender.sendMessage(ChatColor.GREEN + "Successfully reloaded GitBroadcast.");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                }
            }

            else {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.AQUA + "Git Broadcast" + ChatColor.GREEN + "===");
                sender.sendMessage(ChatColor.GOLD + "/gitbroadcast reload");
            }
        }
        return true;
    }

    public void broadcast() {

        if (index >= messages.size()) {
            if (messages.size() == 0) {
                Bukkit.getServer().getLogger().log(Level.SEVERE, "Messages list is empty! Check your formatting on Github repo.");
                return;
            }

            while (index >= messages.size()) {
                index--;
            }
        }

        String base = prefix + messages.get(index);

        boolean isBorder = config.get("border").getAsBoolean();
        String border = "";
        if (isBorder) {
            border = ChatColor.translateAlternateColorCodes('&', config.get("border_color").getAsString());
            border += ChatColor.STRIKETHROUGH + "-----------------------------------------------------";
        }

        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            if (isBorder) {
                player.sendMessage(border);
            }

            String s = base.replace("%player%", player.getName());
            player.sendMessage(s);

            if (isBorder) {
                player.sendMessage(border);
            }
        }
        increment();
    }

    public void increment() {
        index++;
        if (index >= messages.size()) {
            index = 0;
        }
    }

    public boolean loadMessages() {
        try {
            String raw = GitUtil.getGitFile("https://raw.githubusercontent.com/" + config.get("profile").getAsString() + "/" + config.get("repository").getAsString() + "/master/" + config.get("file").getAsString());
            JsonObject jsonObject = new JsonParser().parse(raw).getAsJsonObject();

            index = 0;
            messages.clear();

            this.prefix = ChatColor.translateAlternateColorCodes('&', jsonObject.get("prefix").getAsString());

            Iterator<JsonElement> it = jsonObject.getAsJsonArray("messages").iterator();
            while (it.hasNext()) {
                String msg = it.next().getAsString();
                msg = ChatColor.translateAlternateColorCodes('&', msg);
                messages.add(msg);
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void createConfigFile() {
        JsonObject obj = new JsonObject();
        obj.addProperty("profile", "mchostco");
        obj.addProperty("repository", "Notifications");
        obj.addProperty("file", "hub.json");
        obj.addProperty("interval", 30);
        obj.addProperty("border", true);
        obj.addProperty("border_color", "&6");

        String s = new Gson().toJson(obj);

        new File("plugins/GitBroadcast").mkdirs();

        File myFile = new File("plugins/GitBroadcast/config.json");

        try {
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter =new OutputStreamWriter(fOut);
            myOutWriter.append(s);
            myOutWriter.close();
            fOut.close();

            Bukkit.getServer().getLogger().log(Level.FINEST, "Successfully created config.json file.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
