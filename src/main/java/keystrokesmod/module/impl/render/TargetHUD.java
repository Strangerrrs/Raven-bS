
package keystrokesmod.module.impl.render;

import keystrokesmod.module.Module;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.RenderUtils;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;

public class TargetHUD extends Module {
    private SliderSetting mode;
    private SliderSetting theme;
    private ButtonSetting renderEsp;
    private ButtonSetting showStatus;
    private ButtonSetting healthColor;
    private Timer fadeTimer;
    private Timer healthBarTimer = null;
    private EntityLivingBase target;
    private long lastAliveMS;
    private double lastHealth;
    private float lastHealthBar;
    public EntityLivingBase renderEntity;
    public int posX = 70;
    public int posY = 30;
    private String[] modes = new String[]{ "Modern", "Legacy", "Simple" };

    public TargetHUD() {
        super("TargetHUD", category.render);
        this.registerSetting(new DescriptionSetting("Only works with KillAura."));
        this.registerSetting(mode = new SliderSetting("Mode", 1, modes));
        this.registerSetting(theme = new SliderSetting("Theme", 0, Theme.themes));
        this.registerSetting(new ButtonSetting("Edit position", () -> {
            mc.displayGuiScreen(new EditScreen());
        }));
        this.registerSetting(renderEsp = new ButtonSetting("Render ESP", true));
        this.registerSetting(showStatus = new ButtonSetting("Show win or loss", true));
        this.registerSetting(healthColor = new ButtonSetting("Traditional health color", false));
    }

