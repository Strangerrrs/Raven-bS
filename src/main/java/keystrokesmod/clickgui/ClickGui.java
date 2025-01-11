package keystrokesmod.clickgui;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.components.Component;
import keystrokesmod.clickgui.components.impl.BindComponent;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.utility.Commands;
import keystrokesmod.utility.Theme;
import keystrokesmod.utility.Timer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static keystrokesmod.module.impl.render.HUD.theme;

public class ClickGui extends GuiScreen {
    private ScheduledFuture sf;
    private Timer logoSmoothWidth;
    private Timer logoSmoothLength;
    private Timer smoothEntity;
    private Timer backgroundFade;
    private Timer blurSmooth;
    private ScaledResolution sr;
    private GuiButtonExt commandLineSend;
    private GuiTextField commandLineInput;
    public static ArrayList<CategoryComponent> categories;
    public int originalScale;

    public ClickGui() {
        categories = new ArrayList();
        int y = 5;
        Module.category[] values;
        int length = (values = Module.category.values()).length;

        for (int i = 0; i < length; ++i) {
            Module.category c = values[i];
            CategoryComponent categoryComponent = new CategoryComponent(c);
            categoryComponent.setY(y);
            categories.add(categoryComponent);
            y += 20;
        }
    }

    public void initMain() {
        (this.logoSmoothWidth = this.smoothEntity = this.blurSmooth = this.backgroundFade = new Timer(500.0F)).start();
        this.sf = Raven.getExecutor().schedule(() -> {
            (this.logoSmoothLength = new Timer(650.0F)).start();
        }, 650L, TimeUnit.MILLISECONDS);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.sr = new ScaledResolution(this.mc);
        for (CategoryComponent categoryComponent : categories) {
            categoryComponent.setScreenHeight(this.sr.getScaledHeight());
        }
        (this.commandLineInput = new GuiTextField(1, this.mc.fontRendererObj, 22, this.height - 100, 150, 20)).setMaxStringLength(256);
        this.buttonList.add(this.commandLineSend = new GuiButtonExt(2, 22, this.height - 70, 150, 20, "Send"));
        this.commandLineSend.visible = CommandLine.a;
    }

