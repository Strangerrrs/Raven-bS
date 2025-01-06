package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;

public class Binds extends Command {
    public Binds() {
        super("binds");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length <= 1) {
            HashMap<String, String> binds = getBindsModulesMap(0);
            chatWithPrefix("&b" + binds.size() + " &7module" + (binds.size() == 1 ? "" : "s") + " have keybinds.");
            for (Map.Entry<String, String> bindsMap : binds.entrySet()) {
                chatWithPrefix(" &b" + bindsMap.getKey() + " &7" + bindsMap.getValue());
            }
        }
        else if (args.length == 2) {
            int keycode = Keyboard.getKeyIndex(args[1].toUpperCase());
            if (keycode == 0) {
                chatWithPrefix("&7Invalid key.");
                return;
            }
            HashMap<String, String> binds = getBindsModulesMap(keycode);
            chatWithPrefix("&b" + binds.size() + " &7module" + (binds.size() == 1 ? "" : "s") + " has keybind &b" + args[1].toUpperCase() + "&7.");
            for (Map.Entry<String, String> bindsMap : binds.entrySet()) {
                chatWithPrefix(" &b" + bindsMap.getKey() + " &7" + bindsMap.getValue());
            }
        }
        else {
            syntaxError();
        }
    }

    private HashMap<String, String> getBindsModulesMap(int keycode) {
        HashMap<String, String> binds = new HashMap<>();
        for (Module module : ModuleManager.modules) {
            if (module.getKeycode() == 0) {
                continue;
            }
            if (keycode != 0 && module.getKeycode() != keycode) {
                continue;
            }
            binds.put((module.getKeycode() >= 1000 ? "M" + (module.getKeycode() - 1000) : Keyboard.getKeyName(module.getKeycode())), module.getName());
        }
        for (Profile profile : Raven.profileManager.profiles) {
            Module module = profile.getModule();
            if (module.getKeycode() == 0) {
                continue;
            }
            if (keycode != 0 && module.getKeycode() != keycode) {
                continue;
            }
            binds.put((module.getKeycode() >= 1000 ? "M" + (module.getKeycode() - 1000) : Keyboard.getKeyName(module.getKeycode())), module.getName());
        }
        for (Module module : Raven.scriptManager.scripts.values()) {
            if (module.getKeycode() == 0) {
                continue;
            }
            if (keycode != 0 && module.getKeycode() != keycode) {
                continue;
            }
            binds.put((module.getKeycode() >= 1000 ? "M" + (module.getKeycode() - 1000) : Keyboard.getKeyName(module.getKeycode())), module.getName());
        }
        return binds;
    }
}
