package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.clickgui.components.impl.ModuleComponent;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.classes.*;
import keystrokesmod.script.classes.Vec3;
import keystrokesmod.script.packets.clientbound.SPacket;
import keystrokesmod.script.packets.serverbound.CPacket;
import keystrokesmod.script.packets.serverbound.PacketHandler;
import keystrokesmod.utility.*;
import keystrokesmod.utility.shader.BlurUtils;
import keystrokesmod.utility.shader.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.*;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.Packet;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ScriptDefaults {
    private static ExecutorService cachedExecutor;
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static final Bridge bridge = new Bridge();
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public static class client {
        public static boolean allowFlying() {
            return mc.thePlayer.capabilities.allowFlying;
        }

        public static void removePotionEffect(int id) {
            if (mc.thePlayer == null) {
                return;
            }
            mc.thePlayer.removePotionEffectClient(id);
        }

        public static int getUID() {
            return 4;
        }

        public static String getUser() {
            return "mic";
        }

        public static void addEnemy(String username) {
            Utils.addEnemy(username);
        }

        public static void addFriend(String username) {
            Utils.addFriend(username);
        }

        public static void async(final Runnable method) {
            if (cachedExecutor == null) {
                cachedExecutor = Executors.newCachedThreadPool();
            }
            cachedExecutor.execute(method);
        }

        public static int getFPS() {
            return Minecraft.getDebugFPS();
        }

        public static void chat(String message) {
            mc.thePlayer.sendChatMessage(message);
        }

        public static void print(String string) {
            Utils.sendRawMessage(string);
        }

        public static void print(Object object) {
            String s = String.valueOf(object);
            Utils.sendRawMessage(s);
        }

        public static boolean isDiagonal() {
            return Utils.isDiagonal(false);
        }

        public static boolean isHoldingWeapon() {
            return Utils.holdingWeapon();
        }

        public static void setTimer(float timer) {
            Utils.getTimer().timerSpeed = timer;
        }

        public static boolean isCreative() {
            return mc.thePlayer.capabilities.isCreativeMode;
        }

        public static void processPacket(SPacket packet) {
            packet.packet.processPacket(mc.getNetHandler());
        }

        public static void processPacketNoEvent(SPacket packet) {
            PacketUtils.receivePacketNoEvent(packet.packet);
        }

        public static boolean isFlying() {
            return mc.thePlayer.capabilities.isFlying;
        }

        public static void attack(Entity entity) {
            Utils.attackEntity(entity.entity, true, true);
        }

        public static boolean isSinglePlayer() {
            return mc.isSingleplayer();
        }

        public static boolean isSpectator() {
            return mc.thePlayer.isSpectator();
        }

        public static void setFlying(boolean flying) {
            mc.thePlayer.capabilities.isFlying = flying;
        }

        public static void setJump(boolean jump) {
            mc.thePlayer.movementInput.jump = jump;
        }

        public static void setJumping(boolean jump) {
            mc.thePlayer.setJumping(jump);
        }

        public static void setRenderArmPitch(float pitch) {
            mc.thePlayer.prevRenderArmPitch = pitch;
            mc.thePlayer.renderArmPitch = pitch;
        }

        public static float getRenderArmPitch() {
            return mc.thePlayer.renderArmPitch;
        }

        public static void setRenderArmYaw(float yaw) {
            mc.thePlayer.renderArmYaw = yaw;
        }

        public static float getRenderArmYaw() {
            return mc.thePlayer.renderArmYaw;
        }

        public static long getTotalMemory() {
            return Runtime.getRuntime().totalMemory();
        }

        public static long getFreeMemory() {
            return Runtime.getRuntime().freeMemory();
        }

        public static long getMaxMemory() {
            return Runtime.getRuntime().maxMemory();
        }

        public static List<String[]> getResourcePacks() {
            List<String[]> packs = new ArrayList<>();
            if (mc.getResourcePackRepository().getRepositoryEntries() == null || mc.getResourcePackRepository().getRepositoryEntries().isEmpty()) {
                packs.add(new String[] { mc.mcDefaultResourcePack.getPackName(), "" } );
            }
            else {
                for (ResourcePackRepository.Entry entry : mc.getResourcePackRepository().getRepositoryEntries()) {
                    packs.add(new String[] { entry.getResourcePackName(), entry.getTexturePackDescription() } );
                }
            }
            Collections.reverse(packs); // reverse it to match the correct order
            return packs;
        }

        public static void jump() {
            mc.thePlayer.jump();
        }

        public static boolean allowEditing() {
            if (mc.thePlayer == null || mc.thePlayer.capabilities == null) {
                return false;
            }
            return mc.thePlayer.capabilities.allowEdit;
        }

        public static void setItemInUseCount(int count) {
            Reflection.setItemInUseCount(count);
        }

        public static int getItemInUseCount() {
            return mc.thePlayer.getItemInUseCount();
        }

        public static int getItemInUseDuration() {
            return mc.thePlayer.getItemInUseDuration();
        }

        public static void log(String message) {
            System.out.println(message);
        }

        public static void setSneaking(boolean sneak) {
            mc.thePlayer.setSneaking(sneak);
        }

        public static void setSneak(boolean sneak) {
            mc.thePlayer.movementInput.sneak = sneak;
        }

        public static boolean isSneak() {
            return mc.thePlayer.movementInput.sneak;
        }

        public static Entity getPlayer() {
            if (mc == null || mc.thePlayer == null) {
                return null;
            }
            return Entity.convert(mc.thePlayer);
        }

        public static void removeEnemy(String username) {
            Utils.removeEnemy(username);
        }

        public static void removeFriend(String username) {
            Utils.removeFriend(username);
        }

        public static boolean isRiding() {
            return mc.thePlayer.isRiding();
        }

        public static Vec3 getMotion() {
            return new Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ);
        }

        public static void sleep(long ms) {
            try {
                Thread.sleep(ms);
            }
            catch (InterruptedException e) {
            }
        }

        public static void ping() {
            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
        }

        public static void playSound(String name, float volume, float pitch) {
            mc.thePlayer.playSound(name, volume, pitch);
        }

        public static boolean isMoving() {
            return Utils.isMoving();
        }

        public static boolean isJump() {
            return mc.thePlayer.movementInput.jump;
        }

        public static float getStrafe() {
            return mc.thePlayer.movementInput.moveStrafe;
        }

        public static void sleep(int ms) {
            try {
                Thread.sleep(ms);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static float getForward() {
            return mc.thePlayer.movementInput.moveForward;
        }

        public static void closeScreen() {
            if (mc.currentScreen instanceof ClickGui) {
                mc.displayGuiScreen(null);
                return;
            }
            mc.thePlayer.closeScreen();
        }

        public static String getScreen() {
            return mc.currentScreen == null ? "" : mc.currentScreen.getClass().getSimpleName();
        }

        public static float[] getRotationsToEntity(Entity entity) {
            return RotationUtils.getRotations(entity.entity);
        }

        public static void sendPacket(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 == null) {
                return;
            }
            mc.thePlayer.sendQueue.addToSendQueue(packet1);
        }

        public static void sendPacketNoEvent(CPacket packet) {
            Packet packet1 = PacketHandler.convertCPacket(packet);
            if (packet1 == null) {
                return;
            }
            PacketUtils.sendPacketNoEvent(packet1);
        }

        public static void dropItem(boolean dropStack) {
            mc.thePlayer.dropOneItem(dropStack);
        }

        public static void setMotion(double x, double y, double z) {
            mc.thePlayer.motionX = x;
            mc.thePlayer.motionY = y;
            mc.thePlayer.motionZ = z;
        }

        public static void setSpeed(double speed) {
            Utils.setSpeed(speed);
        }

        public static void setForward(float forward) {
            mc.thePlayer.movementInput.moveForward = forward;
        }

        public static void setStrafe(float strafe) {
            mc.thePlayer.movementInput.moveStrafe = strafe;
        }

        public static String getServerIP() {
            if (mc.getCurrentServerData() == null || mc.isSingleplayer()) {
                return "";
            }
            return mc.getCurrentServerData().serverIP;
        }

        public static int[] getDisplaySize() {
            final ScaledResolution scaledResolution = new ScaledResolution(mc);
            return new int[]{scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), scaledResolution.getScaleFactor()};
        }

        public float getServerDirection(PlayerState state) {
            return state.yaw;
        }

        public static Object[] raycastBlock(double distance) {
            return raycastBlock(distance, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true);
        }

        public static Object[] raycastBlock(double distance, float yaw, float pitch, boolean collisionCheck) {
            MovingObjectPosition hit = RotationUtils.rayCast(distance, yaw, pitch, collisionCheck);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                return null;
            }
            return new Object[]{Vec3.convert(hit.getBlockPos()), new Vec3(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord), hit.sideHit.name()};
        }

        public static Object[] raycastEntity(double distance) {
            return raycastEntity(distance, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        }

        public static Object[] raycastEntity(double distance, float yaw, float pitch) {
            MovingObjectPosition hit = RotationUtils.rayCast(distance, yaw, pitch, true);
            if (hit == null || hit.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY) {
                return null;
            }
            return new Object[]{Entity.convert(hit.entityHit), new Vec3(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord), mc.thePlayer.getDistanceSqToEntity(hit.entityHit)};
        }

        public static boolean placeBlock(Vec3 targetPos, String side, Vec3 hitVec) {
            return mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), Vec3.getBlockPos(targetPos), Utils.getEnum(EnumFacing.class, side), Vec3.getVec3(hitVec));
        }

        public static float[] getRotationsToBlock(Vec3 position) {
            return RotationUtils.getRotations(new BlockPos(position.x, position.y, position.z));
        }

        public static void setSprinting(boolean sprinting) {
            mc.thePlayer.setSprinting(sprinting);
        }

        public static void swing() {
            mc.thePlayer.swingItem();
        }

        public static long time() {
            return System.currentTimeMillis();
        }

        public static boolean isFriend(String username) {
            return Utils.isFriended(username);
        }

        public static boolean isEnemy(String username) {
            return Utils.isEnemy(username);
        }
    }

    public static class world {

        public static Block getBlockAt(int x, int y, int z) {
            net.minecraft.block.Block block = BlockUtils.getBlock(new BlockPos(x, y, z));
            if (block == null) {
                return new Block(Blocks.air, new BlockPos(x, y, z));
            }
            return new Block(block, new BlockPos(x, y, z));
        }

        public static Block getBlockAt(Vec3 pos) {
            net.minecraft.block.Block block = BlockUtils.getBlock(new BlockPos(pos.x, pos.y, pos.z));
            if (block == null) {
                return new Block(Blocks.air, new BlockPos(pos.x, pos.y, pos.z));
            }
            return new Block(block, new BlockPos(pos.x, pos.y, pos.z));
        }

        public static String getDimension() {
            if (mc.theWorld == null) {
                return "";
            }
            return mc.theWorld.provider.getDimensionName();
        }

        public static List<Entity> getEntities() {
            List<Entity> entities = new ArrayList<>();
            if (mc.theWorld == null) {
                return entities;
            }
            for (net.minecraft.entity.Entity entity : mc.theWorld.loadedEntityList) {
                entities.add(Entity.convert(entity));
            }
            return entities;
        }

        public static Entity getEntityById(int entityId) {
            if (mc.theWorld == null) {
                return null;
            }
            return Entity.convert(mc.theWorld.getEntityByID(entityId));
        }

        public static List<NetworkPlayer> getNetworkPlayers() {
            List<NetworkPlayer> entities = new ArrayList<>();
            for (NetworkPlayerInfo networkPlayerInfo : Utils.getTablist(false)) {
                entities.add(new NetworkPlayer(networkPlayerInfo));
            }
            return entities;
        }

        public static List<Entity> getPlayerEntities() {
            List<Entity> entities = new ArrayList<>();
            for (net.minecraft.entity.Entity entity : mc.theWorld.playerEntities) {
                entities.add(Entity.convert(entity));
            }
            return entities;
        }

        public static List<String> getScoreboard() {
            List<String> sidebarLines = Utils.getSidebarLines();
            if (sidebarLines.isEmpty()) {
                return null;
            }
            return sidebarLines;
        }

        public static Map<String, List<String>> getTeams() {
            Map<String, List<String>> teams = new HashMap<>();
            for (Team team : mc.theWorld.getScoreboard().getTeams()) {
                List<String> members = new ArrayList<>();
                for (String member : team.getMembershipCollection()) {
                    members.add(member);
                }
                teams.put(team.getRegisteredName(), members);
            }
            return teams;
        }

        public static List<TileEntity> getTileEntities() {
            List<TileEntity> tileEntities = new ArrayList<>();
            for (net.minecraft.tileentity.TileEntity entity : mc.theWorld.loadedTileEntityList) {
                tileEntities.add(new TileEntity(entity));
            }
            return tileEntities;
        }
    }

    public static class modules {
        private String superName;

        public modules(String superName) {
            this.superName = superName;
        }

        private Module getModule(String moduleName) {
            for (Module module : Raven.getModuleManager().getModules()) {
                if (module.getName().equals(moduleName)) {
                    return module;
                }
            }
            for (Module module : Raven.scriptManager.scripts.values()) {
                if (module.getName().equals(moduleName)) {
                    return module;
                }
            }
            return null;
        }

        private Module getScript(String name) {
            for (Module module : Raven.scriptManager.scripts.values()) {
                if (module.getName().equals(name)) {
                    return module;
                }
            }
            return null;
        }

        private Setting getSetting(Module module, String settingName) {
            if (module == null) {
                return null;
            }
            for (Setting setting : module.getSettings()) {
                if (setting.getName().equals(settingName)) {
                    return setting;
                }
            }
            return null;
        }

        public void enable(String moduleName) {
            if (getModule(moduleName) == null) {
                return;
            }
            getModule(moduleName).enable();
        }

        public void disable(String moduleName) {
            if (getModule(moduleName) == null) {
                return;
            }
            getModule(moduleName).disable();
        }

        public boolean isEnabled(String moduleName) {
            if (getModule(moduleName) == null) {
                return false;
            }
            return getModule(moduleName).isEnabled();
        }

        public Entity getKillAuraTarget() {
            if (KillAura.target == null) {
                return null;
            }
            return Entity.convert(KillAura.target);
        }

        public Map<String, Object> getSettings(String name) {
            Map<String, Object> settings = new HashMap<>();
            Module module = getModule(name);
            if (module == null) {
                return settings;
            }
            for (Setting setting : module.getSettings()) {
                if (setting instanceof SliderSetting) {
                    settings.put(setting.getName(), ((SliderSetting) setting).getInput());
                }
                else if (setting instanceof ButtonSetting) {
                    settings.put(setting.getName(), ((ButtonSetting) setting).isToggled());
                }
            }
            return settings;
        }

        public Map<String, List<String>> getCategories() {
            Map<String, List<String>> categories = new HashMap<>();
            for (CategoryComponent categoryComponent : ClickGui.categories) {
                List<String> modules = new ArrayList<>();
                for (ModuleComponent module : categoryComponent.modules) {
                    modules.add(module.mod.getName());
                }
                categories.put(categoryComponent.categoryName.name(), modules);
            }
            return categories;
        }

        public Vec3 getBedAuraPosition() {
            BlockPos blockPos = ModuleManager.bedAura.currentBlock;
            if (ModuleManager.bedAura == null || !ModuleManager.bedAura.isEnabled() || ModuleManager.bedAura.currentBlock == null) {
                return null;
            }
            return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }

        public boolean isScaffolding() {
            return ModuleManager.scaffold.isEnabled() && ModuleManager.scaffold.tower.isToggled();
        }

        public boolean isTowering() {
            return ModuleManager.tower.canTower();
        }

        public boolean isHidden(String moduleName) {
            Module module = getModule(moduleName);
            if (module != null) {
                return module.isHidden();
            }
            return false;
        }

        public float[] getBedAuraProgress() {
            if (ModuleManager.bedAura != null && ModuleManager.bedAura.isEnabled()) {
                return new float[]{ModuleManager.bedAura.breakProgress, ModuleManager.bedAura.vanillaProgress};
            }
            return new float[]{0, 0};
        }

        public void registerButton(String name, boolean defaultValue) {
            getScript(this.superName).registerSetting(new ButtonSetting(name, defaultValue));
        }

        public void registerSlider(String name, double defaultValue, double minimum, double maximum, double interval) {
            this.registerSlider(name, "", defaultValue, minimum, maximum, interval);
        }

        public void registerSlider(String name,  int defaultValue, String[] stringArray) {
            this.registerSlider(name, "", defaultValue, stringArray);
        }

        public void registerSlider(String name, String suffix, double defaultValue, double minimum, double maximum, double interval) {
            getScript(this.superName).registerSetting(new SliderSetting(name, defaultValue, minimum, maximum, interval));
        }

        public void registerSlider(String name, String suffix, int defaultValue, String[] stringArray) {
            getScript(this.superName).registerSetting(new SliderSetting(name, defaultValue, stringArray));
        }

        public void registerDescription(String description) {
            getScript(this.superName).registerSetting(new DescriptionSetting(description));
        }

        public boolean getButton(String moduleName, String name) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            if (setting == null) {
                return false;
            }
            boolean buttonState = setting.isToggled();
            return buttonState;
        }

        public double getSlider(String moduleName, String name) {
            SliderSetting setting = ((SliderSetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return 0;
            }
            double sliderValue = setting.getInput();
            return sliderValue;
        }

        public void setButton(String moduleName, String name, boolean value) {
            ButtonSetting setting = (ButtonSetting) getSetting(getModule(moduleName), name);
            if (setting == null) {
                return;
            }
            setting.setEnabled(value);
        }

        public void setSlider(String moduleName, String name, double value) {
            SliderSetting setting = ((SliderSetting) getSetting(getModule(moduleName), name));
            if (setting == null) {
                return;
            }
            setting.setValue(value);
        }
    }

    public static class gl {
        public static void alpha(boolean alpha) {
            if (alpha) {
                GlStateManager.enableAlpha();
            }
            else {
                GlStateManager.disableAlpha();
            }
        }

        public static void begin(int mode) {
            GL11.glBegin(mode);
        }

        public static void blend(boolean blend) {
            if (blend) {
                GlStateManager.enableBlend();
            }
            else {
                GlStateManager.disableBlend();
            }
        }

        public static void color(float r, float g, float b, float a) {
            GlStateManager.color(r, g, b, a);
        }

        public static void cull(boolean cull) {
            if (cull) {
                GlStateManager.enableCull();
            }
            else {
                GlStateManager.disableCull();
            }
        }

        public static void depth(boolean depth) {
            if (depth) {
                GlStateManager.enableDepth();
            }
            else {
                GlStateManager.disableDepth();
            }
        }

        public static void depthMask(boolean depthMask) {
            GlStateManager.depthMask(depthMask);
        }

        public static void disable(int cap) {
            GL11.glDisable(cap);
        }

        public static void disableItemLighting() {
            RenderHelper.disableStandardItemLighting();
        }

        public static void enable(int cap) {
            GL11.glEnable(cap);
        }

        public static void enableItemLighting(boolean gui) {
            if (gui) {
                RenderHelper.enableGUIStandardItemLighting();
            }
            else {
                RenderHelper.enableStandardItemLighting();
            }
        }

        public static void end() {
            GL11.glEnd();
        }

        public static void lighting(boolean lighting) {
            if (lighting) {
                GlStateManager.enableLighting();
            }
            else {
                GlStateManager.disableLighting();
            }
        }

        public static void lineSmooth(boolean lineSmooth) {
            setGLEnable(GL11.GL_LINE_SMOOTH, lineSmooth);
        }

        public static void lineWidth(float width) {
            GL11.glLineWidth(width);
        }

        public static void normal(float x, float y, float z) {
            GL11.glNormal3f(x, y, z);
        }

        public static void pop() {
            GL11.glPopMatrix();
        }

        public static void push() {
            GL11.glPushMatrix();
        }

        public static void rotate(float angle, float x, float y, float z) {
            GL11.glRotatef(angle, x, y, z);
        }

        public static void scale(float x, float y, float z) {
            GL11.glScalef(x, y, z);
        }

        public static void scissor(int x, int y, int width, int height) {
            GL11.glScissor(x, y, width, height);
        }

        public static void scissor(boolean scissor) {
            setGLEnable(GL11.GL_SCISSOR_TEST, scissor);
        }

        public static void texture2d(boolean texture2d) {
            if (texture2d) {
                GlStateManager.enableTexture2D();
            }
            else {
                GlStateManager.disableTexture2D();
            }
        }

        public static void translate(float x, float y, float z) {
            GL11.glTranslatef(x, y, z);
        }

        public static void vertex2(float x, float y) {
            GL11.glVertex2f(x, y);
        }

        public static void vertex3(float x, float y, float z) {
            GL11.glVertex3f(x, y, z);
        }

        private static void setGLEnable(int cap, boolean enable) {
            if (enable) {
                GL11.glEnable(cap);
            }
            else {
                GL11.glDisable(cap);
            }
        }
    }

    public static class render {
        private static final IntBuffer VIEWPORT = GLAllocation.createDirectIntBuffer(16);
        private static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);
        private static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
        private static final FloatBuffer SCREEN_COORDS = GLAllocation.createDirectFloatBuffer(3);


        public static void block(Vec3 position, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(position.x, position.y, position.z), color, outline, shade);
        }

        public static void block(int x, int y, int z, int color, boolean outline, boolean shade) {
            RenderUtils.renderBlock(new BlockPos(x, y, z), color, outline, shade);
        }

        public static void entity(Entity en, int color, float partialTicks, boolean outline, boolean shade) {
            net.minecraft.entity.Entity e = en.entity;
            double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * partialTicks - mc.getRenderManager().viewerPosX;
            double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * partialTicks - mc.getRenderManager().viewerPosY;
            double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * partialTicks - mc.getRenderManager().viewerPosZ;
            AxisAlignedBB bbox = e.getEntityBoundingBox().expand(0.1, 0.1, 0.1);
            AxisAlignedBB axis = new AxisAlignedBB(bbox.minX - e.posX + x, bbox.minY - e.posY + y, bbox.minZ - e.posZ + z, bbox.maxX - e.posX + x, bbox.maxY - e.posY + y, bbox.maxZ - e.posZ + z);
            GL11.glPushMatrix();
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glLineWidth(2.0f);
            float a = (color >> 24 & 0xFF) / 255.0f;
            float r = (color >> 16 & 0xFF) / 255.0f;
            float g = (color >> 8 & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            GL11.glColor4f(r, g, b, a);
            if (outline) {
                RenderGlobal.drawSelectionBoundingBox(axis);
            }
            if (shade) {
                RenderUtils.drawBoundingBox(axis, r, g, b);
            }
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDepthMask(true);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static void entityGui(Entity en, int x, int y, float mouseX, float mouseY, int scale) {
            if (!en.isLiving) {
                return;
            }
            GL11.glPushMatrix();
            GuiInventory.drawEntityOnScreen(x, y, scale, mouseX, mouseY, (EntityLivingBase) en.entity);
            GL11.glPopMatrix();
        }

        public static void resetEquippedProgress() {
            mc.getItemRenderer().resetEquippedProgress();
        }

        public static void tracer(Entity entity, float lineWidth, int color, float partialTicks) {
            RenderUtils.drawTracerLine(entity.entity, color, lineWidth, partialTicks);
        }

        public static void item(ItemStack item, float x, float y, float scale) {
            GlStateManager.pushMatrix();
            Reflection.setupCameraTransform(mc.entityRenderer, Utils.getTimer().renderPartialTicks, 0);
            mc.entityRenderer.setupOverlayRendering();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            RenderHelper.enableGUIStandardItemLighting();
            GlStateManager.disableBlend();
            GlStateManager.translate(x / scale, y / scale, 0);
            mc.getRenderItem().renderItemIntoGUI(item.itemStack, 0, 0);
            GlStateManager.enableBlend();
            RenderHelper.disableStandardItemLighting();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.popMatrix();
        }

        public static void image(Image image, float x, float y, float width, float height) {
            if (image == null || !image.isLoaded()) {
                return;
            }
            if (image.textureId == -1) {
                final DynamicTexture dynamicTexture = new DynamicTexture(image.bufferedImage);
                GL11.glTexParameteri(3553, 10240, 9728);
                dynamicTexture.updateDynamicTexture();
                image.textureId = dynamicTexture.getGlTextureId();
            }
            GlStateManager.pushMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(image.textureId);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glTexParameteri(3553, 10240, 9728);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
            worldrenderer.pos(x, (y + height), 0.0).tex(0.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((x + width), (y + height), 0.0).tex(1.0, 1.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos((x + width), y, 0.0).tex(1.0, 0.0).color(255, 255, 255, 255).endVertex();
            worldrenderer.pos(x, y, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        public static Vec3 worldToScreen(double x, double y, double z, int scaleFactor, float partialTicks) {
            x -= mc.getRenderManager().viewerPosX;
            y -= mc.getRenderManager().viewerPosY;
            z -= mc.getRenderManager().viewerPosZ;
            Reflection.setupCameraTransform(mc.entityRenderer, partialTicks, 0);
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
            GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION);
            GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT);
            if (GLU.gluProject((float)x, (float)y, (float)z, render.MODELVIEW, render.PROJECTION, render.VIEWPORT, render.SCREEN_COORDS)) {
                Vec3 vec = new Vec3(render.SCREEN_COORDS.get(0) / scaleFactor, (Display.getHeight() - render.SCREEN_COORDS.get(1)) / scaleFactor, render.SCREEN_COORDS.get(2));
                mc.entityRenderer.setupOverlayRendering();
                return vec;
            }
            return null;
        }

        public static void roundedRect(float startX, float startY, float endX, float endY, float radius, int color) {
            RoundedUtils.drawRoundedRectRise(startX, startY, Math.abs(startX - endX), Math.abs(startY - endY), radius, color);
        }

        public static double[] getRotations() {
            return new double[] { mc.getRenderManager().playerViewY, mc.getRenderManager().playerViewX };
        }

        public static double[] getCameraRotations() {
            return new double[] { Utils.getCameraYaw(), Utils.getCameraPitch() };
        }

        public static int getFontWidth(String text) {
            return mc.fontRendererObj.getStringWidth(text) + Utils.getBoldWidth(text);
        }

        public static int getFontHeight() {
            return mc.fontRendererObj.FONT_HEIGHT;
        }

        public static Vec3 getPosition() {
            net.minecraft.util.Vec3 position = Utils.getCameraPos(Utils.getTimer().renderPartialTicks);
            return new Vec3(position);
        }

        public static void text2d(String text, float x, float y, float scale, int color, boolean shadow) {
            if (scale != 1.0f) {
                GlStateManager.pushMatrix();
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(text, x / scale, y / scale, color, shadow);
            GlStateManager.disableBlend();
            if (scale != 1.0f) {
                GlStateManager.scale(1.0f, 1.0f, 1.0f);
                GlStateManager.popMatrix();
            }
        }

        public static void text3d(String text, float x, float y, float scale, int color, boolean shadow) {
            GlStateManager.pushMatrix();
            Reflection.setupCameraTransform(mc.entityRenderer, Utils.getTimer().renderPartialTicks, 0);
            mc.entityRenderer.setupOverlayRendering();
            if (scale != 1.0f) {
                GlStateManager.scale(scale, scale, scale);
            }
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            mc.fontRendererObj.drawString(text, x, y, color, shadow);
            GlStateManager.disableBlend();
            if (scale != 1.0f) {
                GlStateManager.scale(1.0f, 1.0f, 1.0f);
            }
            GlStateManager.popMatrix();
        }

        public static void rect(float startX, float startY, float endX, float endY, int color) {
            RenderUtils.drawRectangleGL(startX, startY, endX, endY, color);
        }

        public static void line2D(double startX, double startY, double endX, double endY, float lineWidth, int color) {
            GL11.glPushMatrix();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            RenderUtils.glColor(color);
            GL11.glLineWidth(lineWidth);
            GL11.glBegin(GL11.GL_LINES);
            GL11.glVertex2d(startX, startY);
            GL11.glVertex2d(endX, endY);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glPopMatrix();
        }

        public static void line3D(Vec3 pos1, Vec3 pos2, float lineWidth, int color) {
            line3D(pos1.x, pos1.y, pos1.z, pos2.x, pos2.y, pos2.z, lineWidth, color);
        }

        public static void line3D(final double startX, final double startY, final double startZ, double endX, double endY, double endZ, final float lineWidth, final int color) {
            endX -= mc.getRenderManager().viewerPosX;
            endY -= mc.getRenderManager().viewerPosY;
            endZ -= mc.getRenderManager().viewerPosZ;
            final float a = (color >> 24 & 0xFF) / 255.0f;
            final float r = (color >> 16 & 0xFF) / 255.0f;
            final float g = (color >> 8 & 0xFF) / 255.0f;
            final float b = (color & 0xFF) / 255.0f;
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glEnable(2848);
            GL11.glDisable(2929);
            GL11.glDisable(3553);
            GL11.glBlendFunc(770, 771);
            GL11.glLineWidth(lineWidth);
            GlStateManager.color(r, g, b, a);
            GL11.glBegin(2);
            GL11.glVertex3d(startX - mc.thePlayer.posX, startY - mc.thePlayer.posY, startZ - mc.thePlayer.posZ);
            GL11.glVertex3d(endX, endY, endZ);
            GL11.glEnd();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glEnable(3553);
            GL11.glEnable(2929);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glPopMatrix();
        }

        public static boolean isInView(Entity en) {
            return RenderUtils.isInViewFrustum(en.entity);
        }

        public static class blur {
            public static void prepare() {
                BlurUtils.prepareBlur();
            }

            public static void apply(final int passes, final float radius) {
                BlurUtils.blurEnd(passes, radius);
            }
        }

        public static class bloom {
            public static void prepare() {
                BlurUtils.prepareBloom();
            }

            public static void apply(final int passes, final float radius) {
                BlurUtils.bloomEnd(passes, radius);
            }
        }
    }

    public static class inventory {

        public static int getSlot() {
            return mc.thePlayer.inventory.currentItem;
        }

        public static void setSlot(int slot) {
            mc.thePlayer.inventory.currentItem = slot;
        }

        public static void click(int slot, int button, int mode) {
            mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, button, mode, mc.thePlayer);
        }

        public static List<String> getBookContents() {
            if (mc.currentScreen instanceof GuiScreenBook) {
                try {
                    List<String> contents = new ArrayList<>();
                    int max = Math.min(128 / mc.fontRendererObj.FONT_HEIGHT, ((List<IChatComponent>) Reflection.bookContents.get(mc.currentScreen)).size());
                    for (int line = 0; line < max; ++line) {
                        IChatComponent lineStr = ((List<IChatComponent>) Reflection.bookContents.get(mc.currentScreen)).get(line);
                        contents.add(lineStr.getUnformattedText());
                        Utils.sendMessage(lineStr.getUnformattedText());
                    }
                    if (!contents.isEmpty()) {
                        return contents;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        public static String getChest() {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) {
                    return "";
                }
                return chest.getLowerChestInventory().getDisplayName().getUnformattedText();
            }
            return "";
        }

        public static String getContainer() {
            if (mc.currentScreen instanceof GuiContainerCreative) {
                CreativeTabs creativetabs = CreativeTabs.creativeTabArray[((GuiContainerCreative) mc.currentScreen).getSelectedTabIndex()];
                if (creativetabs != null) {
                    return I18n.format(creativetabs.getTranslatedTabLabel());
                }
            }
            else if (mc.currentScreen != null) {
                try {
                    return ((IInventory) Reflection.containerInventoryPlayer.get(mc.currentScreen.getClass()).get(mc.currentScreen)).getDisplayName().getUnformattedText();
                } catch (Exception e) {
                }
            }
            return "";
        }

        public static int getSize() {
            return mc.thePlayer.inventory.getSizeInventory();
        }

        public static int getChestSize() {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest == null) {
                    return -1;
                }
                return chest.getLowerChestInventory().getSizeInventory();
            }
            return -1;
        }

        public static ItemStack getStackInSlot(int slot) {
            if (mc.thePlayer.inventory.getStackInSlot(slot) == null) {
                return null;
            }
            return new ItemStack(mc.thePlayer.inventory.getStackInSlot(slot), (byte) 0);
        }

        public static ItemStack getStackInChestSlot(int slot) {
            if (mc.thePlayer.openContainer instanceof ContainerChest) {
                ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
                if (chest.getLowerChestInventory().getStackInSlot(slot) == null) {
                    return null;
                }
                return new ItemStack(chest.getLowerChestInventory().getStackInSlot(slot), (byte) 0);
            }
            return null;
        }

        public static void open() {
            KeyBinding inventoryKey = mc.gameSettings.keyBindInventory;
            int originalKeyCode = inventoryKey.getKeyCode();

            if (originalKeyCode == 0) {
                inventoryKey.setKeyCode(13);
                KeyBinding.resetKeyBindingArrayAndHash();
            }

            KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), true);
            KeyBinding.onTick(inventoryKey.getKeyCode());
            KeyBinding.setKeyBindState(inventoryKey.getKeyCode(), false);

            if (originalKeyCode == 0) {
                inventoryKey.setKeyCode(0);
                KeyBinding.resetKeyBindingArrayAndHash();
            }
        }
    }

    public static class keybinds {
        public static int[] getMousePosition() {
            return new int[] { Mouse.getX(), Mouse.getY() };
        }

        public static boolean isPressed(final String key) {
            KeyBinding keyBind = Reflection.keybinds.get(key);
            return keyBind != null && keyBind.isKeyDown();
        }

        public static void setPressed(final String key, final boolean pressed) {
            KeyBinding keyBind = Reflection.keybinds.get(key);
            if (keyBind != null) {
                KeyBinding.setKeyBindState(keyBind.getKeyCode(), pressed);
                if (pressed) {
                    KeyBinding.onTick(keyBind.getKeyCode());
                }
            }
        }

        public static int getKeyCode(final String key) {
            final KeyBinding keyBind = Reflection.keybinds.get(key);
            if (keyBind != null) {
                return keyBind.getKeyCode();
            }
            return -1;
        }

        public static int getKeyIndex(String key) {
            return Keyboard.getKeyIndex(key);
        }

        public static boolean isMouseDown(int mouseButton) {
            return Mouse.isButtonDown(mouseButton);
        }

        public static boolean isKeyDown(int key) {
            return Keyboard.isKeyDown(key);
        }

        public static void rightClick() {
            Reflection.rightClick();
        }

        public static void leftClick() {
            Reflection.clickMouse();
        }
    }

    public static class util {
        public static final String colorSymbol = "§";

        public static String color(String message) {
            return Utils.formatColor(message);
        }

        public static String strip(String string) {
            return Utils.stripColor(string);
        }

        public static double round(double value, int decimals) {
            return Utils.round(value, decimals);
        }

        public static int randomInt(int min, int max) {
            return Utils.randomizeInt(min, max);
        }

        public static double randomDouble(double min, double max) {
            return Utils.randomizeDouble(min, max);
        }
    }
}