    public void drawScreen(int x, int y, float p) {
        if (Gui.backgroundBlur.getInput() != 0) {
            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(0, 0, this.width, this.height, 0.0f, true, Color.black);
            float inputToRange = (float) (3 * ((Gui.backgroundBlur.getInput() + 35) / 100));
            BlurUtils.blurEnd(2, this.blurSmooth.getValueFloat(0, inputToRange, 1));
        }
        if (Gui.darkBackground.isToggled()) {
            drawRect(0, 0, this.width, this.height, (int) (this.backgroundFade.getValueFloat(0.0F, 0.7F, 2) * 255.0F) << 24);
        }
        int r;
        if (!Gui.removeWatermark.isToggled()) {
            int h = this.height / 4;
            int wd = this.width / 2;
            int w_c = 30 - this.logoSmoothWidth.getValueInt(0, 30, 3);
            this.drawCenteredString(this.fontRendererObj, "r", wd + 1 - w_c, h - 25, Utils.getChroma(2L, 1500L));
            this.drawCenteredString(this.fontRendererObj, "a", wd - w_c, h - 15, Utils.getChroma(2L, 1200L));
            this.drawCenteredString(this.fontRendererObj, "v", wd - w_c, h - 5, Utils.getChroma(2L, 900L));
            this.drawCenteredString(this.fontRendererObj, "e", wd - w_c, h + 5, Utils.getChroma(2L, 600L));
            this.drawCenteredString(this.fontRendererObj, "n", wd - w_c, h + 15, Utils.getChroma(2L, 300L));
            this.drawCenteredString(this.fontRendererObj, "b4", wd + 1 + w_c, h + 30, Utils.getChroma(2L, 0L));
            this.drawVerticalLine(wd - 10 - w_c, h - 30, h + 43, Color.white.getRGB());
            this.drawVerticalLine(wd + 10 + w_c, h - 30, h + 43, Color.white.getRGB());
            if (this.logoSmoothLength != null) {
                r = this.logoSmoothLength.getValueInt(0, 20, 2);
                this.drawHorizontalLine(wd - 10, wd - 10 + r, h - 29, -1);
                this.drawHorizontalLine(wd + 10, wd + 10 - r, h + 42, -1);
            }
        }

        ScaledResolution sr = new ScaledResolution(mc);
        FontRenderer fontRenderer = this.mc.fontRendererObj;
        Color color = new Color(57, 146, 229);
        int startX = 5;
        int startY = sr.getScaledHeight() - 35;

        int gradientColor = Theme.getGradient((int) theme.getInput(), 0);
        String fixedPart1 = "welcome, ";
        String dynamicPart = Utils.getServerName();
        String fixedPart2 = " (#1)";

        fontRenderer.drawString(fixedPart1, startX, startY, color.getRGB());
        int dynamicStartX = startX + fontRenderer.getStringWidth(fixedPart1);
        fontRenderer.drawString(dynamicPart, dynamicStartX, startY, gradientColor);
        int fixedPart2StartX = dynamicStartX + fontRenderer.getStringWidth(dynamicPart);
        fontRenderer.drawString(fixedPart2, fixedPart2StartX, startY, color.getRGB());

        fontRenderer.drawString("raven premium-15", startX, startY + 10, color.getRGB());

        String original = "dev. blowsy";
        String hiddenChar = "§k";

        String[] gradualText = new String[original.length() - 4 + 1];

        for (int i = 0; i < gradualText.length; i++) {
            int insertPosition = 5 + i;
            StringBuilder sb = new StringBuilder(original);
            if (insertPosition < sb.length()) {
                sb.insert(insertPosition, hiddenChar);
            }
            gradualText[i] = sb.toString();
        }

        int tick = (int) (System.currentTimeMillis() / 300 % gradualText.length);
        String text = gradualText[tick];
        fontRenderer.drawString(text, startX, startY + 20, color.getRGB());

        for (CategoryComponent c : categories) {
            c.render(this.fontRendererObj);
            c.mousePosition(x, y);

            for (Component m : c.getModules()) {
                m.drawScreen(x, y);
            }
        }

        GL11.glColor3f(1.0f, 1.0f, 1.0f);
        if (!Gui.removePlayerModel.isToggled()) {
            GlStateManager.pushMatrix();
            GlStateManager.disableBlend();
            GuiInventory.drawEntityOnScreen(this.width + 15 - this.smoothEntity.getValueInt(0, 40, 2), this.height - 10, 40, (float) (this.width - 25 - x), (float) (this.height - 50 - y), this.mc.thePlayer);
            GlStateManager.enableBlend();
            GlStateManager.popMatrix();
        }


        if (CommandLine.a) {
            if (!this.commandLineSend.visible) {
                this.commandLineSend.visible = true;
            }

            r = CommandLine.animate.isToggled() ? CommandLine.an.getValueInt(0, 200, 2) : 200;
            if (CommandLine.b) {
                r = 200 - r;
                if (r == 0) {
                    CommandLine.b = false;
                    CommandLine.a = false;
                    this.commandLineSend.visible = false;
                }
            }
            drawRect(0, 0, r, this.height, -1089466352);
            this.drawHorizontalLine(0, r - 1, (this.height - 345), -1);
            this.drawHorizontalLine(0, r - 1, (this.height - 115), -1);
            drawRect(r - 1, 0, r, this.height, -1);
            Commands.rc(this.fontRendererObj, this.height, r, this.sr.getScaleFactor());
            int x2 = r - 178;
            this.commandLineInput.xPosition = x2;
            this.commandLineSend.xPosition = x2;
            this.commandLineInput.drawTextBox();
            super.drawScreen(x, y, p);
        }
        else if (CommandLine.b) {
            CommandLine.b = false;
        }
    }

