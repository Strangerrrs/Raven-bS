package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.utility.command.Command;

public class Debug extends Command {
    public Debug() {
        super("debug");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length <= 1) {
            Raven.debug = !Raven.debug;
            chatWithPrefix("&7Debug " + (Raven.debug ? "&aenabled" : "&cdisabled") + "&7.");
        }
        else {
            syntaxError();
        }
    }
}
