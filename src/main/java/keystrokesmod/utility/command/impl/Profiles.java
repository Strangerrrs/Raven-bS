package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Profiles extends Command { // credit: https://github.com/BoxDevelopment
    public Profiles() {
        super("profiles", new String[]{"profile", "p"});
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length < 2) {
            List<Profile> profiles = Raven.profileManager.profiles;
            if (profiles.isEmpty()) {
                chatWithPrefix("&7No profiles found");
                return;
            }
            chatWithPrefix("&b" + profiles.size() + " &7profile" + (profiles.size() == 1 ? "" : "s") + " loaded.");
            for (Profile profile : profiles) {
                chatWithPrefix(" &7" + profile.getName() +
                        (profile == Raven.currentProfile ? " &7(&bcurrent&7)" : ""));
            }
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch(subCommand) {
            case "save":
            case "s":
                if (args.length < 3) {
                    if (Raven.currentProfile != null) {
                        Utils.sendMessage("&7Saved profile: &b" + Raven.currentProfile);
                        Raven.profileManager.saveProfile(Raven.currentProfile);
                    }
                    else {
                        syntaxError();
                    }
                    return;
                }
                String saveName = args[2];
                Profile newProfile = new Profile(saveName, 0);
                Raven.profileManager.saveProfile(newProfile);
                chatWithPrefix("&7Saved profile: &b" + saveName);
                Raven.profileManager.loadProfiles();
                break;
            case "load":
            case "l":
                if (args.length < 3) {
                    syntaxError();
                    return;
                }
                String loadName = args[2];
                if (Raven.profileManager.getProfile(loadName) == null) {
                    chatWithPrefix("&b" + loadName + " &7does not exist");
                    return;
                }
                Raven.profileManager.loadProfile(loadName);
                chatWithPrefix("&7Enabled profile: &b" + loadName);
                break;
            case "delete":
            case "remove":
            case "r":
                if (args.length < 3) {
                    syntaxError();
                    return;
                }
                String deleteName = args[2];
                if (Raven.profileManager.getProfile(deleteName) == null) {
                    chatWithPrefix("&cProfile &b" + deleteName + " &7does not exist");
                    return;
                }
                Raven.profileManager.deleteProfile(deleteName);
                chatWithPrefix("&7Removed profile: &b" + deleteName);
                Raven.profileManager.loadProfiles();
                break;
            case "rename":
                if (args.length < 4) {
                    syntaxError();
                    return;
                }
                String oldName = args[2];
                String newName = args[3];
                Profile oldProfile = Raven.profileManager.getProfile(oldName);
                if (oldProfile == null) {
                    chatWithPrefix("&b" + oldName + " &7does not exist");
                    return;
                }
                if (Raven.profileManager.getProfile(newName) != null) {
                    chatWithPrefix("&b" + newName + " &7already exists");
                    return;
                }
                Profile renamedProfile = new Profile(newName, oldProfile.getBind()); // save this to that pls
                Raven.profileManager.saveProfile(renamedProfile);
                Raven.profileManager.deleteProfile(oldName); // delete old shit (i think thats needed)
                chatWithPrefix("&b" + oldName + " &7renamed to &b" + newName);
                Raven.profileManager.loadProfiles(); // ok so fun fact!! you need to load them again (i think??)
                Raven.profileManager.loadProfile(newName);
                break;
            default:
                syntaxError();
                break;
        }
    }

    @Override
    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            return filterStartingWith(args[1], Arrays.asList("save", "s", "load", "l", "delete", "remove", "r", "rename"));
        }
        else if (args.length == 3 && !args[1].equalsIgnoreCase("save") && !args[1].equalsIgnoreCase("s")) {
            List<String> profileNames = new ArrayList<>();
            for (Profile profile : Raven.profileManager.profiles) {
                profileNames.add(profile.getName());
            }
            return filterStartingWith(args[2], profileNames);
        }
        return new ArrayList<>();
    }

    private List<String> filterStartingWith(String prefix, List<String> options) {
        List<String> filtered = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(prefix.toLowerCase())) {
                filtered.add(option);
            }
        }
        return filtered;
    }
}