    public void mouseClicked(int x, int y, int m) throws IOException {
        Iterator var4 = categories.iterator();

        while (true) {
            CategoryComponent category;
            do {
                do {
                    if (!var4.hasNext()) {
                        if (CommandLine.a) {
                            this.commandLineInput.mouseClicked(x, y, m);
                            super.mouseClicked(x, y, m);
                        }

                        return;
                    }

                    category = (CategoryComponent) var4.next();
                    if (category.v(x, y) && !category.i(x, y) && m == 0) {
                        category.overTitle(true);
                        category.xx = x - category.getX();
                        category.yy = y - category.getY();
                    }

                    if (category.overTitle(x, y) && m == 1) {
                        category.mouseClicked(!category.isOpened());
                    }

                    if (category.i(x, y) && m == 0) {
                        category.cv(!category.p());
                    }
                } while (!category.isOpened());
            } while (category.getModules().isEmpty());

            for (Component c : category.getModules()) {
                if (c.onClick(x, y, m) && c instanceof ModuleComponent) {
                    category.openModule((ModuleComponent) c);
                }
            }

        }
    }

    public void mouseReleased(int x, int y, int s) {
        if (s == 0) {
            Iterator<CategoryComponent> iterator = categories.iterator();
            while (iterator.hasNext()) {
                CategoryComponent category = iterator.next();
                category.overTitle(false);
                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.mouseReleased(x, y, s);
                    }
                }
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheelInput = Mouse.getDWheel();
        if (wheelInput != 0) {
            for (CategoryComponent category : categories) {
                category.onScroll(wheelInput);
            }
        }
    }

    @Override
    public void setWorldAndResolution(Minecraft p_setWorldAndResolution_1_, final int p_setWorldAndResolution_2_, final int p_setWorldAndResolution_3_) {
        this.mc = p_setWorldAndResolution_1_;
        originalScale = this.mc.gameSettings.guiScale;
        this.mc.gameSettings.guiScale = (int) Gui.guiScale.getInput() + 1;
        this.itemRender = p_setWorldAndResolution_1_.getRenderItem();
        this.fontRendererObj = p_setWorldAndResolution_1_.fontRendererObj;
        final ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        this.width = scaledresolution.getScaledWidth();
        this.height = scaledresolution.getScaledHeight();
        if (!MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Pre(this, this.buttonList))) {
            this.buttonList.clear();
            this.initGui();
        }
        MinecraftForge.EVENT_BUS.post(new GuiScreenEvent.InitGuiEvent.Post(this, this.buttonList));
    }

    @Override
    public void keyTyped(char t, int k) {
        if (k == Keyboard.KEY_ESCAPE && !binding()) {
            this.mc.displayGuiScreen(null);
        } else {
            Iterator<CategoryComponent> iterator = categories.iterator();
            while (iterator.hasNext()) {
                CategoryComponent category = iterator.next();

                if (category.isOpened() && !category.getModules().isEmpty()) {
                    for (Component module : category.getModules()) {
                        module.keyTyped(t, k);
                    }
                }
            }
            if (CommandLine.a) {
                String cm = this.commandLineInput.getText();
                if (k == 28 && !cm.isEmpty()) {
                    Commands.rCMD(this.commandLineInput.getText());
                    this.commandLineInput.setText("");
                    return;
                }
                this.commandLineInput.textboxKeyTyped(t, k);
            }
        }
    }

    public void actionPerformed(GuiButton b) {
        if (b == this.commandLineSend) {
            Commands.rCMD(this.commandLineInput.getText());
            this.commandLineInput.setText("");
        }
    }

    @Override
    public void onGuiClosed() {
        this.logoSmoothLength = null;
        if (this.sf != null) {
            this.sf.cancel(true);
            this.sf = null;
        }
        for (CategoryComponent c : categories) {
            c.dragging = false;
            for (Component m : c.getModules()) {
                m.onGuiClosed();
            }
        }
        this.mc.gameSettings.guiScale = originalScale;
    }

    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean binding() {
        for (CategoryComponent c : categories) {
            for (ModuleComponent m : c.getModules()) {
                for (Component component : m.settings) {
                    if (component instanceof BindComponent && ((BindComponent) component).isBinding) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static int[] calculateBlur(int setting) {
        int passes = (int) (setting * 6.0 / 100.0 + 1.0);
        int offset = (int) (setting * 3.0 / 100.0);

        return new int[]{passes, offset};
    }
}
