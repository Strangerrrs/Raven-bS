package keystrokesmod.utility.command.impl;

import keystrokesmod.module.impl.other.NameHider;
import keystrokesmod.utility.command.Command;

public class Cname extends Command {
    public Cname() {
        super("cname");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            NameHider.fakeName = args[1];
            chat("&7[&fcname&7] &7Name has been set to &b" + NameHider.fakeName + "&7.");
        }
        else {
            syntaxError();
        }
    }
}
