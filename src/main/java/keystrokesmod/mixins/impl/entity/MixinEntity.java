package keystrokesmod.mixins.impl.entity;

import keystrokesmod.event.StrafeEvent;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Entity.class, priority = 999)
public abstract class MixinEntity {
    @Shadow
    public double motionX;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;

    @ModifyVariable(method = "moveEntity", at = @At(value = "STORE", ordinal = 0), name = "flag")
    private boolean injectSafeWalk(boolean flag) {
        Entity entity = (Entity) (Object) this;
        Minecraft mc = Minecraft.getMinecraft();

        if (entity == mc.thePlayer && entity.onGround) {
            if (SafeWalk.canSafeWalk() || ModuleManager.scaffold.safewalk()) {
                return true;
            }
        }
        return flag;
    }

    @Overwrite
    public void moveFlying(float strafe, float forward, float friction) {
        StrafeEvent strafeEvent = new StrafeEvent(strafe, forward, friction, this.rotationYaw);
        if((Object) this == Minecraft.getMinecraft().thePlayer) {
            MinecraftForge.EVENT_BUS.post(strafeEvent);
        }

        strafe = strafeEvent.getStrafe();
        forward = strafeEvent.getForward();
        friction = strafeEvent.getFriction();
        float yaw = strafeEvent.getYaw();

        float f = (strafe * strafe) + (forward * forward);

        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) {
                f = 1.0F;
            }

            f = friction / f;
            strafe *= f;
            forward *= f;
            float f1 = MathHelper.sin(yaw * (float)Math.PI / 180.0F);
            float f2 = MathHelper.cos(yaw * (float)Math.PI / 180.0F);
            this.motionX += strafe * f2 - forward * f1;
            this.motionZ += forward * f2 + strafe * f1;
        }
    }
}