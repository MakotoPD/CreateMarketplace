package pl.makoto.createmarketplace.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.block.ServerVendorBlock;

public class BlockRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreateMarketplace.MODID);

    public static final DeferredBlock<ServerVendorBlock> SERVER_VENDOR = BLOCKS.register(
            "server_vendor",
            () -> new ServerVendorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F)
                    .sound(SoundType.COPPER)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
            )
    );
}
