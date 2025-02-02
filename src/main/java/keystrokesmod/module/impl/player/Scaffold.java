package keystrokesmod.module.impl.player;

import keystrokesmod.event.JumpEvent;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.mixins.interfaces.IMixinItemRenderer;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import keystrokesmod.utility.Timer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;
import net.minecraft.init.Blocks;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scaffold extends Module {
    private SliderSetting motion;
    private SliderSetting rotation;
    private SliderSetting fastScaffold;
    private SliderSetting fastScaffoldMotion;
    private SliderSetting precision;
    private SliderSetting multiPlace;
    public ButtonSetting autoSwap;
    private ButtonSetting cancelKnockBack;
    private ButtonSetting fastOnRMB;
    private ButtonSetting highlightBlocks;
    private SliderSetting jumpFacingForward;
    public ButtonSetting safeWalk;
    public ButtonSetting showBlockCount;
    private ButtonSetting slowOnEnable;
    private ButtonSetting delayOnJump;
    private SliderSetting silentSwing;
    public ButtonSetting tower;
    private ButtonSetting disableStrafing;
    private SliderSetting offsetForwardYaw;
    private SliderSetting offsetDiagonalYaw;
    private MovingObjectPosition placeBlock;
    public AtomicInteger lastSlot = new AtomicInteger(-1);
    private String[] rotationModes = new String[]{"None", "Simple", "Strict", "Offset", "Dynamic"};
    private String[] fastScaffoldModes = new String[]{"Disabled", "Edge", "Sprint", "Keep-Y A", "Keep-Y B", "Keep-Y C", "Same-Y", "Float"};
    private String[] precisionModes = new String[]{"Very low", "Low", "Moderate", "High", "Very high"};
    private String[] multiPlaceModes = new String[]{"Disabled", "1 extra", "2 extra"};
    private String[] fallbackRotationModes = new String[]{"None", "Closest", "Centered"};
    private SliderSetting fallbackRotation;
    private String[] jumpFacingForwardModes = new String[]{"None", "Semi", "Full"};
    public float placeYaw;
    public float placePitch;
    public int at;
    public int index;
    public boolean rmbDown;
    private double startPos = -1;
    private Map<BlockPos, Timer> highlight = new HashMap<>();
    private boolean forceStrict;
    private boolean down;
    private boolean delay;
    private boolean place;
    private int add;
    private boolean placedUp;
    private float previousRotation[];
    private int blockSlot = -1;
    public int blocksPlaced;
    public BlockPos previousBlock;
    private EnumFacing[] facings = { EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.UP };
    private BlockPos[] offsets = { new BlockPos(-1, 0, 0), new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(0, -1, 0) };
    private ScaffoldBlockCount scaffoldBlockCount;
    private boolean wasMoving = false;
    private boolean spaceReleased = true;
    private ButtonSetting adjustToSurroundings;
    private ButtonSetting autoJump;
    private boolean initialJumpDone = false;
    public Scaffold() {
        super("Scaffold", category.player);
        this.registerSetting(motion = new SliderSetting("Motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(rotation = new SliderSetting("Rotation", 1, rotationModes));
        this.registerSetting(fallbackRotation = new SliderSetting("Fallback Rotation", 0, fallbackRotationModes));
        this.registerSetting(jumpFacingForward = new SliderSetting("Jump facing forward", 0, jumpFacingForwardModes));
        this.registerSetting(fastScaffold = new SliderSetting("Fast scaffold", 0, fastScaffoldModes));
        this.registerSetting(fastScaffoldMotion = new SliderSetting("Fast scaffold motion", "x", 1.0, 0.5, 1.2, 0.01));
        this.registerSetting(precision = new SliderSetting("Precision", 4, precisionModes));
        this.registerSetting(multiPlace = new SliderSetting("Multi-place", 0, multiPlaceModes));
        this.registerSetting(adjustToSurroundings = new ButtonSetting("Adjust to surroundings", true));
        this.registerSetting(autoJump = new ButtonSetting("Auto jump", true));
        this.registerSetting(autoSwap = new ButtonSetting("Auto swap", true));
        this.registerSetting(cancelKnockBack = new ButtonSetting("Cancel knockback", false));
        this.registerSetting(delayOnJump = new ButtonSetting("Delay on jump", true));
        this.registerSetting(fastOnRMB = new ButtonSetting("Fast on RMB", false));
        this.registerSetting(highlightBlocks = new ButtonSetting("Highlight blocks", true));
        this.registerSetting(safeWalk = new ButtonSetting("Safewalk", true));
        this.registerSetting(showBlockCount = new ButtonSetting("Show block count", true));
        this.registerSetting(silentSwing = new SliderSetting("Silent swing", 0, 0, 100, 5));
        this.registerSetting(slowOnEnable = new ButtonSetting("Slow on enable", false));
        this.registerSetting(tower = new ButtonSetting("Tower", false));
        this.registerSetting(disableStrafing = new ButtonSetting("Disable strafing", false));
        this.registerSetting(offsetForwardYaw = new SliderSetting("Offset Forward Yaw", 120, 0, 360, 5));
        this.registerSetting(offsetDiagonalYaw = new SliderSetting("Offset Diagonal Yaw", 220, 0, 360, 5));
    }

    public void onDisable() {
        placeBlock = null;
        if (lastSlot.get() != -1) {
            mc.thePlayer.inventory.currentItem = lastSlot.get();
            lastSlot.set(-1);
        }
        delay = false;
        highlight.clear();
        add = 0;
        at = index = 0;
        startPos = -1;
        forceStrict = false;
        down = false;
        place = false;
        placedUp = false;
        blockSlot = -1;
        blocksPlaced = 0;
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled()) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(false);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(false);
        }
        scaffoldBlockCount.beginFade();
    }

    public void onEnable() {
        FMLCommonHandler.instance().bus().register(scaffoldBlockCount = new ScaffoldBlockCount(this.mc));
        lastSlot.set(-1);
        startPos = mc.thePlayer.posY;
        placePitch = 85;
        previousRotation = null;
        placeYaw = 2000;
        if (slowOnEnable.isToggled()) {
            Utils.setSpeed(0.0);
        }
        wasMoving = false;
        spaceReleased = true;
        initialJumpDone = false;
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }

        int rotationInput = (int) rotation.getInput();
        if (rotationInput > 0) {
            boolean usePlaceYaw = ((rotationInput == 2 && forceStrict) || rotationInput == 3 || rotationInput == 4) && placeYaw != 2000;
            float yaw = usePlaceYaw ? placeYaw : getYaw();
            float pitch = usePlaceYaw ? placePitch : 85;

            if (disableStrafing.isToggled()) {
                boolean a = mc.gameSettings.keyBindLeft.isKeyDown();
                boolean s = mc.gameSettings.keyBindBack.isKeyDown();
                boolean d = mc.gameSettings.keyBindRight.isKeyDown();

                if (a || s || d) {
                    mc.gameSettings.keyBindLeft.setKeyBindState(mc.gameSettings.keyBindLeft.getKeyCode(), false);
                    mc.gameSettings.keyBindBack.setKeyBindState(mc.gameSettings.keyBindBack.getKeyCode(), false);
                    mc.gameSettings.keyBindRight.setKeyBindState(mc.gameSettings.keyBindRight.getKeyCode(), false);
                }
            }

            if (rotationInput == 3) { // Offset rotation
                boolean w = mc.gameSettings.keyBindForward.isKeyDown();
                boolean a = mc.gameSettings.keyBindLeft.isKeyDown();
                boolean s = mc.gameSettings.keyBindBack.isKeyDown();
                boolean d = mc.gameSettings.keyBindRight.isKeyDown();
                boolean diagonal = Utils.isDiagonal(false);

                if (diagonal) {
                    yaw = Math.round(mc.thePlayer.rotationYaw + offsetDiagonalYaw.getInput());
                } else {
                    if ((w && a && s && d) || (w && !a && !s && !d) || (w && !a && s && !d)) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - offsetForwardYaw.getInput());
                    } else if (w && !a && !s && d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw + 185);
                    } else if (w && a && !s && !d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - 185);
                    } else if (!w && a && !s && !d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - 270);
                    } else if (!w && a && s && !d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - 310);
                    } else if (!w && !a && !s && d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - 90);
                    } else if (!w && !a && s && d) {
                        yaw = Math.round(mc.thePlayer.rotationYaw - 315);
                    }
                }

                if (adjustToSurroundings.isToggled()) {
                    BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
                    if (mc.theWorld.getBlockState(playerPos.east()).getBlock() != Blocks.air) {
                        yaw += 10;
                    } else if (mc.theWorld.getBlockState(playerPos.west()).getBlock() != Blocks.air) {
                        yaw -= 10;
                    } else if (mc.theWorld.getBlockState(playerPos.south()).getBlock() != Blocks.air) {
                        yaw += 5;
                    } else if (mc.theWorld.getBlockState(playerPos.north()).getBlock() != Blocks.air) {
                        yaw -= 5;
                    }
                }

                pitch = 85 + (float) (Math.random() * 1);
            }

            if (rotationInput == 4) { // Dynamic rotation
                yaw = placeYaw;
                pitch = placePitch;
            }

            if (jumpFacingForward.getInput() > 0 && mc.thePlayer.onGround && keepYPosition()) {
                if (jumpFacingForward.getInput() == 2 || (jumpFacingForward.getInput() == 1 && Utils.isDiagonal(false))) {
                    yaw = mc.thePlayer.rotationYaw;
                    pitch = (float) (0 + getRandom());
                    delay = true;
                }
            }

            event.setYaw(yaw);
            event.setPitch(pitch);
        }


        if (isAboutToFall()) {
            int fallbackRotationInput = (int) fallbackRotation.getInput();
            if (fallbackRotationInput > 0) {
                switch (fallbackRotationModes[fallbackRotationInput]) {
                    case "Closest":

                        if (placeBlock != null) {
                            float[] closestRotations = RotationUtils.getRotations(placeBlock.getBlockPos());
                            event.setYaw(closestRotations[0]);
                            event.setPitch(closestRotations[1]);
                        }
                        break;
                    case "Centered":

                        if (placeBlock != null) {
                            BlockPos blockPos = placeBlock.getBlockPos();
                            float[] centeredRotations = RotationUtils.getRotations(new BlockPos(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
                            event.setYaw(centeredRotations[0]);
                            event.setPitch(centeredRotations[1]);
                        }
                        break;
                }
            }
        }

        place = true;
    }

    @SubscribeEvent
    public void onJump(JumpEvent e) {
        delay = true;
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled() && lastSlot.get() != mc.thePlayer.inventory.currentItem && totalBlocks() > 0) {
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelUpdate(true);
            ((IMixinItemRenderer) mc.getItemRenderer()).setCancelReset(true);
        }
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (!autoSwap.isToggled() || getSlot() == -1) {
            if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
                return;
            }
        }
        if (keepYPosition() && !down) {
            startPos = Math.floor(mc.thePlayer.posY);
            down = true;
        }
        else if (!keepYPosition() || Math.floor(mc.thePlayer.posY) < startPos) {
            down = false;
            placedUp = false;
        }
        if (keepYPosition() && (fastScaffold.getInput() == 3 || fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5 || fastScaffold.getInput() == 7) && mc.thePlayer.onGround) {
            if (fastScaffold.getInput() == 7 && initialJumpDone && wasMoving && !Utils.isMoving()) {
                initialJumpDone = false;
            }
            if (fastScaffold.getInput() == 7 && initialJumpDone) {
                
            } else {
                if (autoJump.isToggled()) {
                    mc.thePlayer.jump();
                }
                initialJumpDone = true;
            }
            add = 0;
            if (Math.floor(mc.thePlayer.posY) == Math.floor(startPos) && fastScaffold.getInput() == 5) {
                placedUp = false;
            }
        }
        double original = startPos;
        if (fastScaffold.getInput() == 3) {
            if (groundDistance() >= 2 && add == 0) {
                original++;
                add++;
            }
        }
        else if (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 5) {
            double distanceToGround = Utils.distanceToGroundPos(mc.thePlayer, (int) startPos);
            double threshold = Utils.isDiagonal(false) ? 1.2 : 0.6;
            if (groundDistance() > 0 && distanceToGround > 0 && (distanceToGround < threshold) && (threshold == 0.6 ? mc.thePlayer.posY - startPos < 1.5 : true) && mc.thePlayer.fallDistance > 0 && ((!placedUp || Utils.isDiagonal(true)) || fastScaffold.getInput() == 4)) {
                if (fastScaffold.getInput() != 4) {
                    original++;
                }
            }
        }
        double motionSetting = sprint() ? fastScaffoldMotion.getInput() : motion.getInput();
        if (mc.thePlayer.onGround && Utils.isMoving() && motionSetting != 1) {
            final int speedAmplifier = Utils.getSpeedAmplifier();
            switch (speedAmplifier) {
                case 1:
                    motionSetting = motion.getInput() - 0.022;
                    break;
                case 2:
                    motionSetting = motion.getInput() - 0.04;
                    break;
            }
            Utils.setSpeed(Utils.getHorizontalSpeed() * motionSetting);
        }
        int slot = getSlot();
        if (slot == -1) {
            return;
        }
        if (blockSlot == -1) {
            blockSlot = slot;
        }
        if (lastSlot.get() == -1) {
            lastSlot.set(mc.thePlayer.inventory.currentItem);
        }
        if (autoSwap.isToggled() && blockSlot != -1) {
            mc.thePlayer.inventory.currentItem = ModuleManager.autoSwap.swapToGreaterStack.isToggled() ? slot : blockSlot;
        }
        heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            blockSlot = -1;
            return;
        }
        if (delay && (delayOnJump.isToggled() || jumpFacingForward.getInput() > 0)) {
            delay = false;
            return;
        }

        MovingObjectPosition rayCasted = null;
        int precisionInput = (int) precision.getInput();
        float searchYaw;
        switch (precisionInput) {
            case 4:
                searchYaw = 90;
                break;
            case 3:
                searchYaw = 65;
                break;
            case 1:
                searchYaw = 20;
                break;
            case 0:
                searchYaw = 6;
                break;
            case 2:
            default:
                searchYaw = 35;
                break;
        }

        double playerY = mc.thePlayer.posY;
        double targetY = keepYPosition() ? original - 1 : playerY - 1;
        BlockPos targetPos = new BlockPos(mc.thePlayer.posX, targetY, mc.thePlayer.posZ);

        PlaceData placeData = getBlockData(targetPos);
        if (placeData == null || placeData.blockPos == null || placeData.enumFacing == null) {
            return;
        }

        float[] targetRotation = RotationUtils.getRotations(placeData.blockPos);
        Vec3 bestVec = getVec3(placeData.enumFacing);
        float[] initialSearchPitch = {78, 12};
        float[] searchPitch = initialSearchPitch.clone();
        final double OFFSET_WEIGHT = 0.2D;
        double closestCombinedDistance = Double.MAX_VALUE;

        float playerYaw = getYaw();
        float previousRotationYaw = (previousRotation != null) ? previousRotation[0] : 0f;

        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                if (rayCasted == null && (Utils.overPlaceable(-1) || keepYPosition()) && precisionInput != 4) {
                    searchYaw = 180;
                    searchPitch = new float[]{65, 25};
                }
                else {
                    break;
                }
            }

            float[] yawSearchList = generateSearchSequence(searchYaw);
            float[] pitchSearchList = generateSearchSequence(searchPitch[1]);

            for (float checkYaw : yawSearchList) {
                float fixedYaw = (float) (playerYaw + checkYaw + getRandom());

                if (!Utils.overPlaceable(-1) && !keepYPosition()) {
                    continue;
                }

                for (float checkPitch : pitchSearchList) {
                    float fixedPitch = RotationUtils.clampTo90((float) (targetRotation[1] + checkPitch + getRandom()));

                    MovingObjectPosition raycast = RotationUtils.rayCast(mc.playerController.getBlockReachDistance(), fixedYaw, fixedPitch, true);
                    if (raycast == null || raycast.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                        continue;
                    }
                    if (!raycast.getBlockPos().equals(placeData.blockPos) || raycast.sideHit != placeData.enumFacing) {
                        continue;
                    }
                    if (!(heldItem.getItem() instanceof ItemBlock)) {
                        continue;
                    }
                    ItemBlock itemBlock = (ItemBlock) heldItem.getItem();
                    if (!itemBlock.canPlaceBlockOnSide(mc.theWorld, raycast.getBlockPos(), raycast.sideHit, mc.thePlayer, heldItem)) {
                        continue;
                    }

                    Vec3 hitVec = raycast.hitVec;
                    Vec3 offset = new Vec3(hitVec.xCoord - raycast.getBlockPos().getX(), hitVec.yCoord - raycast.getBlockPos().getY(), hitVec.zCoord - raycast.getBlockPos().getZ());

                    double distanceToCenter = offset.distanceTo(bestVec);
                    double distanceToPreviousRotation = Math.abs(fixedYaw - previousRotationYaw);
                    double combinedDistance = OFFSET_WEIGHT * distanceToCenter + (distanceToPreviousRotation / 360.0);

                    if (rayCasted == null || combinedDistance < closestCombinedDistance) {
                        closestCombinedDistance = combinedDistance;
                        rayCasted = raycast;
                        placeYaw = fixedYaw;
                        placePitch = fixedPitch;
                        forceStrict = forceStrict(checkYaw) && i == 1;
                    }
                }
            }

            if (rayCasted != null) {
                break;
            }
        }
        if (rayCasted != null && (place || rotation.getInput() == 0)) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            placeBlock = rayCasted;
            place(placeBlock, false);
            int input = (int) multiPlace.getInput();
            for (int i = 0; i < input; i++) {
                place(placeBlock, true);
            }
            place = false;
            if (placeBlock.sideHit == EnumFacing.UP && keepYPosition()) {
                placedUp = true;
            }
            previousBlock = placeData.blockPos.offset(placeData.getEnumFacing());
        }
        boolean isMoving = Utils.isMoving();
        boolean isSpaceDown = mc.gameSettings.keyBindJump.isKeyDown();
    
        if (!isSpaceDown) {
            spaceReleased = true;
        } else {
            spaceReleased = false;
        }
    
        wasMoving = isMoving;
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
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
        return fastScaffoldModes[(int) fastScaffold.getInput()];
    }

    public float[] generateSearchSequence(float value) {
        int length = (int) value * 2;
        float[] sequence = new float[length + 1];

        int index = 0;
        sequence[index++] = 0;

        for (int i = 1; i <= value; i++) {
            sequence[index++] = i;
            sequence[index++] = -i;
        }

        return sequence;
    }

    public PlaceData getBlockData(BlockPos pos) {
        if (previousBlock != null && previousBlock.getY() > mc.thePlayer.posY) {
            previousBlock = null;
        }
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (int i = 0; i < offsets.length; i++) {
                BlockPos newPos = pos.add(offsets[i]);
                Block block = BlockUtils.getBlock(newPos);
                if (newPos.equals(previousBlock)) {
                    return new PlaceData(facings[i], newPos);
                }
                if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block)) {
                    return new PlaceData(facings[i], newPos);
                }
            }
        }
        BlockPos[] additionalOffsets = {
                pos.add(-1, 0, 0),
                pos.add(1, 0, 0),
                pos.add(0, 0, 1),
                pos.add(0, 0, -1),
                pos.add(0, -1, 0),
        };
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (BlockPos additionalPos : additionalOffsets) {
                for (int i = 0; i < offsets.length; i++) {
                    BlockPos newPos = additionalPos.add(offsets[i]);
                    Block block = BlockUtils.getBlock(newPos);
                    if (newPos.equals(previousBlock)) {
                        return new PlaceData(facings[i], newPos);
                    }
                    if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block)) {
                        return new PlaceData(facings[i], newPos);
                    }
                }
            }
        }
        BlockPos[] additionalOffsets2 = {
                new BlockPos(-1, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(0, 0, -1),
                new BlockPos(0, -1, 0),
        };
        for (int lastCheck = 0; lastCheck < 2; lastCheck++) {
            for (BlockPos additionalPos2 : additionalOffsets2) {
                for (BlockPos additionalPos : additionalOffsets) {
                    for (int i = 0; i < offsets.length; i++) {
                        BlockPos newPos = additionalPos2.add(additionalPos.add(offsets[i]));
                        Block block = BlockUtils.getBlock(newPos);
                        if (newPos.equals(previousBlock)) {
                            return new PlaceData(facings[i], newPos);
                        }
                        if (!block.getMaterial().isReplaceable() && !BlockUtils.isInteractable(block)) {
                            return new PlaceData(facings[i], newPos);
                        }
                    }
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onMouse(MouseEvent mouseEvent) {
        if (mouseEvent.button == 1) {
            rmbDown = mouseEvent.buttonstate;
            if (placeBlock != null && rmbDown) {
                mouseEvent.setCanceled(true);
            }
        }
    }

    public boolean stopFastPlace() {
        return this.isEnabled() && placeBlock != null;
    }

    public double groundDistance() {
        for (int i = 1; i <= 20; i++) {
            if (!mc.thePlayer.onGround && !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - (i / 10), mc.thePlayer.posZ)) instanceof BlockAir)) {
                return (i / 10);
            }
        }
        return -1;
    }

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        if (!Utils.nullCheck() || !highlightBlocks.isToggled() || highlight.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Timer>> iterator = highlight.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Timer> entry = iterator.next();
            if (entry.getValue() == null) {
                entry.setValue(new Timer(750));
                entry.getValue().start();
            }
            int alpha = entry.getValue() == null ? 210 : 210 - entry.getValue().getValueInt(0, 210, 1);
            if (alpha == 0) {
                iterator.remove();
                continue;
            }
            RenderUtils.renderBlock(entry.getKey(), Utils.mergeAlpha(Theme.getGradient((int) HUD.theme.getInput(), 0), alpha), true, false);
        }
    }

    public boolean blockAbove() {
        return !(BlockUtils.getBlock(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY + 2, mc.thePlayer.posZ)) instanceof BlockAir);
    }

    public boolean sprint() {
        if (this.isEnabled() && fastScaffold.getInput() > 0 && placeBlock != null && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1))) {
            switch ((int) fastScaffold.getInput()) {
                case 1:
                    return Utils.onEdge();
                case 2:
                    return true;
                case 3:
                case 4:
                case 5:
                case 6:
                    return keepYPosition();
                case 7:
                    return mc.thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown();
            }
        }
        return false;
    }

    private boolean forceStrict(float value) {
        return (Utils.inBetween(-170, -105, value) || Utils.inBetween(-80, 80, value) || Utils.inBetween(98, 170, value)) && !Utils.inBetween(-10, 10, value);
    }

    private boolean keepYPosition() {
        return this.isEnabled() && Utils.keysDown() && (fastScaffold.getInput() == 4 || fastScaffold.getInput() == 3 || fastScaffold.getInput() == 5 || fastScaffold.getInput() == 6 || fastScaffold.getInput() == 7) && (!Utils.jumpDown() || fastScaffold.getInput() == 6) && (!fastOnRMB.isToggled() || Mouse.isButtonDown(1)) && (!blockAbove() || fastScaffold.getInput() == 6);
    }

    public boolean safewalk() {
        return this.isEnabled() && safeWalk.isToggled() && (!keepYPosition() || fastScaffold.getInput() == 3 || totalBlocks() == 0) ;
    }

    public boolean stopRotation() {
        return this.isEnabled() && (rotation.getInput() <= 1 || (rotation.getInput() == 2 && placeBlock != null));
    }

    public float getYaw() {
        float yaw = 0.0f;
        double moveForward = mc.thePlayer.movementInput.moveForward;
        double moveStrafe = mc.thePlayer.movementInput.moveStrafe;
        if (moveForward == 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 90.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -90.0f;
            }
        }
        else if (moveForward > 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 180.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 135.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -135.0f;
            }
        }
        else if (moveForward < 0.0) {
            if (moveStrafe == 0.0) {
                yaw = 0.0f;
            }
            else if (moveStrafe > 0.0) {
                yaw = 45.0f;
            }
            else if (moveStrafe < 0.0) {
                yaw = -45.0f;
            }
        }
        return MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) + yaw;
    }

    private void place(MovingObjectPosition block, boolean extra) {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof ItemBlock) || !Utils.canBePlaced((ItemBlock) heldItem.getItem())) {
            return;
        }
        if (!extra && mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, heldItem, block.getBlockPos(), block.sideHit, block.hitVec)) {
            if (silentSwing.getInput() > 0 && Math.random() * 100 < silentSwing.getInput()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
                if (!(autoSwap.isToggled() && ModuleManager.autoSwap.spoofItem.isToggled())) {
                    mc.getItemRenderer().resetEquippedProgress();
                }
            }
            highlight.put(block.getBlockPos().offset(block.sideHit), null);
            previousRotation = new float[]{placeYaw, placePitch};
            if (heldItem.stackSize == 0) {
                blockSlot = -1;
            }
        }
        else if (extra) {
            float f = (float)(block.hitVec.xCoord - (double)block.getBlockPos().getX());
            float f1 = (float)(block.hitVec.yCoord - (double)block.getBlockPos().getY());
            float f2 = (float)(block.hitVec.zCoord - (double)block.getBlockPos().getZ());
            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(block.getBlockPos(), block.sideHit.getIndex(), heldItem, f, f1, f2));
            if (silentSwing.getInput() > 0 && Math.random() * 100 < silentSwing.getInput()) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
                mc.getItemRenderer().resetEquippedProgress();
            }
        }
        blocksPlaced++;
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

    private Vec3 getVec3(EnumFacing facing) {
        double x = 0.5;
        double y = 0.5;
        double z = 0.5;
        if (facing != EnumFacing.UP && facing != EnumFacing.DOWN) {
            y += 0.5;
        }
        else {
            x += 0.3;
            z += 0.3;
        }
        if (facing == EnumFacing.WEST || facing == EnumFacing.EAST) {
            z += 0.15;
        }
        if (facing == EnumFacing.SOUTH || facing == EnumFacing.NORTH) {
            x += 0.15;
        }
        return new Vec3(x, y, z);
    }

    private double getRandom() {
        return Utils.randomizeInt(-40, 40) / 100.0;
    }

    private boolean isAboutToFall() {
        return mc.thePlayer.fallDistance > 2.0f && !mc.thePlayer.onGround;
    }

    static class PlaceData {
        EnumFacing enumFacing;
        BlockPos blockPos;

        PlaceData(EnumFacing enumFacing, BlockPos blockPos) {
            this.enumFacing = enumFacing;
            this.blockPos = blockPos;
        }

        EnumFacing getEnumFacing() {
            return enumFacing;
        }
    }
}