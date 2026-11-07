package me.mss1r.siegeworks.client;

import me.mss1r.siegeworks.menu.CastingMenu;
import me.mss1r.siegeworks.client.gui.GuiPalette;
import me.mss1r.siegeworks.platform.MinecraftVersionCompat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class CastingScreen extends AbstractContainerScreen<CastingMenu> {
    private static final ResourceLocation TEXTURE = MinecraftVersionCompat.id("siegeworks", "textures/gui/casting.png");
    private static final ResourceLocation COLORS = MinecraftVersionCompat.id("siegeworks", "gui/casting.properties");
    private int textColor = 0x404040;
    private int headingColor = 0x303030;
    private int countColor = 0xffffff;
    private Button cast, less, more;
    public CastingScreen(CastingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); imageHeight = 196; inventoryLabelY = 100;
    }
    @Override protected void init() {
        super.init();
        GuiPalette palette = GuiPalette.load(minecraft.getResourceManager(), COLORS);
        textColor = palette.color("text", 0x404040);
        headingColor = palette.color("heading", 0x303030);
        countColor = palette.color("count", 0xffffff);
        less = addRenderableWidget(Button.builder(Component.literal("−"), b -> send(0)).bounds(leftPos+76,topPos+24,20,20).build());
        more = addRenderableWidget(Button.builder(Component.literal("+"), b -> send(1)).bounds(leftPos+148,topPos+24,20,20).build());
        cast = addRenderableWidget(Button.builder(Component.translatable("gui.siegeworks.casting.cast"), b -> send(2)).bounds(leftPos+8,topPos+76,160,20).build());
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id); }
    @Override protected void containerTick() {
        super.containerTick(); cast.active=menu.canCast();less.active=menu.caliber()>5;more.active=menu.caliber()<490;
    }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        g.blit(TEXTURE,x,y,0,0,imageWidth,imageHeight,imageWidth,imageHeight);
        var recipe=menu.recipe();
        if(recipe!=null) {
            var output=recipe.output(menu.caliber());
            g.pose().pushPose();g.pose().translate(x+22,y+31,0);g.pose().scale(2,2,1);
            g.renderItem(output,0,0);g.pose().popPose();
            if(output.getCount()>1) {
                String count=Integer.toString(output.getCount());
                g.drawString(font,count,x+64-font.width(count),y+61,countColor,true);
            }
        }
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY) {
        g.drawString(font,title,titleLabelX,titleLabelY,headingColor,false);
        g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,textColor,false);
        var caliberLabel = Component.translatable("gui.siegeworks.casting.caliber",menu.caliber());
        g.drawString(font,caliberLabel,122-font.width(caliberLabel)/2,30,textColor,false);
        var recipe=menu.recipe();
        if(recipe!=null) {
            var batch=recipe.batch(menu.caliber());
            g.drawString(font,Component.translatable("gui.siegeworks.casting.cost",batch.cost()),76,49,textColor,false);
            g.drawString(font,Component.translatable("gui.siegeworks.casting.yield",batch.count()),76,61,textColor,false);
        }
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        //? if <1.21 {
        /*renderBackground(g);
        *///?}
        super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(isHovering(8,23,60,48,mouseX,mouseY) && menu.recipe()!=null) g.renderTooltip(font,menu.recipe().output(menu.caliber()),mouseX,mouseY);
    }
}
