package pl.makoto.createmarketplace.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.block.ServerVendorBlock;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, CreateMarketplace.MODID);

    public static final RegistryObject<ServerVendorBlock> SERVER_VENDOR = BLOCKS.register(
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
