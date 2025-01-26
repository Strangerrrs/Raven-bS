package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.Utils;
import keystrokesmod.utility.command.Command;

public class Enemy extends Command {
    public Enemy() {
        super("enemy",  new String[] { "enemy", "e" });
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length == 2) {
            if (args[1].equals("clear")) {
                chat("&7[&fenemy&7] &b" + Utils.enemies.size() + " &7enem" + (Utils.enemies.size() == 1 ? "y" : "ies") + " cleared.");
                Utils.enemies.clear();
                return;
            }

            boolean added = Utils.addEnemy(args[1]);
            if (!added) {
                Utils.removeEnemy(args[1]);
            }
        }
        else {
            syntaxError();
        }
    }
}
