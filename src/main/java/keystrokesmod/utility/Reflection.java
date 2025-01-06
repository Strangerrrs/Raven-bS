package keystrokesmod.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiEnchantment;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.inventory.GuiBrewingStand;
import net.minecraft.client.gui.inventory.GuiDispenser;
import net.minecraft.client.gui.inventory.GuiFurnace;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemFood;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;

public class Reflection {
    public static Field button;
    public static Field buttonstate;
    public static Field buttons;
    public static Field leftClickCounter;
    public static Field jumpTicks;
    public static Field rightClickDelayTimerField;
    public static Field curBlockDamageMP;
    public static Field blockHitDelay;
    public static Method clickMouse;
    public static Method setupCameraTransform;
    public static Method rightClickMouse;
    public static Field shaderResourceLocations;
    public static Field useShader;
    public static Field shaderIndex;
    public static Method loadShader;
    public static Field inGround;
    public static Method getFOVModifier;
    public static Field itemInUseCount;
    public static Field S08PacketPlayerPosLookYaw;
    public static Field S08PacketPlayerPosLookPitch;
    public static Field C02PacketUseEntityEntityId;
    public static Field bookContents;
    public static Field fallDistance;
    public static Field thirdPersonDistance;
    public static Field alwaysEdible;
    public static Field mcGuiInGame;
    public static Field C01PacketChatMessageMessage;
    public static HashMap<Class, Field> containerInventoryPlayer = new HashMap<>();
    private static List<Class> containerClasses = Arrays.asList(GuiFurnace.class, GuiBrewingStand.class, GuiEnchantment.class, ContainerHopper.class, GuiDispenser.class, ContainerWorkbench.class, ContainerMerchant.class, ContainerHorseInventory.class);
    public static boolean sendMessage = false;
    public static Map<String, KeyBinding> keybinds = new HashMap<>();

    public static void getFields() {
        try {
            button = MouseEvent.class.getDeclaredField("button");
            buttonstate = MouseEvent.class.getDeclaredField("buttonstate");
            buttons = Mouse.class.getDeclaredField("buttons");

            leftClickCounter = ReflectionHelper.findField(Minecraft.class, "field_71429_W", "leftClickCounter");

            if (leftClickCounter != null) {
                leftClickCounter.setAccessible(true);
            }

            jumpTicks = ReflectionHelper.findField(EntityLivingBase.class, "field_70773_bE", "jumpTicks");

            if (jumpTicks != null) {
                jumpTicks.setAccessible(true);
            }

            rightClickDelayTimerField = ReflectionHelper.findField(Minecraft.class, "field_71467_ac", "rightClickDelayTimer");

            if (rightClickDelayTimerField != null) {
                rightClickDelayTimerField.setAccessible(true);
            }

            C01PacketChatMessageMessage = ReflectionHelper.findField(C01PacketChatMessage.class, "field_149440_a", "message");

            if (C01PacketChatMessageMessage != null) {
                C01PacketChatMessageMessage.setAccessible(true);
            }

            curBlockDamageMP = ReflectionHelper.findField(PlayerControllerMP.class, "field_78770_f", "curBlockDamageMP"); // fastmine and mining related stuff
            if (curBlockDamageMP != null) {
                curBlockDamageMP.setAccessible(true);
            }

            blockHitDelay = ReflectionHelper.findField(PlayerControllerMP.class, "field_78781_i", "blockHitDelay");
            if (blockHitDelay != null) {
                blockHitDelay.setAccessible(true);
            }

            fallDistance = ReflectionHelper.findField(Entity.class, "fallDistance", "field_70143_R");
            if (fallDistance != null) {
                fallDistance.setAccessible(true);
            }

            mcGuiInGame = ReflectionHelper.findField(GuiIngame.class, "mc", "field_73839_d");
            if (mcGuiInGame != null) {
                mcGuiInGame.setAccessible(true);
            }

            shaderResourceLocations = ReflectionHelper.findField(EntityRenderer.class, "shaderResourceLocations", "field_147712_ad");
            if (shaderResourceLocations != null) {
                shaderResourceLocations.setAccessible(true);
            }

            thirdPersonDistance = ReflectionHelper.findField(EntityRenderer.class, "thirdPersonDistance", "field_78490_B");
            if (thirdPersonDistance != null) {
                thirdPersonDistance.setAccessible(true);
            }

            alwaysEdible = ReflectionHelper.findField(ItemFood.class, "alwaysEdible", "field_77852_bZ");
            if (alwaysEdible != null) {
                alwaysEdible.setAccessible(true);
            }

            useShader = ReflectionHelper.findField(EntityRenderer.class, "useShader", "field_175083_ad");
            if (useShader != null) {
                useShader.setAccessible(true);
            }

            shaderIndex = ReflectionHelper.findField(EntityRenderer.class, "field_147713_ae", "shaderIndex"); // for shaders
            if (shaderIndex != null) {
                shaderIndex.setAccessible(true);
            }

            inGround = ReflectionHelper.findField(EntityArrow.class, "field_70254_i", "inGround"); // for indicators
            if (inGround != null) {
                inGround.setAccessible(true);
            }

            itemInUseCount = ReflectionHelper.findField(EntityPlayer.class, "field_71072_f", "itemInUseCount"); // for fake block
            if (itemInUseCount != null) {
                itemInUseCount.setAccessible(true);
            }

            S08PacketPlayerPosLookYaw = ReflectionHelper.findField(S08PacketPlayerPosLook.class, "field_148936_d", "yaw");
            if (S08PacketPlayerPosLookYaw != null) {
                S08PacketPlayerPosLookYaw.setAccessible(true);
            }

            S08PacketPlayerPosLookPitch = ReflectionHelper.findField(S08PacketPlayerPosLook.class, "field_148937_e", "pitch");
            if (S08PacketPlayerPosLookPitch != null) {
                S08PacketPlayerPosLookPitch.setAccessible(true);
            }

            C02PacketUseEntityEntityId = ReflectionHelper.findField(C02PacketUseEntity.class, "entityId", "field_149567_a");
            if (C02PacketUseEntityEntityId != null) {
                C02PacketUseEntityEntityId.setAccessible(true);
            }

            bookContents = ReflectionHelper.findField(GuiScreenBook.class, "field_175386_A");
            if (bookContents != null) {
                bookContents.setAccessible(true);
            }

            for (Class clazz : containerClasses) {
                for (Field field : clazz.getDeclaredFields()) {
                    addToMap(clazz, field);
                }
            }

        } catch (Exception var2) {
            System.out.println("There was an error, relaunch the game.");
            var2.printStackTrace();
            sendMessage = true;
        }
    }

