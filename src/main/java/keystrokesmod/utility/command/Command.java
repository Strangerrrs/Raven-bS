package keystrokesmod.utility.command;

import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    protected String command;
    protected String[] alias;
    public Minecraft mc = Minecraft.getMinecraft();

    public Command(String command, String[] alias) {
        this.command = command;
        this.alias = alias;
    }

    public Command(String command) {
        this.command = command;
        this.alias = new String[]{command};
    }

    public abstract void onExecute(String[] args);

    public List<String> tabComplete(String[] args) {
        return new ArrayList<>();
    }

    protected void chatWithPrefix(String msg) {
        Utils.sendMessage("&7[&f" + this.command + "&7] &r" + (ModuleManager.lowercaseChatCommands() ? msg.toLowerCase() : msg));
    }

    protected void chat(String msg) {
        Utils.sendMessage(ModuleManager.lowercaseChatCommands() ? msg.toLowerCase() : msg);
    }

    protected void syntaxError() {
        Utils.sendMessage("§cSyntax error");
    }
}