    public void onDisable() {
        reset();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent ev) {
        if (!Utils.nullCheck()) {
            reset();
            return;
        }
        if (ev.phase == TickEvent.Phase.END) {
            if (mc.currentScreen != null) {
                reset();
                return;
            }
            if (KillAura.attackingEntity != null) {
                target = KillAura.attackingEntity;
                lastAliveMS = System.currentTimeMillis();
                fadeTimer = null;
            } else if (target != null) {
                if (System.currentTimeMillis() - lastAliveMS >= 400 && fadeTimer == null) {
                    (fadeTimer = new Timer(400)).start();
                }
            }
            else {
                return;
            }
            String playerInfo = target.getDisplayName().getFormattedText();
            double health = target.getHealth() / target.getMaxHealth();
            if (target.isDead) {
                health = 0;
            }
            if (health != lastHealth) {
                (healthBarTimer = new Timer(mode.getInput() == 0 ? 500 : 350)).start();
            }
            lastHealth = health;
            playerInfo += " " + Utils.getHealthStr(target, true);
            drawTargetHUD(fadeTimer, playerInfo, health);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderWorld(RenderWorldLastEvent renderWorldLastEvent) {
        if (!renderEsp.isToggled() || !Utils.nullCheck()) {
            return;
        }
        if (KillAura.target != null) {
            RenderUtils.renderEntity(KillAura.target, 2, 0.0, 0.0, Theme.getGradient((int) theme.getInput(), 0), false);
        }
        else if (renderEntity != null) {
            RenderUtils.renderEntity(renderEntity, 2, 0.0, 0.0, Theme.getGradient((int) theme.getInput(), 0), false);
        }
    }

    private void drawTargetHUD(Timer fadeTimer, String string, double health) {
        if (showStatus.isToggled()) {
            string = string + " " + ((health <= Utils.getCompleteHealth(mc.thePlayer) / mc.thePlayer.getMaxHealth()) ? "§aW" : "§cL");
        }
        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        final int padding = 8;
        final int targetStrWithPadding = mc.fontRendererObj.getStringWidth(string) + padding;
        final int x = (scaledResolution.getScaledWidth() / 2 - targetStrWithPadding / 2) + posX;
        final int y = (scaledResolution.getScaledHeight() / 2 + 15) + posY;
        final int n6 = x - padding;
        final int n7 = y - padding;
        final int n8 = x + targetStrWithPadding;
        final int n9 = y + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding;
        final int alpha = (fadeTimer == null) ? 255 : (255 - fadeTimer.getValueInt(0, 255, 1));
        if (alpha > 0) {
            final int maxAlphaOutline = (alpha > 110) ? 110 : alpha;
            final int maxAlphaBackground = (alpha > 210) ? 210 : alpha;
            final int[] gradientColors = Theme.getGradients((int) theme.getInput());
            switch ((int) mode.getInput()) {
                case 0:
                    float bloomRadius = (fadeTimer == null) ? 2f : (2f * alpha / 255f);
                    float blurRadius = (fadeTimer == null) ? 3 : (3f * alpha / 255f);
                    BlurUtils.prepareBloom();
                    RoundedUtils.drawRound((float) n6, (float) n7, Math.abs((float) n6 - n8), Math.abs((float) n7 - (n9 + 13)), 8.0f, true, new Color(0, 0, 0, maxAlphaBackground));
                    BlurUtils.bloomEnd(3, bloomRadius);
                    BlurUtils.prepareBlur();
                    RoundedUtils.drawRound((float) n6, (float) n7, Math.abs((float) n6 - n8), Math.abs((float) n7 - (n9 + 13)), 8.0f, true, new Color(Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline)));
                    BlurUtils.blurEnd(2, blurRadius);
                    break;
                case 1:
                    RenderUtils.drawRoundedGradientOutlinedRectangle((float) n6, (float) n7, (float) n8, (float) (n9 + 13), 10.0f, Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline), Utils.mergeAlpha(gradientColors[0], alpha), Utils.mergeAlpha(gradientColors[1], alpha));
                    break;
                case 2:
                    RenderUtils.drawRoundedRectangle((float) n6, (float) n7, Math.abs((float) n6 - n8), Math.abs((float) n7 - (n9 + 13)), 8.0f, new Color(0, 0, 0, maxAlphaBackground).getRGB());
                    break;
            }
            final int n13 = n6 + 6;
            final int n14 = n8 - 6;
            final int n15 = n9;

            // Bar background
            RenderUtils.drawRoundedRectangle((float) n13, (float) n15, (float) n14, (float) (n15 + 5), 4.0f, Utils.mergeAlpha(Color.black.getRGB(), maxAlphaOutline));
            int mergedGradientLeft = Utils.mergeAlpha(gradientColors[0], maxAlphaBackground);
            int mergedGradientRight = Utils.mergeAlpha(gradientColors[1], maxAlphaBackground);
            float healthBar = (float) (int) (n14 + (n13 - n14) * (1 - health));
            boolean smoothBack = false;
            if (healthBar != lastHealthBar && lastHealthBar - n13 >= 3 && healthBarTimer != null ) {
                int type = mode.getInput() == 0 ? 4 : 1;
                float diff = lastHealthBar - healthBar;
                if (diff > 0) {
                    lastHealthBar = lastHealthBar - healthBarTimer.getValueFloat(0, diff, type);
                }
                else {
                    smoothBack = true;
                    lastHealthBar = healthBarTimer.getValueFloat(lastHealthBar, healthBar, type);
                }
            }
            else {
                lastHealthBar = healthBar;
            }
            if (healthColor.isToggled()) {
                mergedGradientLeft = mergedGradientRight = Utils.mergeAlpha(Utils.getColorForHealth(health), maxAlphaBackground);
            }
            if (lastHealthBar > n14) { // exceeds total width then clamp
                lastHealthBar = n14;
            }

            switch ((int) mode.getInput()) { // health bar
                case 0:
                    RenderUtils.drawRoundedRectangle((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f, Utils.darkenColor(mergedGradientRight, 25));
                    RenderUtils.drawRoundedGradientRect((float) n13, (float) n15, smoothBack ? lastHealthBar : healthBar, (float) (n15 + 5), 4.0f, mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                    break;
                case 1:
                    RenderUtils.drawRoundedGradientRect((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f, mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                    break;
                case 2:
                    RenderUtils.drawRoundedRectangle((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f, mergedGradientLeft);
                    break;
            }
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_BLEND);
            mc.fontRendererObj.drawString(string, (float) x, (float) y, (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | Utils.clamp(alpha + 15) << 24, true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
        else {
            target = null;
            healthBarTimer = null;
        }
    }

    private void reset() {
        fadeTimer = null;
        target = null;
        healthBarTimer = null;
        renderEntity = null;
    }

    class EditScreen extends GuiScreen {
        GuiButtonExt resetPosition;
        boolean d = false;
        int miX = 0;
        int miY = 0;
        int maX = 0;
        int maY = 0;
        int aX = 70;
        int aY = 30;
        int laX = 0;
        int laY = 0;
        int lmX = 0;
        int lmY = 0;
        int clickMinX = 0;

        public void initGui() {
            super.initGui();
            this.buttonList.add(this.resetPosition = new GuiButtonExt(1, this.width - 90, this.height - 25, 85, 20, "Reset position"));
            this.aX = posX;
            this.aY = posY;
        }

        public void drawScreen(int mX, int mY, float pt) {
            ScaledResolution res = new ScaledResolution(this.mc);
            drawRect(0, 0, this.width, this.height, -1308622848);
            int miX = this.aX;
            int miY = this.aY;
            String playerInfo = mc.thePlayer.getDisplayName().getFormattedText();
            double health = mc.thePlayer.getHealth() / mc.thePlayer.getMaxHealth();
            if (mc.thePlayer.isDead) {
                health = 0;
            }
            lastHealth = health;
            playerInfo += " " + Utils.getHealthStr(mc.thePlayer, true);
            drawTargetHUD(null, playerInfo, health);
            if (showStatus.isToggled()) {
                playerInfo = playerInfo + " " + ((health <= Utils.getCompleteHealth(mc.thePlayer) / mc.thePlayer.getMaxHealth()) ? "§aW" : "§cL");
            }
            int stringWidth = mc.fontRendererObj.getStringWidth(playerInfo) + 8;
            int maX = (res.getScaledWidth() / 2 - stringWidth / 2) + miX + mc.fontRendererObj.getStringWidth(playerInfo) + 8;
            int maY = (res.getScaledHeight() / 2 + 15) +  miY + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + 8;
            this.miX = miX;
            this.miY = miY;
            this.maX = maX;
            this.maY = maY;
            this.clickMinX = miX;
            posX = miX;
            posY = miY;
            String edit = "Edit the HUD position by dragging.";
            int x = res.getScaledWidth() / 2 - fontRendererObj.getStringWidth(edit) / 2;
            int y = res.getScaledHeight() / 2 - 20;
            RenderUtils.drawColoredString(edit, '-', x, y, 2L, 0L, true, this.mc.fontRendererObj);

            try {
                this.handleInput();
            }
            catch (IOException var12) {
            }

            super.drawScreen(mX, mY, pt);
        }

        protected void mouseClickMove(int mX, int mY, int b, long t) {
            super.mouseClickMove(mX, mY, b, t);
            if (b == 0) {
                if (this.d) {
                    this.aX = this.laX + (mX - this.lmX);
                    this.aY = this.laY + (mY - this.lmY);
                }
                else if (mX > this.clickMinX && mX < this.maX && mY > this.miY && mY < this.maY) {
                    this.d = true;
                    this.lmX = mX;
                    this.lmY = mY;
                    this.laX = this.aX;
                    this.laY = this.aY;
                }

            }
        }

        protected void mouseReleased(int mX, int mY, int s) {
            super.mouseReleased(mX, mY, s);
            if (s == 0) {
                this.d = false;
            }

        }

        public void actionPerformed(GuiButton b) {
            if (b == this.resetPosition) {
                this.aX = posX = 70;
                this.aY = posY = 30;
            }

        }

        public boolean doesGuiPauseGame() {
            return false;
        }
    }
}
