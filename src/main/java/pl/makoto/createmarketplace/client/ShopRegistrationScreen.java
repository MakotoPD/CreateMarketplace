package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.PublishShopPayload;

public class ShopRegistrationScreen extends Screen {
    private final BlockPos pos;
    private final ItemStack item;
    private final ItemStack currency;
    private final java.util.List<String> existingShops;

    private EditBox nameEditBox;

    public ShopRegistrationScreen(BlockPos pos, ItemStack item, ItemStack currency, java.util.List<String> existingShops) {
        super(Component.literal("Rejestracja Sklepu"));
        this.pos = pos;
        this.item = item;
        this.currency = currency;
        this.existingShops = existingShops;
    }

    @Override
    protected void init() {
        super.init();
        
        int boxWidth = 200;
        int boxHeight = 20;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameEditBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - 20, boxWidth, boxHeight, Component.literal("Nazwa sklepu"));
        this.nameEditBox.setMaxLength(50);
        this.addRenderableWidget(this.nameEditBox);

        this.addRenderableWidget(Button.builder(Component.literal("Opublikuj"), button -> {
            String shopName = this.nameEditBox.getValue();
            if (!shopName.trim().isEmpty() && this.minecraft != null && this.minecraft.player != null) {
                String ownerName = this.minecraft.player.getName().getString();
                MarketOffer offer = new MarketOffer(
                        this.minecraft.player.getUUID(),
                        ownerName,
                        shopName,
                        this.pos,
                        this.item,
                        this.currency,
                        System.currentTimeMillis()
                );
                PacketDistributor.sendToServer(new PublishShopPayload(offer));
                this.onClose();
            }
        }).bounds(centerX - 50, centerY + 10, 100, 20).build());

        if (!existingShops.isEmpty()) {
            int yOffset = centerY + 45;
            this.addRenderableWidget(new net.minecraft.client.gui.components.StringWidget(centerX - 100, yOffset - 12, 200, 10, Component.literal("§7Dodaj do istniejącego sklepu:"), this.font));
            
            int btnX = centerX - 100;
            for (int i = 0; i < Math.min(existingShops.size(), 4); i++) {
                String shopName = existingShops.get(i);
                this.addRenderableWidget(Button.builder(Component.literal(shopName), b -> {
                    this.nameEditBox.setValue(shopName);
                }).bounds(btnX, yOffset, 95, 20).build());
                
                if (i % 2 == 1) {
                    yOffset += 25;
                    btnX = centerX - 100;
                } else {
                    btnX += 105;
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 50, 0xFFFFFF);
        
        // Rysowanie przedmiotu
        int itemX = this.width / 2 - 8;
        int itemY = this.height / 2 - 80;
        guiGraphics.renderItem(this.item, itemX, itemY);
        guiGraphics.renderItemDecorations(this.font, this.item, itemX, itemY);
        
        // Tooltip przedmiotu po najechaniu
        if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
            guiGraphics.renderTooltip(this.font, this.item, mouseX, mouseY);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
