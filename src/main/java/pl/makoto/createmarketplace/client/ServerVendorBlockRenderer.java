package pl.makoto.createmarketplace.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.joml.Matrix4f;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;

/**
 * Renderuje na bloku Server Vendor: sprzedawany item (środek blatu, obracający się)
 * oraz ikonę ceny kupna z napisem ilościowym (×N) obok.
 *
 * Pozycje 3D są w block-space (0..1). Rotacja blockstate (FACING) jest
 * stosowana wokół środka bloku, żeby itemy zawsze były na tych samych
 * fragmentach modelu niezależnie od orientacji.
 */
public class ServerVendorBlockRenderer implements BlockEntityRenderer<ServerVendorBlockEntity> {

    public ServerVendorBlockRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ServerVendorBlockEntity be, float partialTicks, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);

        // Sprzedawany item — środek blatu, obraca się.
        ItemStack trade = be.getTradeItem();
        renderFloatingItem(be, trade, pose, buffers, packedLight, packedOverlay,
                facing, 0.5f, 0.9f, 0.5f, 0.7f, true, partialTicks);

        // Napis nad sprzedawanym itemem: nazwa + ilość. Billboard — zawsze do gracza.
        if (!trade.isEmpty()) {
            String label = trade.getHoverName().getString() + " x" + trade.getCount();
            renderBillboardText(label, pose, buffers, packedLight,
                    facing, 0.5f, 1.30f, 0.5f);
        }

        // Cena kupna — ikona po lewej stronie z przodu blatu.
        ItemStack buy = be.getBuyPrice();
        renderFloatingItem(be, buy, pose, buffers, packedLight, packedOverlay,
                facing, 0.5f, 0.7f, 0.04f, 0.5f, false, partialTicks);

        // Napis "×N" obok ikony ceny — informuje ile sztuk potrzeba na 1 transakcję.
        if (!buy.isEmpty()) {
            String text = "x" + buy.getCount();
            renderStaticText(text, pose, buffers, packedLight,
                    facing, 0.5f, 0.7f, 0.04f);
        }
    }

    private void renderFloatingItem(ServerVendorBlockEntity be, ItemStack stack, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay, Direction facing,
                                     float x, float y, float z, float scale, boolean spin, float partialTicks) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        applyBlockRotation(pose, facing);
        pose.translate(x, y, z);
        if (spin) {
            float a = (System.currentTimeMillis() % 36000L) / 100.0f;
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(a));
        }
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.GROUND, light, overlay, pose, buffers, be.getLevel(), 0);
        pose.popPose();
    }

    /**
     * Rysuje tekst statycznie zorientowany — leży płasko na froncie bloku
     * (po stronie {@code FACING}), nie obraca się za graczem. Pozycja w block-space (0..1).
     *
     * Tekst jest przesunięty lekko w stronę {@code -Z lokalne} (na zewnątrz frontu)
     * żeby nie z-fightować z geometrią modelu.
     */
    private void renderStaticText(String text, PoseStack pose, MultiBufferSource buffers, int light,
                                   Direction facing, float x, float y, float z) {
        pose.pushPose();
        applyBlockRotation(pose, facing);
        pose.translate(x, y, z);
        // Obrót 180° wokół Y żeby normal tekstu wskazywał -Z (na zewnątrz frontu bloku, do gracza).
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
        // Skala: -X koryguje lustrzaną kolejność po rotacji 180°, -Y żeby tekst nie był do góry nogami.
        pose.scale(-0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = pose.last().pose();
        int textWidth = font.width(text);
        font.drawInBatch(text, -textWidth / 2.0f, 0, 0xFFFFFFFF, true,
                matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }

    /**
     * Rysuje tekst billboardowy — zawsze patrzy w stronę kamery, niezależnie od
     * orientacji bloku. Pozycja w block-space (0..1).
     */
    private void renderBillboardText(String text, PoseStack pose, MultiBufferSource buffers, int light,
                                      Direction facing, float x, float y, float z) {
        pose.pushPose();
        applyBlockRotation(pose, facing);
        pose.translate(x, y, z);
        // Billboard: orientuj płaszczyznę tekstu w stronę kamery.
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        // Skala z negatywnym X i Y — `cameraOrientation` daje układ "z kamery na świat"
        // który wymaga kompensacji żeby tekst nie był lustrzany ani do góry nogami.
        pose.scale(-0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = pose.last().pose();
        int textWidth = font.width(text);
        font.drawInBatch(text, -textWidth / 2.0f, 0, 0xFFFFFFFF, true,
                matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
        pose.popPose();
    }

    /** Wspólna rotacja zgodna z blockstate.json (north/east/south/west). */
    private static void applyBlockRotation(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        pose.translate(-0.5, -0.5, -0.5);
    }
}
