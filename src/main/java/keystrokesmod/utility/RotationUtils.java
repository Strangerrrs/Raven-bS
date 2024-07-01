package keystrokesmod.utility;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.module.impl.client.Settings;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

public class RotationUtils {
    public static final Minecraft mc = Minecraft.getMinecraft();
    public static float renderPitch;
    public static float prevRenderPitch;
    public static float renderYaw;
    public static float prevRenderYaw;

    public static void setRenderYaw(float yaw) {
        mc.thePlayer.rotationYawHead = yaw;
        if (Settings.rotateBody.isToggled() && Settings.fullBody.isToggled()) {
            mc.thePlayer.renderYawOffset = yaw;
        }
    }

    public static float[] getRotations(BlockPos blockPos, final float n, final float n2) {
        final float[] array = getRotations(blockPos);
        return fixRotation(array[0], array[1], n, n2);
    }

    public static float[] getRotations(final BlockPos blockPos) {
        final double n = blockPos.getX() + 0.45 - mc.thePlayer.posX;
        final double n2 = blockPos.getY() + 0.45 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double n3 = blockPos.getZ() + 0.45 - mc.thePlayer.posZ;
        return new float[] { mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n3, n) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clamp(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float)(-(Math.atan2(n2, MathHelper.sqrt_double(n * n + n3 * n3)) * 57.295780181884766)) - mc.thePlayer.rotationPitch)) };
    }

    public static float interpolateValue(float tickDelta, float old, float newFloat) {
        return old + (newFloat - old) * tickDelta;
    }

    public static float[] getRotations(Entity entity, final float n, final float n2) {
        final float[] array = getRotations(entity);
        if (array == null) {
            return null;
        }
        return fixRotation(array[0], array[1], n, n2);
    }

    public static double getDistanceSqToEntity(final Entity entity) {
        EntityPlayerSP player = mc.thePlayer;
        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(
                entity.getCollisionBorderSize(),
                entity.getCollisionBorderSize(),
                entity.getCollisionBorderSize()
        );

        double deltaX = player.posX - MathHelper.clamp_double(player.posX, bb.minX, bb.maxX);
        double deltaY = (player.posY + player.getEyeHeight()) - MathHelper.clamp_double(player.posY + player.getEyeHeight(), bb.minY, bb.maxY);
        double deltaZ = player.posZ - MathHelper.clamp_double(player.posZ, bb.minZ, bb.maxZ);

        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public static double distanceFromYaw(final Entity entity, final boolean b) {
        return Math.abs(MathHelper.wrapAngleTo180_double(i(entity.posX, entity.posZ) - ((b && PreMotionEvent.setRenderYaw()) ? RotationUtils.renderYaw : mc.thePlayer.rotationYaw)));
    }

    public static float i(final double n, final double n2) {
        return (float)(Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static boolean inRange(final BlockPos blockPos, final double n) {
        final float[] array = RotationUtils.getRotations(blockPos);
        final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        final float n2 = -array[0] * 0.017453292f;
        final float n3 = -array[1] * 0.017453292f;
        final float cos = MathHelper.cos(n2 - 3.1415927f);
        final float sin = MathHelper.sin(n2 - 3.1415927f);
        final float n4 = -MathHelper.cos(n3);
        final Vec3 vec3 = new Vec3(sin * n4, MathHelper.sin(n3), cos * n4);
        Block block = BlockUtils.getBlock(blockPos);
        IBlockState blockState = BlockUtils.getBlockState(blockPos);
        if (block != null && blockState != null) {
            AxisAlignedBB boundingBox = block.getCollisionBoundingBox(mc.theWorld, blockPos, blockState);
            if (boundingBox != null) {
                Vec3 targetVec = getPositionEyes.addVector(vec3.xCoord * n, vec3.yCoord * n, vec3.zCoord * n);
                MovingObjectPosition intercept = boundingBox.calculateIntercept(getPositionEyes, targetVec);
                if (intercept != null) {
                    return true;
                }
            }
        }
        return false;
    }

    public static float[] getRotations(final Entity entity) {
        if (entity == null) {
            return null;
        }

        EntityPlayerSP player = mc.thePlayer;
        AxisAlignedBB bb = entity.getEntityBoundingBox().expand(
                entity.getCollisionBorderSize(),
                entity.getCollisionBorderSize(),
                entity.getCollisionBorderSize()
        );

        double deltaX = player.posX - MathHelper.clamp_double(player.posX, bb.minX, bb.maxX);
        double deltaY = (player.posY + player.getEyeHeight()) - MathHelper.clamp_double(player.posY + player.getEyeHeight(), bb.minY, bb.maxY);
        double deltaZ = player.posZ - MathHelper.clamp_double(player.posZ, bb.minZ, bb.maxZ);

        // FIXME: this math looks horrible, probably inaccurate too, but it will have to do for now
        // adding onto above, this probably generates invalid values, someone please look into fixing this whenever possible
        return new float[] {mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float) (Math.atan2(deltaZ, deltaX) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clamp(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float) (-(Math.atan2(deltaY, MathHelper.sqrt_double(deltaX * deltaX + deltaZ * deltaZ)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f)};
    }

    public static float[] getRotationsPredicated(final Entity entity, final int ticks) {
        if (entity == null) {
            return null;
        }
        if (ticks == 0) {
            return getRotations(entity);
        }
        double posX = entity.posX;
        final double posY = entity.posY;
        double posZ = entity.posZ;
        final double n2 = posX - entity.lastTickPosX;
        final double n3 = posZ - entity.lastTickPosZ;
        for (int i = 0; i < ticks; ++i) {
            posX += n2;
            posZ += n3;
        }
        final double n4 = posX - mc.thePlayer.posX;
        double n5;
        if (entity instanceof EntityLivingBase) {
            n5 = posY + entity.getEyeHeight() * 0.9 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        else {
            n5 = (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        }
        final double n6 = posZ - mc.thePlayer.posZ;
        return new float[] { mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float((float)(Math.atan2(n6, n4) * 57.295780181884766) - 90.0f - mc.thePlayer.rotationYaw), clamp(mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float((float)(-(Math.atan2(n5, MathHelper.sqrt_double(n4 * n4 + n6 * n6)) * 57.295780181884766)) - mc.thePlayer.rotationPitch) + 3.0f) };
    }

    public static float clamp(final float n) {
        return MathHelper.clamp_float(n, -90.0f, 90.0f);
    }

    public static float[] fixRotation(float n, float n2, final float n3, final float n4) {
        float n5 = n - n3;
        final float abs = Math.abs(n5);
        final float n7 = n2 - n4;
        final float n8 = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        final double n9 = n8 * n8 * n8 * 1.2;
        final float n10 = (float) (Math.round((double) n5 / n9) * n9);
        final float n11 = (float) (Math.round((double) n7 / n9) * n9);
        n = n3 + n10;
        n2 = n4 + n11;
        if (abs >= 1.0f) {
            final int n12 = (int) Settings.randomYawFactor.getInput();
            if (n12 != 0) {
                final int n13 = n12 * 100 + Utils.randomizeInt(-30, 30);
                n += Utils.randomizeInt(-n13, n13) / 100.0;
            }
        } else if (abs <= 0.04) {
            n += ((abs > 0.0f) ? 0.01 : -0.01);
        }
        return new float[]{n, clamp(n2)};
    }

    public static float angle(final double n, final double n2) {
        return (float) (Math.atan2(n - mc.thePlayer.posX, n2 - mc.thePlayer.posZ) * 57.295780181884766 * -1.0);
    }

    public static MovingObjectPosition rayCast(final double distance, final float yaw, final float pitch) {
        final Vec3 getPositionEyes = mc.thePlayer.getPositionEyes(1.0f);
        final float n4 = -yaw * 0.017453292f;
        final float n5 = -pitch * 0.017453292f;
        final float cos = MathHelper.cos(n4 - 3.1415927f);
        final float sin = MathHelper.sin(n4 - 3.1415927f);
        final float n6 = -MathHelper.cos(n5);
        final Vec3 vec3 = new Vec3(sin * n6, MathHelper.sin(n5), cos * n6);
        return mc.theWorld.rayTraceBlocks(getPositionEyes, getPositionEyes.addVector(vec3.xCoord * distance, vec3.yCoord * distance, vec3.zCoord * distance), false, false, false);
    }

    public static Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3(f1 * f2, f3, f * f2);
    }
}
