package com.example.targetassist;

import net.minecraftforge.fml.common.Mod;

@Mod(TargetAssistMod.MODID)
public class TargetAssistMod {
    public static final String MODID = "targetassist";

    public TargetAssistMod() {
        Settings.load();
    }
}
