package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.BlockUtils;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NoFall extends Module {
    public SliderSetting mode;
    private SliderSetting minFallDistance;
    private ButtonSetting disableAdventure;
    private ButtonSetting ignoreVoid;
    private SliderSetting packetTimerSpeed;
    private String[] modes = new String[]{"Spoof", "Packet", "NoGround"};
    private double initialY;

    public NoFall() {
        super("NoFall", category.player);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(minFallDistance = new SliderSetting("Minimum fall distance", 3, 0, 10, 0.1));
        this.registerSetting(disableAdventure = new ButtonSetting("Disable adventure", false));
        this.registerSetting(ignoreVoid = new ButtonSetting("Ignore void", true));
        this.registerSetting(packetTimerSpeed = new SliderSetting("Packet Timer", 0.7, 0.1, 2.0, 0.1));
    }

    public void onDisable() {
        Utils.resetTimer();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (reset()) {
            Utils.resetTimer();
            initialY = mc.thePlayer.posY;
            return;
        }
        double predictedY = mc.thePlayer.posY + mc.thePlayer.motionY;
        double distanceFallen = initialY - predictedY;
        if (((double) mc.thePlayer.fallDistance > minFallDistance.getInput() || minFallDistance.getInput() == 0) || mode.getInput() == 2) {
            switch ((int) mode.getInput()) {
                case 0:
                    e.setOnGround(true);
                    break;
                case 1:
                    if (distanceFallen >= minFallDistance.getInput()) {
                        Utils.getTimer().timerSpeed = (float) packetTimerSpeed.getInput();
                        PacketUtils.sendPacketNoEvent(new C03PacketPlayer(true));
                        initialY = mc.thePlayer.posY;
                        if (Raven.debug) {
                            Utils.sendMessage("&7nofalling");
                        }
                    }
                    break;
                case 2:
                    e.setOnGround(false);
                    break;
            }
        }
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    private boolean isVoid() {
        return Utils.overVoid(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
    }

    private boolean reset() {
        if (disableAdventure.isToggled() && mc.playerController.getCurrentGameType().isAdventure()) {
            return true;
        }
        if (ignoreVoid.isToggled() && isVoid()) {
            return true;
        }
        if (Utils.isBedwarsPractice()) {
            return true;
        }
        if (mc.thePlayer.onGround) {
            return true;
        }
        if (BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY- 1, mc.thePlayer.posZ)) != Blocks.air) {
            return true;
        }
        if (mc.thePlayer.motionY > 0) {
            return true;
        }
        return false;
    }
}