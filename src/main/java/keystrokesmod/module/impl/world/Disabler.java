package keystrokesmod.module.impl.world;

import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0BPacketEntityAction.Action;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

public class Disabler extends Module {

    boolean isJumping = false;
    boolean isDisabling = false;
    int tickCounter = 0;
    public boolean isDisabled = false;
    int lastTickTime = 0;
    boolean isInLobby = false;

    private SliderSetting mode;
    private String[] modes = new String[]{"Meow", "Full", "00", "SprintCancel", "MotionDisabler"};

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

    public void packet(ReceivePacketEvent event) {
        if (modes[(int) mode.getInput()].equals("MotionDisabler")) {
            if (!this.isEnabled()) {
                return;
            }
            if (event.getPacket() instanceof S02PacketChat) {
                if (((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText().toLowerCase().contains("joined the lobby")) {
                    isInLobby = true;
                }
            }

            if (event.getPacket() instanceof S07PacketRespawn) {
                isJumping = true;
            }

            if (event.getPacket() instanceof S08PacketPlayerPosLook) {
                tickCounter++;
                if (tickCounter == 23) {
                    isDisabling = false;
                    tickCounter = 0;
                    lastTickTime = mc.thePlayer.ticksExisted - lastTickTime;
                    Utils.sendMessage("Disabling Watchdog");
                    if (tickCounter == 90) {
                        Utils.sendMessage("Disabled jump checks");
                        isDisabled = true;
                    } else if (tickCounter < 23) {
                        mc.thePlayer.motionY = mc.thePlayer.motionZ = mc.thePlayer.motionX = 0;
                    }
                }
            }
        }
    }
}
