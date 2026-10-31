package com.example.targetassist;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEvents {
    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        e.register(Keys.OPEN_GUI);
        e.register(Keys.LOCK_ON);
        e.register(Keys.FREECAM);
        e.register(Keys.FULLBRIGHT);
    }
}
