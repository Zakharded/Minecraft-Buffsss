package com.example.targetassist;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Keys {
    public static final KeyMapping OPEN_GUI = new KeyMapping("key.targetassist.open",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "key.categories.targetassist");
    public static final KeyMapping LOCK_ON = new KeyMapping("key.targetassist.lock",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.targetassist");
    public static final KeyMapping FREECAM = new KeyMapping("key.targetassist.freecam",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.targetassist");
    public static final KeyMapping FULLBRIGHT = new KeyMapping("key.targetassist.fullbright",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.targetassist");
}
