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
                facing, 0.5f, 0.9f, 0.5f, 1.0f, true, partialTicks);

        // Napis nad sprzedawanym itemem: nazwa + ilość. Hologram — zawsze do gracza.
        // if (!trade.isEmpty()) {
        //     String label = trade.getHoverName().getString() + " x" + trade.getCount();
        //     renderBillboardText(label, pose, buffers, packedLight,
        //             facing, 0.5f, 1.40f, 0.5f);
        // }

        // Cena kupna — ikona po lewej stronie z przodu blatu.
        ItemStack buy = be.getBuyPrice();
        renderFloatingItem(be, buy, pose, buffers, packedLight, packedOverlay,
                facing, 0.7f, 0.3f, -0.03f, 0.5f, false, partialTicks);

        // Napis pod ikoną ceny — ile waluty potrzeba na 1 transakcję. Statyczny: zawsze
        // zwrócony w stronę, w którą patrzy blok (FACING), bez tła.
        // Dla monet Numismatics pokazujemy wartość w spursach (nie liczbę monet).
        if (!buy.isEmpty()) {
            renderFacingText(priceLabel(buy), pose, buffers, packedLight,
                    facing, 0.4f, 0.45f, -0.03f);
        }
    }

    /** Etykieta ceny w formacie "xN", gdzie N to ilość sztuk waluty (bez nazwy). */
    private static String priceLabel(ItemStack buy) {
        return "x" + buy.getCount();
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
     * Rysuje tekst billboardowy (hologram) — zawsze patrzy w stronę kamery, jak nazwa
     * encji. Pozycja w block-space (0..1).
     *
     * Pozycję liczymy ręcznie: offset względem środka bloku obracamy tą samą rotacją co
     * blockstate (żeby x/y/z trafiały zawsze na te same fragmenty modelu), a do macierzy
     * pozy wstawiamy już TYLKO translację — orientacja jest czystym billboardem
     * ({@code cameraOrientation}), bez żadnej rotacji bloku do "odkręcania".
     *
     * Styl jak nametag moba: biały tekst na półprzezroczystym ciemnym tle (parametr
     * {@code backgroundColor} w {@link Font#drawInBatch}). Renderujemy TYLKO w trybie
     * {@code NORMAL} (bez {@code SEE_THROUGH}), więc napis chowa się za geometrią — nie
     * prześwituje przez ściany.
     */
    private void renderBillboardText(String text, PoseStack pose, MultiBufferSource buffers, int light,
                                      Direction facing, float x, float y, float z) {
        // Pozycja: offset względem środka bloku obrócony rotacją blockstate (żeby x/y/z
        // trafiały zawsze na te same fragmenty modelu niezależnie od FACING).
        org.joml.Vector3f off = new org.joml.Vector3f(x - 0.5f, y - 0.5f, z - 0.5f);
        off.rotate(com.mojang.math.Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        float px = 0.5f + off.x(), py = 0.5f + off.y(), pz = 0.5f + off.z();

        // Billboard wokół osi Y: tekst obraca się poziomo za kamerą (jak nazwa moba,
        // tu uproszczone do yaw — napis zostaje pionowy). Obrót budujemy przez Axis.YP
        // z yaw kamery, bo surowy cameraOrientation() w tym BER-ze nie renderował tekstu.
        float camYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();

        // Tekst jest jednostronny (back-face culling). Rysujemy go DWUKROTNIE, obie kopie
        // obrócone o 180° — z dowolnej strony jedna jest zwrócona przodem (czytelna),
        // druga cullowana. Dzięki temu napis widać z każdej strony bloku.
        drawYawText(text, pose, buffers, light, px, py, pz, 180.0f - camYaw, true);
        drawYawText(text, pose, buffers, light, px, py, pz, 360.0f - camYaw, true);
    }

    /**
     * Rysuje napis statycznie zwrócony w stronę, w którą patrzy blok ({@code FACING}),
     * bez tła. Nie śledzi kamery — czytelny od frontu bloku (gdzie stoi klient).
     */
    private void renderFacingText(String text, PoseStack pose, MultiBufferSource buffers, int light,
                                   Direction facing, float x, float y, float z) {
        org.joml.Vector3f off = new org.joml.Vector3f(x - 0.5f, y - 0.5f, z - 0.5f);
        off.rotate(com.mojang.math.Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        // Yaw odpowiadający widzowi stojącemu od frontu bloku (zob. analiza billboardu).
        drawYawText(text, pose, buffers, light,
                0.5f + off.x(), 0.5f + off.y(), 0.5f + off.z(), 180.0f - facing.toYRot(), false);
    }

    private void drawYawText(String text, PoseStack pose, MultiBufferSource buffers, int light,
                             float px, float py, float pz, float yawDeg, boolean withBackground) {
        pose.pushPose();
        pose.translate(px, py, pz);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yawDeg));
        pose.scale(-0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        Matrix4f matrix = pose.last().pose();
        float xOff = -font.width(text) / 2.0f;
        // Opcjonalne ciemne tło-"pigułka" jak przy nazwach mobów. Tryb NORMAL → napis chowa się za blokami.
        int bgColor = 0;
        if (withBackground) {
            float bgOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25f);
            bgColor = (int) (bgOpacity * 255.0f) << 24;
        }
        font.drawInBatch(text, xOff, 0, 0xFFFFFFFF, false,
                matrix, buffers, Font.DisplayMode.NORMAL, bgColor, light);
        pose.popPose();
    }

    /** Wspólna rotacja zgodna z blockstate.json (north/east/south/west). */
    private static void applyBlockRotation(PoseStack pose, Direction facing) {
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f - facing.toYRot()));
        pose.translate(-0.5, -0.5, -0.5);
    }
}
