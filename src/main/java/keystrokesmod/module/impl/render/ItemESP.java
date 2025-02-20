package keystrokesmod.module.impl.render;

import keystrokesmod.mixin.impl.accessor.IAccessorMinecraft;
import keystrokesmod.module.Module;
import keystrokesmod.module.impl.player.Freecam;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.RenderUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ItemESP extends Module {
    private final ButtonSetting renderIron;
    private final ButtonSetting renderGold;

    public ItemESP() {
        super("ItemESP", category.render);
        this.registerSetting(renderIron = new ButtonSetting("Render iron", true));
        this.registerSetting(renderGold = new ButtonSetting("Render gold", true));
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        HashMap<Item, ArrayList<EntityItem>> itemsMap = new HashMap<>();
        HashMap<Double, Integer> colorMap = new HashMap<>();

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityItem) {
                if (entity.ticksExisted < 3) {
                    continue;
                }
                EntityItem entityItem = (EntityItem) entity;
                if (entityItem.getEntityItem().stackSize == 0) {
                    continue;
                }
                Item currentItem = entityItem.getEntityItem().getItem();
                if (currentItem == null) {
                    continue;
                }

                int stackSize = entityItem.getEntityItem().stackSize;
                double colorDouble = getColorForItem(currentItem, entity.posX, entity.posY, entity.posZ);

                Integer existingStackCount = colorMap.get(colorDouble);
                int newStackCount;
                if (existingStackCount == null) {
                    newStackCount = stackSize;
                    ArrayList<EntityItem> itemList = itemsMap.get(currentItem);
                    if (itemList == null) {
                        itemList = new ArrayList<>();
                    }
                    itemList.add(entityItem);
                    itemsMap.put(currentItem, itemList);
                }
                else {
                    newStackCount = existingStackCount + stackSize;
                }
                colorMap.put(colorDouble, newStackCount);
            }
        }
        if (!itemsMap.isEmpty()) {
            float renderPartialTicks = ((IAccessorMinecraft) mc).getTimer().renderPartialTicks;
            for (Map.Entry<Item, ArrayList<EntityItem>> entry : itemsMap.entrySet()) {
                Item item = entry.getKey();
                int boxColor;
                int textColor;
                if (item == Items.iron_ingot && renderIron.isToggled()) {
                    textColor = (boxColor = -1);
                }
                else if (item == Items.gold_ingot && renderGold.isToggled()) {
                    boxColor = -331703;
                    textColor = -152;
                }
                else if (item == Items.golden_apple) {
                    boxColor = -331703;
                    textColor = -152;
                }
                else if (item == Items.diamond) {
                    boxColor = -10362113;
                    textColor = -7667713;
                }
                else {
                    if (item != Items.emerald) {
                        continue;
                    }
                    boxColor = -15216030;
                    textColor = -14614644;
                }

                for (EntityItem entityItem2 : entry.getValue()) {
                    double itemColor = getColorForItem(item, entityItem2.posX, entityItem2.posY, entityItem2.posZ);
                    double interpolatedX = entityItem2.lastTickPosX + (entityItem2.posX - entityItem2.lastTickPosX) * renderPartialTicks;
                    double interpolatedY = entityItem2.lastTickPosY + (entityItem2.posY - entityItem2.lastTickPosY) * renderPartialTicks;
                    double interpolatedZ = entityItem2.lastTickPosZ + (entityItem2.posZ - entityItem2.lastTickPosZ) * renderPartialTicks;

                    EntityPlayer self = (Freecam.freeEntity == null) ? mc.thePlayer : Freecam.freeEntity;
                    double diffX = self.lastTickPosX + (self.posX - self.lastTickPosX) * renderPartialTicks - interpolatedX;
                    double diffY = self.lastTickPosY + (self.posY - self.lastTickPosY) * renderPartialTicks - interpolatedY;
                    double diffZ = self.lastTickPosZ + (self.posZ - self.lastTickPosZ) * renderPartialTicks - interpolatedZ;

                    double dist = MathHelper.sqrt_double(diffX * diffX + diffY * diffY + diffZ * diffZ);

                    GlStateManager.pushMatrix();
                    drawBox(boxColor, textColor, colorMap.get(itemColor), interpolatedX, interpolatedY, interpolatedZ, dist);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    public double getColor(double x, double y, double z) {
        if (x == 0.0) {
            x = 1.0;
        }
        if (y == 0.0) {
            y = 1.0;
        }
        if (z == 0.0) {
            z = 1.0;
        }
        return Math.round((x + 1.0) * Math.floor(y) * (z + 2.0));
    }

    private double getColorForItem(Item item, double x, double y, double z) {
        double color = getColor(x, y, z);
        if (item == Items.iron_ingot) {
            color += 0.155;
        }
        else if (item == Items.gold_ingot) {
            color += 0.255;
        }
        else if (item == Items.golden_apple) {
            color += 0.255;
        }
        else if (item == Items.diamond) {
            color += 0.355;
        }
        else if (item == Items.emerald) {
            color += 0.455;
        }
        return color;
    }

    public void drawBox(int boxColor, int textColor, int size, double posY, double posX, double posZ, double dist) {
        posY -= mc.getRenderManager().viewerPosX;
        posX -= mc.getRenderManager().viewerPosY;
        posZ -= mc.getRenderManager().viewerPosZ;
        GL11.glPushMatrix();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glLineWidth(2.0f);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        float r = (boxColor >> 16 & 0xFF) / 255.0f;
        float g = (boxColor >> 8 & 0xFF) / 255.0f;
        float b = (boxColor & 0xFF) / 255.0f;

        float radius = Math.min(Math.max(0.2f, (float) (0.009999999776482582 * dist)), 0.4f);
        RenderUtils.drawBoundingBox(new AxisAlignedBB(posY - radius, posX, posZ - radius, posY + radius, posX + radius * 2.0f, posZ + radius), r, g, b, 0.35f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posY, (float) posX + 0.3, (float) posZ);
        GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate((mc.gameSettings.thirdPersonView == 2 ? -1 : 1) * mc.getRenderManager().playerViewX, 1.0f, 0.0f, 0.0f);
        float scale = Math.min(Math.max(0.02266667f, (float) (0.001500000013038516 * dist)), 0.07f);
        GlStateManager.scale(-scale, -scale, -scale);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        String value = String.valueOf(size);
        mc.fontRendererObj.drawString(value, -(mc.fontRendererObj.getStringWidth(value) / 2) + scale * 3.5f, -(123.805f * scale - 2.47494f), textColor, true);
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }
}
