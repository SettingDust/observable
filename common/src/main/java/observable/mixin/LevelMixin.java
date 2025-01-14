package observable.mixin;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import observable.Observable;
import observable.Props;
import observable.server.Profiler;
import observable.server.TaggedSampler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;

@Mixin(Level.class)
public class LevelMixin {
    @Redirect(method = "tickBlockEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    public final void redirectTick(TickingBlockEntity blockEntity) {
        if (Props.notProcessing) blockEntity.tick();
        else {
            if (Props.blockEntityDepth < 0) Props.blockEntityDepth = Thread.currentThread().getStackTrace().length - 1;
            if ((Object)this instanceof ServerLevel) {
                Profiler.TimingData data = Observable.INSTANCE.getPROFILER().processBlockEntity(blockEntity, (Level)(Object)this);
                Props.currentTarget.set(data);
                long start = System.nanoTime();
                blockEntity.tick();
                data.setTime(System.nanoTime() - start + data.getTime());
                Props.currentTarget.set(null);
                data.setTicks(data.getTicks() + 1);
            } else {
                blockEntity.tick();
            }
        }
    }
}
