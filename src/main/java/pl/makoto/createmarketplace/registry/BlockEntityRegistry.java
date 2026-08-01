package pl.makoto.createmarketplace.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateMarketplace.MODID);

    public static final RegistryObject<BlockEntityType<ServerVendorBlockEntity>> SERVER_VENDOR =
            BLOCK_ENTITIES.register("server_vendor",
                    () -> BlockEntityType.Builder.of(ServerVendorBlockEntity::new,
                            BlockRegistry.SERVER_VENDOR.get()).build(null));
}