    public static void setKeyBindings() {
        for (KeyBinding keyBind : Minecraft.getMinecraft().gameSettings.keyBindings) {
            String keyName = keyBind.getKeyDescription().replaceFirst("key\\.", "");
            keybinds.put(keyName, keyBind);
        }
    }

    public static void getMethods() {
        try {
            try {
                rightClickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("func_147121_ag");
            } catch (NoSuchMethodException var4) {
                try {
                    rightClickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("rightClickMouse");
                } catch (NoSuchMethodException var3) {
                }
            }

            if (rightClickMouse != null) {
                rightClickMouse.setAccessible(true);
            }

            getFOVModifier = ReflectionHelper.findMethod(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, new String[]{"func_78481_a", "getFOVModifier"}, float.class, boolean.class);

            if (getFOVModifier != null) {
                getFOVModifier.setAccessible(true);
            }

            loadShader = ReflectionHelper.findMethod(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, new String[]{"func_175069_a", "loadShader"}, ResourceLocation.class);

            if (loadShader != null) {
                loadShader.setAccessible(true);
            }

            try {
                clickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("clickMouse");
            } catch (NoSuchMethodException var4) {
                try {
                    clickMouse = Minecraft.getMinecraft().getClass().getDeclaredMethod("func_147116_af");
                } catch (NoSuchMethodException var3) {
                }
            }

            if (clickMouse != null) {
                clickMouse.setAccessible(true);
            }

            setupCameraTransform = ReflectionHelper.findMethod(EntityRenderer.class, Minecraft.getMinecraft().entityRenderer, new String[]{"func_78479_a", "setupCameraTransform"}, float.class, int.class);

            if (setupCameraTransform != null) {
                setupCameraTransform.setAccessible(true);
            }
        }
        catch (Exception e) {
            System.out.println("There was an error, relaunch the game.");
            e.printStackTrace();
            sendMessage = true;
        }
    }

    public static void setButton(int button, boolean state) {
        if (Reflection.button != null && buttonstate != null && buttons != null) {
            MouseEvent m = new MouseEvent();
            try {
                Reflection.button.setAccessible(true);
                Reflection.button.set(m, button);
                buttonstate.setAccessible(true);
                buttonstate.set(m, state);
                MinecraftForge.EVENT_BUS.post(m);
                buttons.setAccessible(true);
                ByteBuffer bf = (ByteBuffer) buttons.get(null);
                buttons.setAccessible(false);
                bf.put(button, (byte) (state ? 1 : 0));
            }
            catch (IllegalAccessException var4) {
            }
        }
    }

    private static void addToMap(Class clazz, Field field) {
        if (field == null || field.getType() != IInventory.class) {
            return;
        }
        field = ReflectionHelper.findField(clazz, field.getName());
        if (field == null) {
            return;
        }
        field.setAccessible(true);
        containerInventoryPlayer.put(clazz, field);
    }

    public static void rightClick() {
        try {
            Reflection.rightClickMouse.invoke(Minecraft.getMinecraft());
        }
        catch (InvocationTargetException ex) {}
        catch (IllegalAccessException ex2) {}
    }

    public static void clickMouse() {
        if (clickMouse != null) {
            try {
                clickMouse.invoke(Minecraft.getMinecraft());
            }
            catch (InvocationTargetException ex) {}
            catch (IllegalAccessException ex2) {}
        }
    }

    public static boolean setItemInUse(boolean blocking) {
        try {
            itemInUseCount.set(Minecraft.getMinecraft().thePlayer, blocking ? 1 : 0);
        }
        catch (Exception e) {
            e.printStackTrace();
            Utils.sendMessage("§cFailed to set block state client-side.");
            return false;
        }
        return blocking;
    }

    public static void setItemInUseCount(int count) {
        try {
            itemInUseCount.set(Minecraft.getMinecraft().thePlayer, count);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean setupCameraTransform(EntityRenderer entityRenderer, float partialTicks, int eyeIndex) {
        try {
            if (setupCameraTransform == null) {
                return false;
            }
            setupCameraTransform.invoke(entityRenderer, partialTicks, eyeIndex);
            return true;
        }
        catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }
}
