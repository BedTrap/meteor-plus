package me.seasnail.meteorplus.mixins;

import net.minecraft.util.crash.CrashReport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import me.seasnail.meteorplus.utils.IntermediaryDeobfuscator;

@Mixin(CrashReport.class)
public class CrashReportMixin {
    @Redirect(method = "asString", at = @At(value = "INVOKE", target = "Ljava/lang/StringBuilder;toString()Ljava/lang/String;"))
    public String toString(StringBuilder stringBuilder) {
        return IntermediaryDeobfuscator.vaugeMap(stringBuilder.toString()).replace('/', '.');
    }
}
