package keystrokesmod.module.impl.combat;

import keystrokesmod.Raven;
import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.PreUpdateEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.world.AntiBot;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.PacketUtils;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.play.client.*;
import net.minecraft.util.*;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.minecraft.util.EnumFacing.DOWN;

public class KillAura extends Module {
    private SliderSetting minCPS;
    private SliderSetting maxCPS;
    public SliderSetting autoBlockMode;
    private SliderSetting fov;
    private SliderSetting attackRange;
    private SliderSetting swingRange;
    private SliderSetting blockRange;
    private SliderSetting rotationMode;
    private SliderSetting rotationSpeed;
    private SliderSetting sortMode;
    private SliderSetting switchDelay;
    private SliderSetting targets;
    private ButtonSetting attackMobs;
    private ButtonSetting targetInvis;
    private ButtonSetting disableInInventory;
    private ButtonSetting disableWhileBlocking;
    private ButtonSetting disableWhileMining;
    private ButtonSetting hitThroughBlocks;
    private ButtonSetting ignoreTeammates;
    public ButtonSetting manualBlock;
    private ButtonSetting prioritizeEnemies;
    private ButtonSetting requireMouseDown;
    private ButtonSetting silentSwing;
    private ButtonSetting weaponOnly;
    float lastPitch, lastYaw;

    private String[] autoBlockModes = new String[] { "Manual", "Vanilla", "Fake", "Partial", "Interact A", "Interact B", "Buffer A", "Buffer B", "Buffer C"};
    private String[] rotationModes = new String[] { "Silent", "Lock view", "None" };
    private String[] sortModes = new String[] { "Distance", "Health", "Hurttime", "Yaw" };

    // autoblock related
    private String[] swapBlacklist = { "compass", "snowball", "spawn", "skull" };

    // target variables
    public static EntityLivingBase target;
    public static EntityLivingBase attackingEntity;
    private HashMap<Integer, Integer> hitMap = new HashMap<>(); // entity id, ticks existed client\
    private List<Entity> hostileMobs = new ArrayList<>();

    // blocking related
    public boolean blockingClient;
    public boolean blockingServer;
    private int interactTicks;
    private boolean firstCycle;
    private boolean partialDown;
    private int partialTicks;
    private boolean blocked;

    // blink related
    private ConcurrentLinkedQueue<Packet> blinkedPackets = new ConcurrentLinkedQueue<>();
    private AtomicBoolean blinking = new AtomicBoolean(false);
    public boolean lag;
    private boolean swapped;

    // other
    private long lastTime = 0L;
    private long delay;
    private boolean shouldAttack;
    private int previousAutoBlockMode;
    private int fistTick;
    private boolean reset;
    private boolean rotated;
    private boolean sendUnBlock;
    private int delayTicks = 0;
    private boolean lastPressedLeft;
    private boolean lastPressedRight;

