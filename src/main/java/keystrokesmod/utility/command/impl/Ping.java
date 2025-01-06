package keystrokesmod.utility.command.impl;

import keystrokesmod.utility.command.Command;

public class Ping extends Command {
    public Ping() {
        super("ping");
    }

    @Override
    public void onExecute(String[] args) {
        keystrokesmod.utility.Ping.checkPing(true);
    }
}
