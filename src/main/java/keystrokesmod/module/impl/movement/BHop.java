package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BHop extends Module {
    public SliderSetting mode;
    public static SliderSetting speedSetting;
    private ButtonSetting liquidDisable;
    private ButtonSetting sneakDisable;
    public ButtonSetting rotateYaw;
    public String[] modes = new String[] {"Strafe", "Ground", "8 tick", "7 tick"};
    public boolean hopping, lowhop, didMove, collided, setRotation;

    public BHop() {
        super("BHop", Module.category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 2.0, 0.5, 8.0, 0.1));
        this.registerSetting(liquidDisable = new ButtonSetting("Disable in liquid", true));
        this.registerSetting(sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(rotateYaw = new ButtonSetting("Rotate yaw", false));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (!mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
            return;
        }
        if (hopping) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && liquidDisable.isToggled()) || (mc.thePlayer.isSneaking() && sneakDisable.isToggled())) {
            return;
        }
        if (ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.disableBHop.isToggled() && ModuleManager.bedAura.currentBlock != null && RotationUtils.inRange(ModuleManager.bedAura.currentBlock, ModuleManager.bedAura.range.getInput())) {
            return;
        }
        if (ModuleManager.scaffold.moduleEnabled && (ModuleManager.tower.canTower() || ModuleManager.scaffold.fastScaffoldKeepY)) {
            return;
        }
        if (!Utils.isMoving()) {
            return;
        }
        if (mode.getInput() >= 1) {
            if (mc.thePlayer.isCollidedHorizontally) {
                collided = true;
            } else if (mc.thePlayer.onGround) {
                collided = false;
            }
            if (mc.thePlayer.onGround) {
                if (mc.thePlayer.moveForward <= -0.5 && mc.thePlayer.moveStrafing == 0 && KillAura.target == null && !Utils.noSlowingBackWithBow() && !ModuleManager.scaffold.isEnabled && !mc.thePlayer.isCollidedHorizontally) {
                    setRotation = true;
                }
                mc.thePlayer.jump();
                double horizontalSpeed = Utils.getHorizontalSpeed();
                double speedModifier = 0.48;
                final int speedAmplifier = Utils.getSpeedAmplifier();
                switch (speedAmplifier) {
                    case 1:
                        speedModifier = 0.5;
                        break;
                    case 2:
                        speedModifier = 0.52;
                        break;
                    case 3:
                        speedModifier = 0.58;
                        break;
                }
                double additionalSpeed = speedModifier * ((speedSetting.getInput() - 1.0) / 3.0 + 1.0);
                if (horizontalSpeed < additionalSpeed) {
                    horizontalSpeed = additionalSpeed;
                }
                if (Utils.isMoving() && !Utils.noSlowingBackWithBow() && !ModuleManager.sprint.disableBackwards()) {
                    Utils.setSpeed(horizontalSpeed);
                    didMove = true;
                }
                hopping = true;
            }
        }
        switch ((int) mode.getInput()) {
            case 0:
                if (Utils.isMoving()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump();
                    }
                    mc.thePlayer.setSprinting(true);
                    Utils.setSpeed(Utils.getHorizontalSpeed() + 0.005 * speedSetting.getInput());
                    hopping = true;
                    break;
                }
                break;
            case 2:
                if (mode.getInput() == 2 && didMove) {
                    int simpleY = (int) Math.round((e.posY % 1) * 10000);

                    if (mc.thePlayer.hurtTime == 0 && !collided) {
                        switch (simpleY) {
                            case 13:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483;
                                break;
                            case 2000:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913;
                                didMove = false;
                                break;
                        }
                    }
                }
                break;
            case 3:
                if (mode.getInput() == 3 && didMove) {
                    int simpleY = (int) Math.round((e.posY % 1) * 10000);

                    if (mc.thePlayer.hurtTime == 0 && !collided) {
                        switch (simpleY) {
                            case 4200:
                                mc.thePlayer.motionY = 0.39;
                                lowhop = true;
                                break;
                            case 1138:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                                lowhop = false;
                                break;
                            case 2031:
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                                didMove = false;
                                break;
                        }
                    }
                }
                break;
        }
    }

    public void onDisable() {
        hopping = false;
    }
}