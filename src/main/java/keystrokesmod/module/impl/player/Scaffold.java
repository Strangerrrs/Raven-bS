package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixin.impl.accessor.IAccessorEntityPlayerSP;
import keystrokesmod.mixin.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private final SliderSetting sprintScaffoldMotion;
    private final SliderSetting fastScaffoldMotion;
    public SliderSetting rotation;
    private SliderSetting sprint;
    private SliderSetting fastScaffold;
    private SliderSetting multiPlace;
    public ButtonSetting autoSwap;
    private ButtonSetting cancelKnockBack;
    private ButtonSetting fastOnRMB;
    public ButtonSetting highlightBlocks;
    private ButtonSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting silentSwing;

    private String[] rotationModes = new String[] { "None", "Simple", "Offset", "Precise" };
    private String[] sprintModes = new String[] { "None", "Vanilla", "Float" };
    private String[] fastScaffoldModes = new String[] { "None", "Jump B", "Jump C", "Jump D", "Keep-Y A", "Keep-Y B", "Jump A" };
    private String[] multiPlaceModes = new String[] { "Disabled", "1 extra", "2 extra" };

    public Map<BlockPos, Timer> highlight = new HashMap<>();

    private ScaffoldBlockCount scaffoldBlockCount;

    public AtomicInteger lastSlot = new AtomicInteger(-1);

    public boolean hasSwapped;
    private boolean hasPlaced;

    private boolean rotateForward;
    private int onGroundTicks;
    private double startYPos = -1;
    public boolean fastScaffoldKeepY;
    private boolean firstKeepYPlace;
    private boolean rotatingForward;
    private int keepYTicks;
    private boolean lowhop;
    private int rotationDelay;
    private int blockSlot = -1;

    public boolean canBlockFade;

    private boolean floatJumped;
    private boolean floatStarted;
    private boolean floatWasEnabled;
    private boolean floatKeepY;

    private Vec3 targetBlock;
    private PlaceData blockInfo;
    private Vec3 hitVec, lookVec;
    private float[] blockRotations;
    private long rotationTimeout = 250L;
    private float lastYaw = 0.0f;
    public float yaw, pitch, blockYaw, yawOffset;
    private boolean set2;

    public boolean moduleEnabled;
    public boolean isEnabled;
    private boolean disabledModule;
    private boolean dontDisable, towerEdge;
    private int disableTicks;
    private int scaffoldTicks;

    private boolean was451, was452;

    private float minOffset;

    private long firstStroke, strokeDelay = 575;
    private float yawAngle;

    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(sprintScaffoldMotion = new SliderSetting("Sprint Scaffold Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(fastScaffoldMotion = new SliderSetting("Fast Scaffold Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(sprint = new SliderSetting("Sprint mode", 0, sprintModes));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));
        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));
        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(cancelKnockBack = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", true));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(jumpFacingForward = new ButtonSetting("Jump facing forward", false));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing", false));

        this.alwaysOn = true;
    }

    public void onDisable() {
        if (ModuleManager.tower.canTower() && (ModuleManager.tower.upCount == 0 || !Utils.isMoving())) {
            towerEdge = true;
        }
        disabledModule = true;
        moduleEnabled = false;
    }

    public void onEnable() {
        isEnabled = true;
        moduleEnabled = true;

        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(mc));
        lastSlot.set(-1);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(MouseEvent e) {
        if (!isEnabled) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        if (e.button >= 0) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        onGroundTicks = !mc.thePlayer.onGround ? 0 : ++onGroundTicks;
        if (!isEnabled) {
            return;
        }
        if (Utils.isMoving()) {
            scaffoldTicks++;
        }
        else {
            scaffoldTicks = 0;
        }
        canBlockFade = true;
        int simpleY = (int) Math.round((e.posY % 1) * 10000);
        if (Utils.keysDown() && usingFastScaffold() && fastScaffold.getInput() >= 1 && !ModuleManager.tower.canTower() && !ModuleManager.longJump.function) { // jump mode
            if (mc.thePlayer.onGround && Utils.isMoving()) {
                if (scaffoldTicks > 1) {
                    rotateForward();
                    mc.thePlayer.jump();
                    Utils.setSpeed(getSpeed(getSpeedLevel()) - Utils.randomizeDouble(0.0003, 0.0001));
                    if (fastScaffold.getInput() == 5 || fastScaffold.getInput() == 2 && firstKeepYPlace) {
                        lowhop = true;
                    }
                    if (startYPos == -1 || Math.abs(startYPos - e.posY) > 5) {
                        startYPos = e.posY;
                        fastScaffoldKeepY = true;
                    }
                }
            }
        }
        else if (fastScaffoldKeepY) {
            fastScaffoldKeepY = firstKeepYPlace = false;
            startYPos = -1;
            keepYTicks = 0;
        }
        if (lowhop) {
            switch (simpleY) {
                case 4200:
                    mc.thePlayer.motionY = 0.39;
                    break;
                case 1138:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.13;
                    break;
                case 2031:
                    mc.thePlayer.motionY = mc.thePlayer.motionY - 0.2;
                    lowhop = false;
                    break;
            }
        }

        //Float
        if (sprint.getInput() == 2 && !usingFastScaffold() && !ModuleManager.bHop.isEnabled() && !ModuleManager.tower.canTower() && !ModuleManager.longJump.function) {
            floatWasEnabled = true;
            if (!floatStarted) {
                if (onGroundTicks > 8 && mc.thePlayer.onGround) {
                    floatKeepY = true;
                    startYPos = e.posY;
                    mc.thePlayer.jump();
                    Utils.setSpeed(Utils.getHorizontalSpeed() - 0.1);
                    floatJumped = true;
                } else if (onGroundTicks <= 8 && mc.thePlayer.onGround) {
                    floatStarted = true;
                }
                if (floatJumped && !mc.thePlayer.onGround) {
                    floatStarted = true;
                }
            }

            if (floatStarted && mc.thePlayer.onGround) {
                floatKeepY = false;
                startYPos = -1;
                if (moduleEnabled) {
                    e.setPosY(e.getPosY() + 1E-12F);
                    if (Utils.isMoving()) Utils.setSpeed(getFloatSpeed(getSpeedLevel()));
                }
            }
        } else if (floatWasEnabled && moduleEnabled) {
            if (floatKeepY) {
                startYPos = -1;
            }
            floatStarted = floatJumped = floatKeepY = floatWasEnabled = false;
        }


        if (targetBlock != null) {
            Vec3 lookAt = new Vec3(targetBlock.xCoord - lookVec.xCoord, targetBlock.yCoord - lookVec.yCoord, targetBlock.zCoord - lookVec.zCoord);
            blockRotations = RotationUtils.getRotations(lookAt);
            targetBlock = null;
        }

        switch ((int) rotation.getInput()) {
            case 1:
                e.setRotations(mc.thePlayer.rotationYaw - hardcodedYaw(), 81.150F);
                break;
            case 2:
                float moveAngle = (float) getMovementAngle();
                float relativeYaw = mc.thePlayer.rotationYaw + moveAngle;
                float normalizedYaw = (relativeYaw % 360 + 360) % 360;
                float quad = normalizedYaw % 90;

                float side = MathHelper.wrapAngleTo180_float(getMotionYaw() - yaw);
                float offset = yawAngle;//(!Utils.scaffoldDiagonal(false)) ? 125.500F : 143.500F;
                float yawBackwards = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
                float blockYawOffset = MathHelper.wrapAngleTo180_float(yawBackwards - blockYaw);
                int quadVal = 0;

                float minPitch = 78.650f;
                // Adjust the pitch to look more down
                float adjustedPitch = 85.0f;

                float firstStraight = 123.50f;
                float secondStraight = 125.50f;
                float thirdStraight = 127.50f;
                float firstDiag = 128.50f;
                float secondDiag = 130.50f;
                float thirdDiag = 132.50f;
                float fourthDiag = 138f;

                float firstOffset = 16;
                float secondOffset = 14;
                float thirdOffset = 10;
                float fourthOffset = 9;
                float fifthOffset = 8;
                float sixthOffset = 5;
                float seventhOffset = 2;

                //first straight
                if (quad <= 5 || quad >= 85) {
                    yawAngle = firstStraight;
                    minOffset = firstOffset;
                    quadVal = 1;
                }
                else if (quad > 5 || quad < 85) {

                    //second straight
                    if (quad >= 80 || quad < 10) {
                        yawAngle = secondStraight;
                        minOffset = secondOffset;
                        quadVal = 2;

                        //third straight
                    } else if (quad >= 65 || quad < 25) {
                        yawAngle = thirdStraight;
                        minOffset = thirdOffset;
                        quadVal = 3;

                        //first diag
                    } else if (quad >= 55 || quad < 35) {
                        yawAngle = firstDiag;
                        minOffset = fourthOffset;
                        quadVal = 4;

                        //second diag
                    } else if (quad >= 15 && quad < 45) {
                        yawAngle = secondDiag;
                        minOffset = fifthOffset;
                        quadVal = 5;
                        if (quad >= 38) {
                            yawAngle = thirdDiag;
                            minOffset = sixthOffset;
                            quadVal = 6;
                            if (quad >= 42) {
                                yawAngle = fourthDiag;
                                minOffset = seventhOffset;
                                quadVal = 7;
                            }
                        }
                    } else {
                        yawAngle = secondDiag;
                        minOffset = fifthOffset;
                        quadVal = 5;
                        if (quad >= 45 && quad < 52) {
                            yawAngle = thirdDiag;
                            minOffset = sixthOffset;
                            quadVal = 6;
                            if (quad < 48) {
                                yawAngle = fourthDiag;
                                minOffset = seventhOffset;
                                quadVal = 7;
                            }
                        }
                    }
                }

                if (firstStroke > 0 && (System.currentTimeMillis() - firstStroke) > strokeDelay) {
                    firstStroke = 0;
                }

                if (blockRotations != null) {
                    blockYaw = blockRotations[0];
                    pitch = blockRotations[1];
                    yawOffset = blockYawOffset;
                    if (pitch < adjustedPitch && Utils.getHorizontalSpeed() < 0.6) {
                        pitch = adjustedPitch;
                    }
                    if (firstStroke == 0) {
                        strokeDelay = 300;
                    }
                } else {
                    firstStroke = System.currentTimeMillis();
                    yawOffset = 0;
                    pitch = adjustedPitch;
                    strokeDelay = 200;
                }
                minOffset = 0;//turning this off for now

                if (!Utils.isMoving() || Utils.getHorizontalSpeed() == 0.0D) {
                    e.setRotations(yaw, pitch);
                    break;
                }

                float motionYaw = getMotionYaw();

                float lastYaw = ((IAccessorEntityPlayerSP) mc.thePlayer).getLastReportedYaw();
                float newYaw = motionYaw - offset * Math.signum(
                        MathHelper.wrapAngleTo180_float(motionYaw - yaw)
                );
                yaw = applyGcd(
                        lastYaw + MathHelper.wrapAngleTo180_float(newYaw - lastYaw)
                );

                if (quadVal != 1) {
                    if (quad >= 0 && quad < 45F) {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = false;
                            } else {
                                set2 = true;
                            }
                        }
                        if (was452) {
                            firstStroke = System.currentTimeMillis();
                        }
                        was451 = true;
                        was452 = false;
                    } else {
                        if (firstStroke == 0) {
                            if (side >= 0) {
                                set2 = true;
                            } else {
                                set2 = false;
                            }
                        }
                        if (was451) {
                            firstStroke = System.currentTimeMillis();
                        }
                        was452 = true;
                        was451 = false;
                    }
                }

                double minSwitch = (!Utils.scaffoldDiagonal(false)) ? 0 : 15;
                if (side >= 0) {
                    if (quadVal == 1) {
                        if (yawOffset <= -minSwitch && firstStroke == 0) {
                            if (set2) {
                                firstStroke = System.currentTimeMillis();
                            }
                            set2 = false;
                        } else if (yawOffset >= 0 && firstStroke == 0) {
                            if (yawOffset >= minSwitch) {
                                if (!set2) {
                                    firstStroke = System.currentTimeMillis();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset <= -0) yawOffset = -0;
                        if (yawOffset >= minOffset) yawOffset = minOffset;
                        e.setRotations((yaw + offset * 2) - yawOffset, pitch);
                        break;
                    }
                } else if (side <= -0) {
                    if (quadVal == 1) {
                        if (yawOffset >= minSwitch && firstStroke == 0) {
                            if (set2) {
                                firstStroke = System.currentTimeMillis();
                            }
                            set2 = false;
                        } else if (yawOffset <= 0 && firstStroke == 0) {
                            if (yawOffset <= -minSwitch) {
                                if (!set2) {
                                    firstStroke = System.currentTimeMillis();
                                }
                                set2 = true;
                            }
                        }
                    }
                    if (set2) {
                        if (yawOffset >= 0) yawOffset = 0;
                        if (yawOffset <= -minOffset) yawOffset = -minOffset;
                        e.setRotations((yaw - offset * 2) - yawOffset, pitch);
                        break;
                    }
                }

                if (side >= 0) {
                    if (yawOffset >= 0) yawOffset = 0;
                    if (yawOffset <= -minOffset) yawOffset = -minOffset;
                } else if (side <= -0) {
                    if (yawOffset <= -0) yawOffset = -0;
                    if (yawOffset >= minOffset) yawOffset = minOffset;
                }
                e.setRotations(yaw - yawOffset, pitch);
                set2 = false;
                break;
            case 3:
                if (blockRotations != null) {
                    e.setRotations(blockRotations[0], blockRotations[1]);
                }
                else {
                    e.setRotations(mc.thePlayer.rotationYaw - hardcodedYaw(), 81.150F);
                }
                break;
        }

        //get yaw - player yaw offset
        float yv = MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - hardcodedYaw();
        if (Raven.debug) {
            Utils.sendModuleMessage(this, "" + MathHelper.wrapAngleTo180_float(yv - e.getYaw()) + " " + minOffset);
        }

        //jump facing forward
        if (ModHelper.inAirTicks >= 1) {
            rotateForward = false;
        }
        if (rotateForward && jumpFacingForward.isToggled()) {
            if (rotation.getInput() > 0) {
                if (!rotatingForward) {
                    rotationDelay = 2;
                    rotatingForward = true;
                }
                float forwardYaw = (mc.thePlayer.rotationYaw - hardcodedYaw() - 180 - (float) Utils.randomizeInt(-5, 5));
                e.setYaw(forwardYaw);
                e.setPitch(10 - (float) Utils.randomizeDouble(1, 5));
            }
        }
        else {
            rotatingForward = false;
        }

        if (ModuleManager.tower.isVerticalTowering()) {
            if (blockRotations != null) {
                e.setYaw(blockRotations[0]);
            }
            if (ModuleManager.tower.yaw != 0) {
                e.setYaw(ModuleManager.tower.yaw);
            }
            if (ModuleManager.tower.pitch != 0) {
                e.setPitch(ModuleManager.tower.pitch);
            }
        }

        //pitch fix
        if (e.getPitch() > 89.9F) {
            e.setPitch(89.9F);
        }

        lastYaw = mc.thePlayer.rotationYaw;
        if (rotationDelay > 0) --rotationDelay;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!isEnabled) {
            return;
        }
        if (holdingBlocks() && setSlot()) {
            if (LongJump.stopModules) {
                return;
            }
            if (KillAura.target != null || ModuleManager.killAura.stoppedTargeting) {
                return;
            }

            hasSwapped = true;
            int mode = (int) fastScaffold.getInput();
            if (rotation.getInput() == 0 || rotationDelay == 0) {
                placeBlock(0, 0);
            }
            if (ModuleManager.tower.placeExtraBlock) {
                placeBlock(0, -1);
            }
            if (fastScaffoldKeepY && !ModuleManager.tower.canTower()) {
                ++keepYTicks;
                if ((int) mc.thePlayer.posY > (int) startYPos) {
                    switch (mode) {
                        case 1:
                            if (!firstKeepYPlace && keepYTicks == 8 || keepYTicks == 11) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 2:
                            if (!firstKeepYPlace && keepYTicks == 8 || firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 3:
                            if (!firstKeepYPlace && keepYTicks == 7) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                        case 6:
                            if (!firstKeepYPlace && keepYTicks == 3) {
                                firstKeepYPlace = true;
                            }
                        case 7: // Jump A
                            if (!firstKeepYPlace && keepYTicks == 3) {
                                placeBlock(1, 0);
                                firstKeepYPlace = true;
                            }
                            break;
                    }
                }
                if (mc.thePlayer.onGround) keepYTicks = 0;
                if ((int) mc.thePlayer.posY == (int) startYPos) firstKeepYPlace = false;
            }
            handleMotion();
        }

        if (disabledModule) {
            if (hasPlaced && (towerEdge || floatStarted && Utils.isMoving())) {
                dontDisable = true;
            }

            if (dontDisable && ++disableTicks >= 2) {
                isEnabled = false;
            }
            if (!dontDisable) {
                isEnabled = false;
            }


            if (!isEnabled) {
                disabledModule = dontDisable = false;
                disableTicks = 0;

                if (ModuleManager.tower.speed) {
                    Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
                }

                if (lastSlot.get() != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot.get();
                    lastSlot.set(-1);
                }
                blockSlot = -1;
                if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
                    ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
                }
                scaffoldBlockCount.beginFade();
                hasSwapped = hasPlaced = false;
                targetBlock = null;
                blockInfo = null;
                blockRotations = null;
                fastScaffoldKeepY = firstKeepYPlace = rotateForward = rotatingForward = lowhop = floatStarted = floatJumped = floatWasEnabled = towerEdge =
                        was451 = was452 = false;
                rotationDelay = keepYTicks = scaffoldTicks = 0;
                firstStroke = 0;
                startYPos = -1;
                lookVec = null;
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!isEnabled) {
            return;
        }
        if (!Utils.nullCheck() || !cancelKnockBack.isToggled()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
        }
    }

    @Override
    public String getInfo() {
        String info;
        info = sprintModes[(int) sprint.getInput()];
        return info;
    }

    public boolean stopFastPlace() {
        return this.isEnabled();
    }

    float getAngleDifference(float from, float to) {
        float difference = (to - from) % 360.0F;
        if (difference < -180.0F) {
            difference += 360.0F;
        } else if (difference >= 180.0F) {
            difference -= 360.0F;
        }
        return difference;
    }

    public void rotateForward() {
        rotateForward = true;
        rotatingForward = false;
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public boolean sprint() {
        if (isEnabled) {
            return handleFastScaffolds() > 0 || !holdingBlocks();
        }
        return false;
    }

    private int handleFastScaffolds() {
        if (fastOnRMB.isToggled()) {
            return Mouse.isButtonDown(1) && Utils.tabbedIn() ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
        else {
            return fastScaffold.getInput() > 0 ? (int) fastScaffold.getInput() : (int) sprint.getInput();
        }
    }

    private boolean usingFastScaffold() {
        return fastScaffold.getInput() > 0 && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1) && Utils.tabbedIn());
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled();
    }

    public boolean stopRotation() {
        return this.isEnabled() && rotation.getInput() > 0;
    }

    private void place(PlaceData block) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.blockPos, block.enumFacing, block.hitVec)) {
            if (silentSwing.isToggled()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            else {
                mc.thePlayer.swingItem();
                if (!(autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled())) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            highlight.put(block.blockPos.offset(block.enumFacing), null);
            hasPlaced = true;
        }
    }

    public boolean canSafewalk() {
        if (usingFastScaffold()) {
            return false;
        }
        if (ModuleManager.tower.canTower()) {
            return false;
        }
        if (!isEnabled) {
            return false;
        }
        return true;
    }

    public int totalBlocks() {
        int totalBlocks = 0;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.thePlayer.inventory.mainInventory[i];
            if (stack != null && stack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) stack.getItem()) && stack.stackSize > 0) {
                totalBlocks += stack.stackSize;
            }
        }
        return totalBlocks;
    }

    private void placeBlock(int yOffset, int xOffset) {
        locateAndPlaceBlock(yOffset, xOffset);
        int input = (int) multiPlace.getInput();
        if (sprint.getInput() == 0 && mc.thePlayer.onGround && !ModuleManager.tower.canTower() && !usingFastScaffold()) {
            return;
        }
        if (ModuleManager.tower.canTower() && !ModuleManager.tower.tower) {
            return;
        }
        if (input >= 1) {
            locateAndPlaceBlock(yOffset, xOffset);
            if (input >= 2) {
                locateAndPlaceBlock(yOffset, xOffset);
            }
        }
    }

    private void locateAndPlaceBlock(int yOffset, int xOffset) {
        locateBlocks(yOffset, xOffset);
        if (blockInfo == null) {
            return;
        }
        place(blockInfo);
        blockInfo = null;
    }

    private void locateBlocks(int yOffset, int xOffset) {
        List<PlaceData> blocksInfo = findBlocks(yOffset, xOffset);

        if (blocksInfo == null) {
            return;
        }

        double sumX = 0, sumY = !mc.thePlayer.onGround ? 0 : blocksInfo.get(0).blockPos.getY(), sumZ = 0;
        int index = 0;
        for (PlaceData blockssInfo : blocksInfo) {
            if (index > 1 || (!Utils.isDiagonal(false) && index > 0 && mc.thePlayer.onGround)) {
                break;
            }
            sumX += blockssInfo.blockPos.getX();
            if (!mc.thePlayer.onGround) {
                sumY += blockssInfo.blockPos.getY();
            }
            sumZ += blockssInfo.blockPos.getZ();
            index++;
        }

        double avgX = sumX / index;
        double avgY = !mc.thePlayer.onGround ? sumY / index : blocksInfo.get(0).blockPos.getY();
        double avgZ = sumZ / index;

        targetBlock = new Vec3(avgX, avgY, avgZ);

        PlaceData blockInfo2 = blocksInfo.get(0);
        int blockX = blockInfo2.blockPos.getX();
        int blockY = blockInfo2.blockPos.getY();
        int blockZ = blockInfo2.blockPos.getZ();
        EnumFacing blockFacing = blockInfo2.enumFacing;
        blockInfo = blockInfo2;

        double hitX = (blockX + 0.5D) + getCoord(blockFacing.getOpposite(), "x") * 0.5D;
        double hitY = (blockY + 0.5D) + getCoord(blockFacing.getOpposite(), "y") * 0.5D;
        double hitZ = (blockZ + 0.5D) + getCoord(blockFacing.getOpposite(), "z") * 0.5D;
        lookVec = new Vec3(0.5D + getCoord(blockFacing.getOpposite(), "x") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "y") * 0.5D, 0.5D + getCoord(blockFacing.getOpposite(), "z") * 0.5D);
        hitVec = new Vec3(hitX, hitY, hitZ);
        blockInfo.hitVec = hitVec;
    }

    private double getCoord(EnumFacing facing, String axis) {
        switch (axis) {
            case "x": return (facing == EnumFacing.WEST) ? -0.5 : (facing == EnumFacing.EAST) ? 0.5 : 0;
            case "y": return (facing == EnumFacing.DOWN) ? -0.5 : (facing == EnumFacing.UP) ? 0.5 : 0;
            case "z": return (facing == EnumFacing.NORTH) ? -0.5 : (facing == EnumFacing.SOUTH) ? 0.5 : 0;
        }
        return 0;
    }

    private List<PlaceData> findBlocks(int yOffset, int xOffset) {
        List<PlaceData> possibleBlocks = new ArrayList<>();
        int x = (int) Math.floor(mc.thePlayer.posX + xOffset);
        int y = (int) Math.floor(((startYPos != -1) ? startYPos : (mc.thePlayer.posY)) + yOffset);
        int z = (int) Math.floor(mc.thePlayer.posZ);

        if (BlockUtils.replaceable(new BlockPos(x, y - 1, z))) {
            for (EnumFacing enumFacing : EnumFacing.values()) {
                if (enumFacing != EnumFacing.UP && placeConditions(enumFacing, yOffset, xOffset)) {
                    BlockPos offsetPos = new BlockPos(x, y - 1, z).offset(enumFacing);
                    if (!BlockUtils.replaceable(offsetPos) && !BlockUtils.isInteractable(BlockUtils.getBlock(offsetPos))) {
                        possibleBlocks.add(new PlaceData(offsetPos, enumFacing.getOpposite()));
                    }
                }
            }
            for (EnumFacing enumFacing2 : EnumFacing.values()) {
                if (enumFacing2 != EnumFacing.UP && placeConditions(enumFacing2, yOffset, xOffset)) {
                    BlockPos offsetPos2 = new BlockPos(x, y - 1, z).offset(enumFacing2);
                    if (BlockUtils.replaceable(offsetPos2)) {
                        for (EnumFacing enumFacing3 : EnumFacing.values()) {
                            if (enumFacing3 != EnumFacing.UP && placeConditions(enumFacing3, yOffset, xOffset)) {
                                BlockPos offsetPos3 = offsetPos2.offset(enumFacing3);
                                if (!BlockUtils.replaceable(offsetPos3) && !BlockUtils.isInteractable(BlockUtils.getBlock(offsetPos3))) {
                                    possibleBlocks.add(new PlaceData(offsetPos3, enumFacing3.getOpposite()));
                                }
                            }
                        }
                    }
                }
            }
            if (mc.thePlayer.motionY > -0.0784) {
                for (EnumFacing enumFacing5 : EnumFacing.values()) {
                    if (enumFacing5 != EnumFacing.UP && placeConditions(enumFacing5, yOffset, xOffset)) {
                        BlockPos offsetPos5 = new BlockPos(x, y - 2, z).offset(enumFacing5);
                        if (BlockUtils.replaceable(offsetPos5)) {
                            for (EnumFacing enumFacing6 : EnumFacing.values()) {
                                if (enumFacing6 != EnumFacing.UP && placeConditions(enumFacing6, yOffset, xOffset)) {
                                    BlockPos offsetPos6 = offsetPos5.offset(enumFacing6);
                                    if (!BlockUtils.replaceable(offsetPos6) && !BlockUtils.isInteractable(BlockUtils.getBlock(offsetPos6))) {
                                        possibleBlocks.add(new PlaceData(offsetPos6, enumFacing6.getOpposite()));
                                    }
                                }
                            }
                        }
                    }
                }
                for (EnumFacing enumFacing7 : EnumFacing.values()) {
                    if (enumFacing7 != EnumFacing.UP && placeConditions(enumFacing7, yOffset, xOffset)) {
                        BlockPos offsetPos7 = new BlockPos(x, y - 3, z).offset(enumFacing7);
                        if (BlockUtils.replaceable(offsetPos7)) {
                            for (EnumFacing enumFacing8 : EnumFacing.values()) {
                                if (enumFacing8 != EnumFacing.UP && placeConditions(enumFacing8, yOffset, xOffset)) {
                                    BlockPos offsetPos8 = offsetPos7.offset(enumFacing8);
                                    if (!BlockUtils.replaceable(offsetPos8) && !BlockUtils.isInteractable(BlockUtils.getBlock(offsetPos8))) {
                                        possibleBlocks.add(new PlaceData(offsetPos8, enumFacing8.getOpposite()));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        else {
            return null;
        }
        return possibleBlocks.isEmpty() ? null : possibleBlocks;
    }

    private boolean placeConditions(EnumFacing enumFacing, int yCondition, int xCondition) {
        if (xCondition == -1) {
            return enumFacing == EnumFacing.EAST;
        }
        if (yCondition == 1) {
            return enumFacing == EnumFacing.DOWN;
        }

        return true;
    }

    float applyGcd(float value) {
        float gcd = 0.2F * 0.2F * 0.2F * 8.0F;
        return (float) ((double) value - (double) value % ((double) gcd * 0.15D));
    }

    float getMotionYaw() {
        return MathHelper.wrapAngleTo180_float((float) Math.toDegrees(Math.atan2(mc.thePlayer.motionZ, mc.thePlayer.motionX)) - 90.0F);
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    double[] speedLevels = {0.48, 0.5, 0.52, 0.58, 0.68};

    double getSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return speedLevels[speedLevel];
        }
        return speedLevels[0];
    }

    double[] floatSpeedLevels = {0.2, 0.22, 0.28, 0.29, 0.3};

    double getFloatSpeed(int speedLevel) {
        if (speedLevel >= 0) {
            return floatSpeedLevels[speedLevel];
        }
        return floatSpeedLevels[0];
    }

    private void handleMotion() {
        if (ModuleManager.tower.canTower() || !mc.thePlayer.onGround) {
            return;
        }
        if (usingFastScaffold()) {
            mc.thePlayer.motionX *= fastScaffoldMotion.getInput();
            mc.thePlayer.motionZ *= fastScaffoldMotion.getInput();
        } else {
            mc.thePlayer.motionX *= sprintScaffoldMotion.getInput();
            mc.thePlayer.motionZ *= sprintScaffoldMotion.getInput();
        }
    }

    public float hardcodedYaw() {
        float simpleYaw = 0F;
        float f = 0.8F;

        if (mc.thePlayer.moveForward >= f) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 45;
        }
        else if (mc.thePlayer.moveForward == 0) {
            simpleYaw -= 180;
            if (mc.thePlayer.moveStrafing >= f) simpleYaw += 90;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw -= 90;
        }
        else if (mc.thePlayer.moveForward <= -f) {
            if (mc.thePlayer.moveStrafing >= f) simpleYaw -= 45;
            if (mc.thePlayer.moveStrafing <= -f) simpleYaw += 45;
        }
        return simpleYaw;
    }

    public boolean holdingBlocks() {
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return false;
            }
        }
        return true;
    }

    private double getMovementAngle() {
        double angle = Math.toDegrees(Math.atan2(-mc.thePlayer.moveStrafing, mc.thePlayer.moveForward));
        return angle == -0 ? 0 : angle;
    }

    private int getSlot() {
        int slot = -1;
        int highestStack = -1;
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        for (int i = 0; i < 9; ++i) {
            final ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack != null && itemStack.getItem() instanceof ItemBlock && Utils.canBePlaced((ItemBlock) itemStack.getItem()) && itemStack.stackSize > 0) {
                if (Utils.getBedwarsStatus() == 2 && ((ItemBlock) itemStack.getItem()).getBlock() instanceof BlockTNT) {
                    continue;
                }
                if (itemStack != null && heldItem != null && (heldItem.getItem() instanceof ItemBlock) && Utils.canBePlaced((ItemBlock) heldItem.getItem()) && ModuleManager.autoSwap.sameType.isToggled() && !(itemStack.getItem().getClass().equals(heldItem.getItem().getClass()))) {
                    continue;
                }
                if (itemStack.stackSize > highestStack) {
                    highestStack = itemStack.stackSize;
                    slot = i;
                }
            }
        }
        return slot;
    }

    public boolean setSlot() {
        int slot = getSlot();
        if (slot == -1) {
            return false;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            if (ModuleManager.autoSwap.swapToGreaterStack.isToggled()) {
                mc.thePlayer.inventory.currentItem = slot;
            }
            else {
                mc.thePlayer.inventory.currentItem = blockSlot;
            }
        }

        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return false;
        }
        return true;
    }


    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;
        Vec3 hitVec;

        PlaceData(BlockPos blockPos, EnumFacing enumFacing) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }
    }
}
