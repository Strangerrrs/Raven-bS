package keystrokesmod.module.impl.movement;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PostPlayerInputEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import keystrokesmod.event.PreUpdateEvent;
import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;

public class BHop extends Module {
    private SliderSetting mode;
    public static SliderSetting speedSetting;
    private ButtonSetting autoJump;
    private ButtonSetting disableInInventory;
    private ButtonSetting liquidDisable;
    private ButtonSetting sneakDisable;
    private ButtonSetting stopMotion;
    private ButtonSetting watchdogMode;
    private ButtonSetting airStrafe;
    private ButtonSetting disableOnScaffold;
    private String[] modes = new String[]{"Strafe", "Ground", "8-Tick", "7-Tick (Old)"};
    public boolean hopping;
    private boolean collided, strafe, down;
    private int Strafies = 0;

    public BHop() {
        super("Bhop", Module.category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", 0, modes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 1.0, 0.5, 8.0, 0.1));
        this.registerSetting(autoJump = new ButtonSetting("Auto jump", true));
        this.registerSetting(disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(liquidDisable = new ButtonSetting("Disable in liquid", true));
        this.registerSetting(sneakDisable = new ButtonSetting("Disable while sneaking", true));
        this.registerSetting(watchdogMode = new ButtonSetting("Watchdog Mode", false));
        this.registerSetting(airStrafe = new ButtonSetting("4-Tick AirStrafe", true));
        this.registerSetting(disableOnScaffold = new ButtonSetting("Disable on Scaffold", true));
    }

    @Override
    public String getInfo() {
        return modes[(int) mode.getInput()];
    }

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        if (hopping) {
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (((mc.thePlayer.isInWater() || mc.thePlayer.isInLava()) && liquidDisable.isToggled()) || (mc.thePlayer.isSneaking() && sneakDisable.isToggled())) {
            return;
        }
        if (disableInInventory.isToggled() && Settings.inInventory()) {
            return;
        }
        if (ModuleManager.bedAura.isEnabled() && ModuleManager.bedAura.disableBHop.isToggled() && ModuleManager.bedAura.currentBlock != null && RotationUtils.inRange(ModuleManager.bedAura.currentBlock, ModuleManager.bedAura.range.getInput())) {
            return;
        }
        if (disableOnScaffold.isToggled() && ModuleManager.scaffold.isEnabled()) {
            return;
        }
        if (watchdogMode.isToggled() && mc.gameSettings.keyBindBack.isKeyDown()) {
            if (mc.thePlayer.onGround && autoJump.isToggled()) {
                mc.thePlayer.jump();
            }
            hopping = false;
            return;
        }
        switch ((int) mode.getInput()) {
            case 0:
                if (Utils.isMoving()) {
                    if (mc.thePlayer.onGround && autoJump.isToggled()) {
                        mc.thePlayer.jump();
                    }
                    mc.thePlayer.setSprinting(true);
                    Utils.setSpeed(Utils.getHorizontalSpeed() + 0.005 * speedSetting.getInput());
                    hopping = true;
                    break;
                }
                break;
            case 1:
            case 2:
            case 3:
                if (mc.thePlayer.isCollidedHorizontally) {
                    collided = true;
                }
                else if (mc.thePlayer.onGround) {
                    collided = false;
                }
                if (Utils.isMoving() && mc.thePlayer.onGround) {
                    strafe = down = false;
                    if (autoJump.isToggled()) {
                        mc.thePlayer.jump();
                    }
                    else if (!Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()) && !autoJump.isToggled()) {
                        return;
                    }
                    mc.thePlayer.setSprinting(true);
                    double horizontalSpeed = Utils.getHorizontalSpeed();
                    double speedModifier = 0.4847;
                    final int speedAmplifier = Utils.getSpeedAmplifier();
                    switch (speedAmplifier) {
                        case 1:
                            speedModifier = 0.5252;
                            break;
                        case 2:
                            speedModifier = 0.587;
                            break;
                        case 3:
                            speedModifier = 0.6289;
                            break;
                    }
                    double additionalSpeed = speedModifier * ((speedSetting.getInput() - 1.0) / 3.0 + 1.0);
                    if (horizontalSpeed < additionalSpeed) {
                        horizontalSpeed = additionalSpeed;
                    }
                    Utils.setSpeed(horizontalSpeed);
                    hopping = true;
                }
                if ((mode.getInput() == 2 || mode.getInput() == 3) && Utils.isMoving()) {
                    int simpleY = (int) Math.round((e.posY % 1) * 10000);

                    if (mc.thePlayer.hurtTime == 0 && Utils.isMoving() && !collided) {
                        if (mode.getInput() == 2) { // 8-Tick
                            if (simpleY == 13) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.02483;
                            }
                            if (simpleY == 2000) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.1913;
                            }
                        } else if (mode.getInput() == 3) { // 7-Tick (Old)
                            if (simpleY == 4200) {
                                mc.thePlayer.motionY = 0.39;
                            }
                            if (simpleY == 1138) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                            }
                            if (simpleY == 2031) {
                                mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                            }
                        }

                        if (simpleY == 13) {
                            down = true;
                        }
                        if (down) {
                            e.posY -= 1E-5;
                        }

                        if (simpleY == 3426) {
                            strafe = true;
                        }
                        if (strafe) {
                            Utils.setSpeed(Utils.getHorizontalSpeed());
                        }
                    }
                }
                break;
        }
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (canstrafe()) {
            Strafies = mc.thePlayer.onGround ? 0 : Strafies + 1;

            if (mc.thePlayer.fallDistance > 1 || mc.thePlayer.onGround) {
                Strafies = 0;
                return;
            }

            if (Strafies == 1) {
                strafe();
            }

            if (!blockRelativeToPlayer(0, mc.thePlayer.motionY, 0).getUnlocalizedName().contains("air") && Strafies > 2) {
                strafe();
            }

            if (airStrafe.isToggled() && Strafies >= 2 && (!blockRelativeToPlayer(0, mc.thePlayer.motionY * 3, 0).getUnlocalizedName().contains("air") || Strafies == 9) && !ModuleManager.scaffold.isEnabled()) {
                mc.thePlayer.motionY += 0.0754;
                strafe();
            }
        }
    }

    private boolean canstrafe() {
        return mc.thePlayer.hurtTime == 0
            && !mc.thePlayer.isUsingItem()
            && mc.gameSettings.keyBindForward.isKeyDown();
    }

    private void strafe() {
        Utils.setSpeed(Utils.getHorizontalSpeed());
    }

    private Block blockRelativeToPlayer(double offsetX, double offsetY, double offsetZ) {
        Vec3 pos = mc.thePlayer.getPositionVector();
        double x = pos.xCoord + offsetX;
        double y = pos.yCoord + offsetY;
        double z = pos.zCoord + offsetZ;

        BlockPos blockPos = new BlockPos(Math.floor(x), Math.floor(y), Math.floor(z));
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }
}
