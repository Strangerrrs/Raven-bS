package keystrokesmod.mixins.impl.client;

import keystrokesmod.module.ModuleManager;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WorldInfo.class, priority = 999)
public abstract class MixinWorldInfo {
    @Shadow
    private long worldTime;

    @Overwrite
    public long getWorldTime() {
        if (ModuleManager.weather != null && ModuleManager.weather.isEnabled()) {
            return (long) (ModuleManager.weather.time.getInput() * 1000);
        }
        return worldTime;
    }

    @Inject(method = "isRaining", at = @At("RETURN"), cancellable = true)
    private void setPrecipitation(CallbackInfoReturnable<Boolean> clr) {
        if (ModuleManager.weather != null && ModuleManager.weather.isEnabled() && ModuleManager.weather.rain.isToggled()) {
            clr.setReturnValue(true);
        }
    }
}