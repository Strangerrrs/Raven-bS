package keystrokesmod.module.impl.combat;

import keystrokesmod.mixin.impl.accessor.IAccessorGuiScreen;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Reflection;
import keystrokesmod.utility.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.util.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class AutoClicker extends Module {
    private final SliderSetting minCPS, maxCPS;
    public static ButtonSetting leftClick;
    private final ButtonSetting rightClick, breakBlocks, inventoryFill, weaponOnly, blocksOnly;

    private long nextClickTime, leftReleaseTime, rightReleaseTime, lastFrameTime;
    private int burstCount;
    private boolean inBurst, isHoldingBlockBreak;
    private double currentCPS, burstBaseCPS, clickVariance;
    private final Random rand = ThreadLocalRandom.current();

    public AutoClicker() {
        super("AutoClicker", category.combat, 0);
        registerSetting(minCPS = new SliderSetting("Min CPS", 12.0, 1.0, 25.0, 0.5));
        registerSetting(maxCPS = new SliderSetting("Max CPS", 20.0, 1.0, 25.0, 0.5));
        registerSetting(leftClick = new ButtonSetting("Left click", true));
        registerSetting(rightClick = new ButtonSetting("Right click", false));
        registerSetting(breakBlocks = new ButtonSetting("Break blocks", false));
        registerSetting(inventoryFill = new ButtonSetting("Inventory fill", false));
        registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
        registerSetting(blocksOnly = new ButtonSetting("Blocks only", true));
        closetModule = true;
    }

    @Override
    public void onEnable() {
        burstCount = 0;
        inBurst = false;
        currentCPS = (minCPS.getInput() + maxCPS.getInput()) / 2;
        burstBaseCPS = generateBurstBaseCPS();
        // Use Gaussian jitter for variance (clamped to [0.8, 1.2])
        clickVariance = Math.max(0.8, Math.min(1.2, 1.0 + 0.1 * rand.nextGaussian()));
        lastFrameTime = System.currentTimeMillis();
        nextClickTime = leftReleaseTime = rightReleaseTime = 0;
    }

    @SubscribeEvent
    public void onRenderTick(RenderTickEvent ev) {
        if (ev.phase != Phase.END || !Utils.nullCheck() || Utils.isConsuming(mc.thePlayer)) return;
        long now = System.currentTimeMillis();
        long delta = now - lastFrameTime;
        lastFrameTime = now;
        updateBurstState(delta);
        if (mc.currentScreen == null && mc.inGameHasFocus) handleCombat(now);
        else if (inventoryFill.isToggled() && mc.currentScreen instanceof GuiInventory) handleInventory(now);
        processReleases(now);
    }

    // Update burst state and recalc variance using Gaussian jitter
    private void updateBurstState(long delta) {
        if (!inBurst && rand.nextInt(100) < 20) {
            inBurst = true;
            burstBaseCPS = generateBurstBaseCPS();
            burstCount = 15 + rand.nextInt(10);
        } else if (inBurst && burstCount <= 0) {
            inBurst = false;
            burstBaseCPS = generateNormalBaseCPS();
        }
        double target = inBurst ? burstBaseCPS : generateNormalBaseCPS();
        currentCPS += (target - currentCPS) * delta / 100.0;
        currentCPS = Math.max(minCPS.getInput(), Math.min(maxCPS.getInput(), currentCPS));
        clickVariance = Math.max(0.8, Math.min(1.2, 1.0 + 0.1 * rand.nextGaussian()));
    }

    private void handleCombat(long now) {
        if (weaponOnly.isToggled() && !Utils.holdingWeapon()) return;
        boolean l = leftClick.isToggled() && Mouse.isButtonDown(0);
        boolean r = rightClick.isToggled() && Mouse.isButtonDown(1);
        if (l) {
            if (breakBlocks.isToggled()) handleBlock(now);
            else if (now >= nextClickTime) performClick(now, 0, mc.gameSettings.keyBindAttack.getKeyCode());
        }
        if (r && (!blocksOnly.isToggled() || isHoldingBlock()) && now >= nextClickTime)
            performClick(now, 1, mc.gameSettings.keyBindUseItem.getKeyCode());
    }

    // For block breaking, similar to combat click but ensures block exists
    private void handleBlock(long now) {
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null) return;
        Block b = mc.theWorld.getBlockState(pos).getBlock();
        if (b == Blocks.air || b instanceof BlockLiquid) {
            if (isHoldingBlockBreak) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
                isHoldingBlockBreak = false;
            }
            return;
        }
        if (now >= nextClickTime) {
            performClick(now, 0, mc.gameSettings.keyBindAttack.getKeyCode());
            isHoldingBlockBreak = true;
        }
    }

    // Modified click: uses Gaussian jitter and occasional hesitation events
    private void performClick(long now, int button, int key) {
        // Mean delay (ms) per click based on CPS, modulated by variance:
        double meanDelay = 1000.0 / currentCPS;
        // Use Gaussian multiplier (about 1.0 ± 0.1)
        double delayMultiplier = 1.0 + 0.1 * rand.nextGaussian();
        long delay = (long) (meanDelay * delayMultiplier * clickVariance);
        // Occasional hesitation (simulate momentary distraction)
        if (rand.nextInt(200) == 0) {
            delay += 50 + rand.nextInt(50);
        }
        // Press duration ratio using Gaussian (clamped between 0.3 and 0.7)
        double pressRatio = 0.4 + 0.1 * rand.nextGaussian();
        pressRatio = Math.max(0.3, Math.min(0.7, pressRatio));
        long press = (long) (delay * pressRatio);

        // Simulate key press and schedule release
        KeyBinding.setKeyBindState(key, true);
        KeyBinding.onTick(key);
        Reflection.setButton(button, true);
        if (button == 0) leftReleaseTime = now + press;
        else rightReleaseTime = now + press;
        nextClickTime = now + delay;
        burstCount--;
    }

    private double generateBurstBaseCPS() {
        return Math.min(maxCPS.getInput(), minCPS.getInput() + 5 + rand.nextDouble() * 5);
    }

    private double generateNormalBaseCPS() {
        return (minCPS.getInput() + maxCPS.getInput()) / 2.0;
    }

    private boolean isHoldingBlock() {
        ItemStack it = mc.thePlayer.getCurrentEquippedItem();
        return it != null && it.getItem() instanceof ItemBlock;
    }

    private void handleInventory(long now) {
        if (!Mouse.isButtonDown(0) || (!Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
            nextClickTime = 0;
            return;
        }
        if (now >= nextClickTime) {
            doInventoryClick();
            updateInvDelay(now);
        }
    }

    private void doInventoryClick() {
        GuiScreen s = mc.currentScreen;
        if (s instanceof GuiInventory) {
            int x = Mouse.getX() * s.width / mc.displayWidth;
            int y = s.height - Mouse.getY() * s.height / mc.displayHeight - 1;
            // Add slight random movement jitter
            x += rand.nextInt(3) - 1;
            y += rand.nextInt(3) - 1;
            ((IAccessorGuiScreen)s).callMouseClicked(x, y, 0);
            if (rand.nextInt(10) < 2)
                ((IAccessorGuiScreen)s).callMouseClicked(x, y, 0);
        }
    }

    private void updateInvDelay(long now) {
        double d = 50 + rand.nextDouble() * 150;
        if (rand.nextInt(10) < 3) d *= 0.6;
        nextClickTime = now + (long)d;
    }

    private void processReleases(long now) {
        if (leftReleaseTime != 0 && now >= leftReleaseTime) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
            Reflection.setButton(0, false);
            leftReleaseTime = 0;
        }
        if (rightReleaseTime != 0 && now >= rightReleaseTime) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            Reflection.setButton(1, false);
            rightReleaseTime = 0;
        }
    }

    @Override
    public void onDisable() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
        leftReleaseTime = rightReleaseTime = 0;
    }
}
