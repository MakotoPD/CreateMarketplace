package pl.makoto.createmarketplace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreateMarketplace.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerVendorBlockEntity>> SERVER_VENDOR =
            BLOCK_ENTITIES.register("server_vendor",
                    () -> BlockEntityType.Builder.of(ServerVendorBlockEntity::new, BlockRegistry.SERVER_VENDOR.get()).build(null));
}
