package fi.dy.masa.malilib.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.client.gui.inventory.GuiContainer.class)
public interface IMixinGuiContainer
{
    @Accessor("hoveredSlot")
    net.minecraft.inventory.Slot getHoveredSlot();

    @Accessor("guiLeft")
    int getGuiPosX();

    @Accessor("guiTop")
    int getGuiPosY();

    @Accessor("xSize")
    int getGuiSizeX();

    @Accessor("ySize")
    int getGuiSizeY();
}