    public KillAura() {
        super("KillAura", category.combat);
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 9.0, 1.0, 25.0, 0.5));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 12.0, 1.0, 25.0, 0.5));
        this.registerSetting(autoBlockMode = new SliderSetting("Autoblock", 0, autoBlockModes));
        this.registerSetting(fov = new SliderSetting("FOV", 360.0, 30.0, 360.0, 4.0));
        this.registerSetting(attackRange = new SliderSetting("Range (attack)", 3.0, 3.0, 6.0, 0.05));
        this.registerSetting(swingRange = new SliderSetting("Range (swing)", 3.3, 3.0, 8.0, 0.05));
        this.registerSetting(blockRange = new SliderSetting("Range (block)", 6.0, 3.0, 12.0, 0.05));
        this.registerSetting(rotationMode = new SliderSetting("Rotation mode", 0, rotationModes));
        this.registerSetting(rotationSpeed = new SliderSetting("Rotation speed", 5.0, 1.0, 5.0, 0.1));
        this.registerSetting(sortMode = new SliderSetting("Sort mode", 0, sortModes));
        this.registerSetting(switchDelay = new SliderSetting("Switch delay", "ms", 200.0, 50.0, 1000.0, 25.0));
        this.registerSetting(targets = new SliderSetting("Targets", 3.0, 1.0, 10.0, 1.0));
        this.registerSetting(targetInvis = new ButtonSetting("Target invis", true));
        this.registerSetting(attackMobs = new ButtonSetting("Attack mobbs", false));
        this.registerSetting(disableInInventory = new ButtonSetting("Disable in inventory", true));
        this.registerSetting(disableWhileBlocking = new ButtonSetting("Disable while blocking", false));
        this.registerSetting(disableWhileMining = new ButtonSetting("Disable while mining", false));
        this.registerSetting(hitThroughBlocks = new ButtonSetting("Hit through blocks", true));
        this.registerSetting(ignoreTeammates = new ButtonSetting("Ignore teammates", true));
        this.registerSetting(manualBlock = new ButtonSetting("Manual block", false)); // does absolutely nothing
        this.registerSetting(prioritizeEnemies = new ButtonSetting("Prioritize enemies", false));
        this.registerSetting(requireMouseDown = new ButtonSetting("Require mouse down", false));
        this.registerSetting(silentSwing = new ButtonSetting("Silent swing while blocking", false));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
    }

    @Override
    public String getInfo() {
        if (rotationMode.getInput() == 2) { // None, return FOV if rotation mode is none
            return String.valueOf((int) this.fov.getInput());
        }
        return rotationModes[(int) rotationMode.getInput()];
    }

    @Override
    public void onEnable() {
        lastYaw = mc.thePlayer.rotationYaw;
        lastPitch = mc.thePlayer.rotationPitch;
        if (rotationMode.getInput() == 0 && autoBlockMode.getInput() <= 1) {
            delayTicks = 1;
        }
    }

    @Override
    public void onDisable() {
        handleBlocking(false);
        hitMap.clear();
        if (blinkAutoBlock()) { // interact autoblock
            resetBlinkState(true);
        }
        blinking.set(false);
        interactTicks = 0;
        setTarget(null);
        if (rotated || reset) {
            resetYaw();
        }
        rotated = false;
        swapped = false;
        partialTicks = 0;
        delayTicks = 0;
    }


    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            boolean pressedLeft = Mouse.isButtonDown(0);
            if (pressedLeft && !lastPressedLeft) {
                onCustomMouse(0, true);
            }
            if (this.maxCPS.getInput() < this.minCPS.getInput()) {
                minCPS.setValue(maxCPS.getInput() - 1);
            }
            if (!pressedLeft && lastPressedLeft) {
                onCustomMouse(0, false);
            }
            if (target == null) {
                boolean pressedRight = Mouse.isButtonDown(1);
                if (pressedRight && !lastPressedRight) {
                    onCustomMouse(1, true);
                }
                if (!pressedRight && lastPressedRight) {
                    onCustomMouse(1, false);
                }
                lastPressedRight = pressedRight;
            }
            lastPressedLeft = pressedLeft;
        }
        if (sendUnBlock) {
            if (Raven.packetsHandler.C07.get()) {
                sendUnBlock = false;
                return;
            }
            Reflection.setItemInUse(blockingClient = false);
            sendDigPacket();
            sendUnBlock = false;
            return;
        }
        delayTicks--;
        if (delayTicks >= 0) {
            return;
        }
        if (reset) {
            resetYaw();
            reset = false;
        }
        if (!basicCondition() || !settingCondition()) {
            setTarget(null);
            return;
        }
        if (target == null) {
            handleBlocking(false);
            return;
        }
        double distanceToBB = getDistanceToBoundingBox(target);
        boolean inBlockRange = distanceToBB <= blockRange.getInput();
        if (!autoBlockOverride() || !inBlockRange) { // regular swing & attack if autoblock isnt overriding or isnt in autoblock range
            handleSwingAndAttack(distanceToBB, false);
        }
        else if (inBlockRange && autoBlockOverride() && manualBlock()) {
            handleAutoBlock(distanceToBB);
        }
        else if ((blinkAutoBlock() && !Utils.holdingSword()) || !inBlockRange) { // for blink autoblocks
            if (blinking.get() || lag) {
                resetBlinkState(true);
                blinking.set(false);
            }
            interactTicks = 0;
        }
        if (inBlockRange && manualBlock()) {
            handleBlocking(true);
        }
        if (mc.currentScreen == null || mc.currentScreen.allowUserInput) {
            boolean pressedRight = Mouse.isButtonDown(1);
            if (pressedRight && !lastPressedRight) {
                onCustomMouse(1, true);
            }
            if (!pressedRight && lastPressedRight) {
                onCustomMouse(1, false);
            }
            lastPressedRight = pressedRight;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onPreMotion(PreMotionEvent e) {
        if (delayTicks >= 0) {
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (!basicCondition() || !settingCondition()) {
            setTarget(null);
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        handleTarget();
        if (target == null) {
            if (rotated) {
                resetYaw(e);
            }
            return;
        }
        if (rotationMode.getInput() != 2) {
            if (inRange(target, attackRange.getInput() - 0.005)) {
                float[] rotations = RotationUtils.getRotations(target, e.getYaw(), e.getPitch());
                float[] smoothedRotations = calculateSmoothRotations(rotations);
                if (rotationMode.getInput() == 0) { // silent
                    e.setYaw(smoothedRotations[0]);
                    e.setPitch(smoothedRotations[1]);
                    rotated = true;
                }
                else {
                    mc.thePlayer.rotationYaw = smoothedRotations[0];
                    mc.thePlayer.rotationPitch = smoothedRotations[1];
                }
            }
            else if (rotationMode.getInput() == 0) {
                if (rotated) {
                    reset = true;
                    e.setYaw(RotationUtils.serverRotations[0]);
                    e.setPitch(RotationUtils.serverRotations[1]);
                    fistTick = mc.thePlayer.ticksExisted + 1;
                    rotated = false;
                }
            }
        }
    }

    public void onUpdate() {
        if (rotationMode.getInput() == 1 && target != null) {
            if (inRange(target, attackRange.getInput() - 0.005)) {
                float[] rotations = RotationUtils.getRotations(target, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
                float[] smoothedRotations = calculateSmoothRotations(rotations);
                mc.thePlayer.rotationYaw = smoothedRotations[0];
                mc.thePlayer.rotationPitch = smoothedRotations[1];
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (event.phase == TickEvent.Phase.START) {
            if (System.currentTimeMillis() - this.lastTime >= delay && target != null) {
                this.lastTime = System.currentTimeMillis();
                updateAttackDelay();
                if (target != null) {
                    shouldAttack = true;
                }
            }
        }
        else if (event.phase == TickEvent.Phase.END && mc.thePlayer.ticksExisted == fistTick && rotationMode.getInput() == 0) {
            mc.thePlayer.renderArmPitch = mc.thePlayer.rotationPitch;
            mc.thePlayer.renderArmYaw = mc.thePlayer.rotationYaw;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onSendPacket(SendPacketEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        Packet packet = e.getPacket();
        if (packet instanceof C08PacketPlayerBlockPlacement) {
            if (delayTicks >= 0) {
                if (((C08PacketPlayerBlockPlacement) packet).getStack() != null && ((C08PacketPlayerBlockPlacement) packet).getStack().getItem() instanceof ItemSword && ((C08PacketPlayerBlockPlacement) packet).getPlacedBlockDirection() != 255) {
                    e.setCanceled(true);
                }
            }
        }
        if (blinking.get() && !e.isCanceled()) { // blink
            if (packet.getClass().getSimpleName().startsWith("S")) {
                return;
            }
            if (packet instanceof C00PacketKeepAlive || packet instanceof C00PacketLoginStart || packet instanceof C00Handshake) {
                return;
            }
            blinkedPackets.add(packet);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onSetAttackTarget(LivingSetAttackTargetEvent e) {
        if (e.entity != null && !hostileMobs.contains(e.entity)) {
            if (!(e.target instanceof EntityPlayer) || !e.target.getName().equals(mc.thePlayer.getName())) {
                return;
            }
            hostileMobs.add(e.entity);
        }
        if (e.target == null && hostileMobs.contains(e.entity)) {
            hostileMobs.remove(e.entity);
            if (Raven.debug) {
                Utils.sendModuleMessage(this, "&7mob stopped attack player");
            }
        }
    }

    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        if (e.button == 0 || e.button == 1) {
            if (!Utils.holdingWeapon() || target == null || rotationMode.getInput() != 0) {
                return;
            }
            e.setCanceled(true);
        }
    }

    public void onCustomMouse(int button, boolean state) {
        if (blinkAutoBlock() || autoBlockMode.getInput() == 3 || rotationMode.getInput() != 0) {
            return;
        }
        if (button == 1) {
            if (state) {
                if (target != null) {
                    if (basicCondition() && settingCondition()) {
                        if (!ModuleManager.bedAura.rotate) {
                            if (isLookingAtEntity()) {
                                if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                                    interactAt(true, true, false, true);
                                }
                            }
                        }
                    }
                    Reflection.setItemInUse(blockingClient = true);
                    sendBlockPacket();
                    // cancel
                }
                else {
                    delayTicks = 1;
                }
            }
            else {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
                Reflection.setItemInUse(blockingClient = false);
                sendUnBlock = true;
            }
        }
        else if (button == 0) {
            if (!state) {
                delayTicks = 1;
            }
            if (state && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && !Mouse.isButtonDown(1)) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
                KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
            }
            else if (!state) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            }
        }
    }

    @SubscribeEvent
    public void onWorldJoin(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            hitMap.clear();
            hostileMobs.clear();
        }
    }

    private void setTarget(Entity entity) {
        if (entity == null || !(entity instanceof EntityLivingBase)) {
            if (blockingClient) {
                Reflection.setItemInUse(blockingClient = false);
                sendUnBlock = true;
            }
            if (blinkAutoBlock()) { // interact autoblock
                resetBlinkState(true);
                blinking.set(false);
            }
            swapped = false;
            partialTicks = 0;
            interactTicks = 0;
            handleBlocking(false);
            target = null;
            attackingEntity = null;
        }
        else {
            target = (EntityLivingBase) entity;
        }
    }

    private void handleTarget() {
        // Narrow down the targets available
        List<EntityLivingBase> availableTargets = new ArrayList<>();
        double maxRange = getMaxRange();
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity == null || entity == mc.thePlayer || entity.isDead) {
                continue;
            }
            if (entity instanceof EntityPlayer) {
                if (Utils.isFriended((EntityPlayer) entity)) {
                    continue;
                }
                if (((EntityPlayer) entity).deathTime != 0) {
                    continue;
                }
                if (AntiBot.isBot(entity) || (Utils.isTeamMate(entity) && ignoreTeammates.isToggled())) {
                    continue;
                }
            }
            else if (entity instanceof EntityCreature && attackMobs.isToggled()) {
                if (((EntityCreature) entity).tasks == null || ((EntityCreature) entity).isAIDisabled() || ((EntityCreature) entity).deathTime != 0) { // no ai
                    continue;
                }
                if (!entity.getClass().getCanonicalName().startsWith("net.minecraft.entity.monster.")) {
                    continue;
                }
            }
            else {
                continue;
            }
            if (entity.isInvisible() && !targetInvis.isToggled()) {
                continue;
            }
            float fovInput = (float) fov.getInput();
            if (fovInput != 360.0f && !Utils.inFov(fovInput, entity)) {
                continue;
            }
            if (mc.thePlayer.getDistanceToEntity(entity) < maxRange + maxRange / 3) { // simple distance check
                availableTargets.add((EntityLivingBase) entity);
            }
        }
        // Init as a new class and adding to list
        List<KillAuraTarget> toClassTargets = new ArrayList<>();
        for (EntityLivingBase target : availableTargets) {
            double distanceRayCasted = getDistanceToBoundingBox(target);
            if (distanceRayCasted > maxRange) {
                continue;
            }
            if (!(target instanceof EntityPlayer) && attackMobs.isToggled() && !isHostile((EntityCreature) target)) {
                continue;
            }
            if (!hitThroughBlocks.isToggled() && (!Utils.canPlayerBeSeen(target) || !inRange(target, attackRange.getInput() - 0.005))) {
                continue;
            }
            toClassTargets.add(new KillAuraTarget(distanceRayCasted, target.getHealth(), target.hurtTime, RotationUtils.distanceFromYaw(target, false), target.getEntityId(), (target instanceof EntityPlayer) ? Utils.isEnemy((EntityPlayer) target) : false));
        }
        // Sorting targets
        Comparator<KillAuraTarget> comparator = null;
        switch ((int) sortMode.getInput()) {
            case 0:
                comparator = Comparator.comparingDouble(entity -> entity.distance);
                break;
            case 1:
                comparator = Comparator.comparingDouble(entityPlayer -> (double)entityPlayer.health);
                break;
            case 2:
                comparator = Comparator.comparingDouble(entityPlayer2 -> (double)entityPlayer2.hurttime);
                break;
            case 3:
                comparator = Comparator.comparingDouble(entity2 -> entity2.yawDelta);
                break;
        }
        if (prioritizeEnemies.isToggled()) {
            List<KillAuraTarget> enemies = new ArrayList<>();
            for (KillAuraTarget entity : toClassTargets) {
                if (entity.isEnemy) {
                    enemies.add(entity);
                }
            }
            if (!enemies.isEmpty()) {
                toClassTargets = new ArrayList<>(enemies);
            }
        }
        if (sortMode.getInput() != 0) {
            Collections.sort(toClassTargets, Comparator.comparingDouble(entity -> entity.distance));
        }
        Collections.sort(toClassTargets, comparator); // then sort by selected sorting mode

        List<KillAuraTarget> attackTargets = new ArrayList<>();
        for (KillAuraTarget killAuraTarget : toClassTargets) {
            if (killAuraTarget.distance <= attackRange.getInput() - 0.005) {
                attackTargets.add(killAuraTarget);
            }
        }

        if (!attackTargets.isEmpty()) {
            // Switch aura
            int ticksExisted = mc.thePlayer.ticksExisted;
            int switchDelayTicks = (int) (switchDelay.getInput() / 50);
            long noHitTicks = (long) Math.min(attackTargets.size(), targets.getInput()) * switchDelayTicks;
            for (KillAuraTarget auraTarget : attackTargets) {
                Integer firstHit = hitMap.get(auraTarget.entityId);
                if (firstHit == null || ticksExisted - firstHit >= switchDelayTicks) {
                    continue;
                }
                if (auraTarget.distance < attackRange.getInput() - 0.005) {
                    setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
                    return;
                }
            }

            for (KillAuraTarget auraTarget : attackTargets) {
                Integer firstHit = hitMap.get(auraTarget.entityId);
                if (firstHit == null || ticksExisted >= firstHit + noHitTicks) {
                    hitMap.put(auraTarget.entityId, ticksExisted);
                    setTarget(mc.theWorld.getEntityByID(auraTarget.entityId));
                    return;
                }
            }
        }
        else if (!toClassTargets.isEmpty()) {
            KillAuraTarget killAuraTarget = toClassTargets.get(0);
            setTarget(mc.theWorld.getEntityByID(killAuraTarget.entityId));
        }
        else {
            setTarget(null);
        }
    }

    private void handleSwingAndAttack(double distance, boolean swung) {
        boolean inAttackDistance = inRange(target, attackRange.getInput() - 0.005);
        if ((distance <= swingRange.getInput() || inAttackDistance) && shouldAttack && !swung) { // swing if in swing range or needs to attack
            if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                swingItem();
            }
        }
        if (inAttackDistance) {
            attackingEntity = target;
            if (shouldAttack) {
                shouldAttack = false;
                if (ModuleManager.bedAura.rotate) {
                    return;
                }
                if (!isLookingAtEntity()) {
                    return;
                }
                if (!mc.thePlayer.isBlocking() || !disableWhileBlocking.isToggled()) {
                    mc.playerController.attackEntity(mc.thePlayer, target);
                }
            }
        }
        else {
            attackingEntity = null;
        }
    }

    private void handleBlocking(boolean blockState) {
        if (!Utils.holdingSword()) {
            if (blockingClient) {
                Reflection.setItemInUse(blockingClient = false);
            }
            return;
        }
        if (this.blockingClient == blockState && autoBlockMode.getInput() != 3) {
            return;
        }
        if (autoBlockMode.getInput() != previousAutoBlockMode) {
            if (previousAutoBlockMode == 4 || previousAutoBlockMode == 5 || previousAutoBlockMode == 6) { // if == interact
                resetBlinkState(true);
                blinking.set(false);
            }
        }
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        switch ((int) autoBlockMode.getInput()) {
            case 0: // manual, do nothing
                break;
            case 1: // vanilla
                setKeyBindState(keyCode, blockState, false);
                this.blockingClient = blockState;
                break;
            case 2: // fake
            case 4: // interact a
            case 5: // interact b
            case 6: // interact c
            case 7: // interact d
            case 8:
                Reflection.setItemInUse(this.blockingClient = blockState);
                break;
            case 3: // partial
                if (!blockState) {
                    rightClick(partialDown = false);
                    break;
                }
                partialTicks++;
                if (partialDown) {
                    rightClick(partialDown = false);
                    partialTicks = 0;
                    break;
                }
                else if (partialTicks == 2) {
                    rightClick(partialDown = true);
                }
                break;
        }
        previousAutoBlockMode = (int) autoBlockMode.getInput();
    }

    private void rightClick(boolean state) {
        int keyCode = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(keyCode, state);
        if (state) {
            KeyBinding.onTick(keyCode);
        }
        Reflection.setButton(1, state);
    }

    private double getMaxRange() {
        return Math.max(Math.max(swingRange.getInput(), attackRange.getInput() - 0.005), blockRange.getInput());
    }

    public boolean autoBlockOverride() {
        return blinkAutoBlock() && Utils.holdingSword();
    }

    public boolean blinkAutoBlock() {
        return (autoBlockMode.getInput() == 4 || autoBlockMode.getInput() == 5 || autoBlockMode.getInput() == 6 || autoBlockMode.getInput() == 7 || autoBlockMode.getInput() == 8);
    }

    private float unwrapYaw(float yaw, float prevYaw) {
        return prevYaw + ((((yaw - prevYaw + 180f) % 360f) + 360f) % 360f - 180f);
    }

    private boolean isLookingAtEntity() { //
        if (rotationMode.getInput() == 0) { // silent
            return RotationUtils.isPossibleToHit(attackingEntity, attackRange.getInput() - 0.005, RotationUtils.serverRotations);
        }
        return true;
    }


    private void handleAutoBlock(double distance) {
        boolean inAttackDistance = inRange(target, attackRange.getInput() - 0.005);
        if (inAttackDistance) {
            attackingEntity = target;
        }
        boolean swung = false;
        if ((distance <= swingRange.getInput() || inAttackDistance) && shouldAttack) { // swing if in swing range or needs to attack
            swingItem();
            swung = true;
            if (!inAttackDistance) {
                shouldAttack = false;
            }
        }
        if (ModuleManager.bedAura.stopAutoblock) {
            resetBlinkState(false);
            blinking.set(false);
            blockingServer = false;
            return;
        }
        switch ((int) autoBlockMode.getInput()) {
            case 4: // interact a
                if (interactTicks >= 3) {
                    interactTicks = 0;
                }
                interactTicks++;
                if (firstCycle) {
                    switch (interactTicks) {
                        case 1:
                            blinking.set(true);
                            int bestSwapSlot = getBestSwapSlot();
                            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
                            Raven.packetsHandler.playerSlot.set(bestSwapSlot);
                            swapped = true;
                            lag = false;
                            break;
                        case 2:
                            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                            Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
                            swapped = false;
                            handleInteractAndAttack(distance, true, true, swung);
                            break;
                        case 3:
                            sendBlockPacket();
                            releasePackets(); // release
                            firstCycle = false;
                            lag = true;
                            break;
                    }
                } else {
                    switch (interactTicks) {
                        case 1:
                            break;
                        case 2:
                            lag = false;
                            int bestSwapSlot = getBestSwapSlot();
                            blinking.set(true);
                            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
                            Raven.packetsHandler.playerSlot.set(bestSwapSlot);
                            swapped = true;
                            break;
                        case 3:
                            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                            Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
                            swapped = false;
                            handleInteractAndAttack(distance, true, true, swung);
                            sendBlockPacket();
                            releasePackets(); // release
                            //firstCycle = true;
                            lag = true;
                            break;
                    }
                }
                break;
            case 5: // interact b
                if (interactTicks >= 3) {
                    interactTicks = 0;
                }
                interactTicks++;
                switch (interactTicks) {
                    case 1:
                        break;
                    case 2:
                        lag = false;
                        int bestSwapSlot = getBestSwapSlot();
                        blinking.set(true);
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
                        Raven.packetsHandler.playerSlot.set(bestSwapSlot);
                        swapped = true;
                        break;
                    case 3:
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
                        swapped = false;
                        handleInteractAndAttack(distance, true, true, swung);
                        sendBlockPacket();
                        releasePackets(); // release
                        firstCycle = true;
                        lag = true;
                        break;
                }
                break;
            case 6: // interact c
                if (interactTicks >= 3) {
                    interactTicks = 0;
                }
                interactTicks++;
                switch (interactTicks) {
                    case 2:
                        blinking.set(true);
                        int bestSwapSlot = getBestSwapSlot();
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(bestSwapSlot));
                        Raven.packetsHandler.playerSlot.set(bestSwapSlot);
                        swapped = true;
                        lag = false;
                        break;
                    case 3:
                        mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
                        Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
                        swapped = false;
                        handleInteractAndAttack(distance, true, true, swung);
                        sendBlockPacket();
                        releasePackets(); // release
                        //firstCycle = false;
                        lag = true;
                        break;
                }
                break;
            case 7: // interact d
                if (interactTicks >= 3) {
                    interactTicks = 0;
                }
                interactTicks++;
                switch (interactTicks) {
                    case 1:
                        blinking.set(true);
                        sendDigPacket();
                        lag = false;
                        break;
                    case 3:
                        handleInteractAndAttack(distance, true, true, swung);
                        sendBlockPacket();
                        releasePackets(); // release
                        lag = true;
                        break;
                }
                break;
            case 8: // Buffer C
                if (interactTicks >= 3) {
                    interactTicks = 0;
                }
                interactTicks++;
                switch (interactTicks) {
                    case 1:
                        blinking.set(true);
                        if (blocked) {
                            mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
                            blocked = false;
                        }
                        break;
                    case 2:
                        handleInteractAndAttack(distance,true,true,swung);
                        sendBlockPacket();
                        blocked = true;
                        releasePackets();
                        break;
                }
                break;


        }
    }

    private void resetYaw(PreMotionEvent e) {
        reset = true;
        e.setYaw(RotationUtils.serverRotations[0]);
        e.setPitch(RotationUtils.serverRotations[1]);
        fistTick = mc.thePlayer.ticksExisted + 1;
        rotated = false;
    }

    private boolean basicCondition() {
        if (!Utils.nullCheck()) {
            return false;
        }
        if (mc.thePlayer.isDead) {
            return false;
        }
        return true;
    }

    private boolean settingCondition() {
        if (requireMouseDown.isToggled() && !Mouse.isButtonDown(0)) {
            return false;
        }
        else if (weaponOnly.isToggled() && !Utils.holdingWeapon()) {
            return false;
        }
        else if (disableWhileMining.isToggled() && isMining()) {
            return false;
        }
        else if (disableInInventory.isToggled() && Settings.inInventory()) {
            return false;
        }
        else if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled() && !ModuleManager.bedAura.allowAura.isToggled() && ModuleManager.bedAura.currentBlock != null) {
            return false;
        }
        return true;
    }

    private boolean isMining() {
        return Mouse.isButtonDown(0) && mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null;
    }

    private void sendBlockPacket() {
        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
    }

    private void setKeyBindState(int keycode, boolean state, boolean invokeTick) {
        KeyBinding.setKeyBindState(keycode, state);
        if (invokeTick) {
            KeyBinding.onTick(keycode);
        }
    }

    private void updateAttackDelay() {
        delay = (long)(1000.0 / Utils.randomizeDouble(minCPS.getInput(), maxCPS.getInput()) + Utils.randomizeInt(-4, 4));
    }

    private void swingItem() {
        if (silentSwing.isToggled() && mc.thePlayer.isBlocking()) {
            mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
        }
        else {
            mc.thePlayer.swingItem();
        }
    }

    private double getDistanceToBoundingBox(Entity target) {
        if (mc.thePlayer == null) {
            return 0;
        }
        Vec3 playerEyePos = mc.thePlayer.getPositionEyes(Utils.getTimer().renderPartialTicks);
        AxisAlignedBB boundingBox = target.getEntityBoundingBox();
        double nearestX = MathHelper.clamp_double(playerEyePos.xCoord, boundingBox.minX, boundingBox.maxX);
        double nearestY = MathHelper.clamp_double(playerEyePos.yCoord, boundingBox.minY, boundingBox.maxY);
        double nearestZ = MathHelper.clamp_double(playerEyePos.zCoord, boundingBox.minZ, boundingBox.maxZ);
        Vec3 nearestPoint = new Vec3(nearestX, nearestY, nearestZ);
        return playerEyePos.distanceTo(nearestPoint);
    }

    private int getBestSwapSlot() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        int bestSlot = -1;
        double bestDamage = -1;
        for (int i = 0; i < 9; ++i) {
            if (i == currentSlot) {
                continue;
            }
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            double damage = Utils.getDamage(stack);
            if (damage != 0) {
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot == -1) {
            for (int i = 0; i < 9; ++i) {
                if (i == currentSlot) {
                    continue;
                }
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack == null || Arrays.stream(swapBlacklist).noneMatch(stack.getUnlocalizedName().toLowerCase()::contains)) {
                    bestSlot = i;
                    break;
                }
            }
        }

        return bestSlot;
    }

    public void resetYaw() {
        float serverYaw = RotationUtils.serverRotations[0];
        float unwrappedYaw = unwrapYaw(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw), serverYaw);
        mc.thePlayer.rotationYaw = unwrappedYaw;
        mc.thePlayer.prevRotationYaw = unwrappedYaw;
    }

    private void interactAt(boolean interactAt, boolean interact, boolean noEvent, boolean requireInteractAt) {
        if (attackingEntity == null) {
            return;
        }
        if (ModuleManager.bedAura.rotate) {
            return;
        }
        boolean sent = false;
        if (interactAt) {
            boolean canHit = RotationUtils.isPossibleToHit(attackingEntity, attackRange.getInput() - 0.005, RotationUtils.serverRotations);
            if (!canHit) {
                return;
            }
            MovingObjectPosition mov = RotationUtils.rayTrace(10, Utils.getTimer().renderPartialTicks, RotationUtils.serverRotations, hitThroughBlocks.isToggled() ? attackingEntity : null);
            if (mov != null && mov.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mov.entityHit == attackingEntity) {
                Vec3 hitVec = mov.hitVec;
                hitVec = new Vec3(hitVec.xCoord - attackingEntity.posX, hitVec.yCoord - attackingEntity.posY, hitVec.zCoord - attackingEntity.posZ);
                if (!noEvent) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(attackingEntity, hitVec));
                }
                else {
                    PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(attackingEntity, hitVec));
                }
                sent = true;
            }
        }
        if (requireInteractAt && !sent) {
            return;
        }
        if (interact) {
            if (!noEvent) {
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(attackingEntity, C02PacketUseEntity.Action.INTERACT));
            }
            else {
                PacketUtils.sendPacketNoEvent(new C02PacketUseEntity(attackingEntity, C02PacketUseEntity.Action.INTERACT));
            }
        }
    }

    private void sendDigPacket() {
        if (!Utils.holdingSword()) {
            return;
        }
        mc.thePlayer.sendQueue.addToSendQueue(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, DOWN));
    }

    private float[] calculateSmoothRotations(float[] targetRotations) {
        if (targetRotations == null) {
            return new float[]{lastYaw, lastPitch};
        }

        float sensitivityFactor = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float rotationFactor = sensitivityFactor * sensitivityFactor * sensitivityFactor * 128F * (float) rotationSpeed.getInput();

        double deltaX = target.posX - mc.thePlayer.posX;
        double deltaZ = target.posZ - mc.thePlayer.posZ;
        double deltaY = target.posY + target.getEyeHeight() - (mc.thePlayer.getEntityBoundingBox().minY
                + (mc.thePlayer.getEntityBoundingBox().maxY - mc.thePlayer.getEntityBoundingBox().minY));

        double horizontalDistance = MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ);
        float targetYaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 180.0 / Math.PI) - 90.0F;
        float targetPitch = (float) (-(MathHelper.atan2(deltaY, horizontalDistance) * 180.0 / Math.PI));
        float randomFactorYaw = (float) (rotationFactor * Utils.randomizeDouble(0.9F, 1.4));
        float randomFactorPitch = (float) (rotationFactor * Utils.randomizeDouble(0.7F, 1.7));

        targetYaw = applyRotationSmoothing(mc.thePlayer.prevRotationYawHead, targetYaw, randomFactorYaw);
        targetPitch = applyRotationSmoothing(mc.thePlayer.prevRotationPitch, targetPitch, randomFactorPitch);

        return new float[]{targetYaw, targetPitch};
    }

    public static float applyRotationSmoothing(float currentRotation, float targetRotation, float speed) {
        float rotationDifference = MathHelper.wrapAngleTo180_float(targetRotation - currentRotation);

        float randomSpeedFactor = (float) Utils.randomizeDouble(0.9F, 1.2F);
        speed *= randomSpeedFactor;

        if (rotationDifference > speed) {
            rotationDifference = speed;
        }

        if (rotationDifference < -speed) {
            rotationDifference = -speed;
        }

        return currentRotation + rotationDifference;
    }



    private void handleInteractAndAttack(double distance, boolean interactAt, boolean interact, boolean swung) {
        if (ModuleManager.antiFireball != null && ModuleManager.antiFireball.isEnabled() && ModuleManager.antiFireball.fireball != null && ModuleManager.antiFireball.attack) {
            if (ModuleManager.bedAura.rotate) {
                return;
            }
            if (!ModuleManager.antiFireball.silentSwing.isToggled()) {
                mc.thePlayer.swingItem();
            }
            else {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            }
            mc.playerController.attackEntity(mc.thePlayer, ModuleManager.antiFireball.fireball);
            if (interact) {
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(ModuleManager.antiFireball.fireball, C02PacketUseEntity.Action.INTERACT));
            }
        }
        else {
            handleSwingAndAttack(distance, swung);
            interactAt(interactAt, interact, false, false);
        }
    }

    public void resetBlinkState(boolean unblock) {
        releasePackets();
        blockingServer = false;
        if (Raven.packetsHandler.playerSlot.get() != mc.thePlayer.inventory.currentItem && swapped) {
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            Raven.packetsHandler.playerSlot.set(mc.thePlayer.inventory.currentItem);
            swapped = false;
        }
        if (lag && unblock) {
            sendDigPacket();
        }
        lag = false;
    }

    private void releasePackets() {
        try {
            synchronized (blinkedPackets) {
                for (Packet packet : blinkedPackets) {
                    Raven.packetsHandler.handlePacket(packet);
                    PacketUtils.sendPacketNoEvent(packet);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Utils.sendModuleMessage(this, "&cThere was an error releasing blinked packets");
        }
        blinkedPackets.clear();
    }

    private boolean inRange(final Entity target, final double distance) {
        return RotationUtils.isPossibleToHit(target, distance, RotationUtils.getRotations(target));
    }

    private boolean isHostile(EntityCreature entityCreature) {
        if (entityCreature instanceof EntityIronGolem || entityCreature instanceof EntitySilverfish) {
            String teamColor = Utils.getFirstColorCode(entityCreature.getCustomNameTag());
            String teamColorSelf = Utils.getFirstColorCode(mc.thePlayer.getDisplayName().getFormattedText());
            if (!teamColor.isEmpty() && teamColorSelf.equals(teamColor)) { // same team
                return false;
            }
            return true;
        }
        else {
            return hostileMobs.contains(entityCreature);
        }
    }

    private boolean manualBlock() {
        return !manualBlock.isToggled() || Mouse.isButtonDown(1);
    }

    static class KillAuraTarget {
        double distance;
        float health;
        int hurttime;
        double yawDelta;
        int entityId;
        boolean isEnemy;

        public KillAuraTarget(double distance, float health, int hurttime, double yawDelta, int entityId, boolean isEnemy) {
            this.distance = distance;
            this.health = health;
            this.hurttime = hurttime;
            this.yawDelta = yawDelta;
            this.entityId = entityId;
            this.isEnemy = isEnemy;
        }
    }
}