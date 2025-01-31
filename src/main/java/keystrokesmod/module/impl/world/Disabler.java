package keystrokesmod.module.impl.world;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;

public class Disabler extends Module {
    private SliderSetting mode;
    private String[] modes = new String[]{"Meow", "Full", "00", "SprintCancel"};

    public Disabler() {
        super("Disabler", Module.category.world);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @Override
    public void onUpdate() {
        if (modes[(int) mode.getInput()].equals("SprintCancel")) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0BPacketEntityAction(mc.thePlayer, Action.STOP_SPRINTING));
        }
    }
}
