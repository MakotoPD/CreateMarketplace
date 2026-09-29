package pl.makoto.createmarketplace.util;

import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public class ShopScanner {

    public static Optional<ItemStack> invokeMethodReturningItemStack(Object target, String methodName) {
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName);
            Object result = m.invoke(target);
            if (result instanceof ItemStack stack && !stack.isEmpty()) {
                return Optional.of(stack.copy());
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    /** Wywołuje bezargumentową metodę publiczną; pusty Optional gdy jej nie ma lub rzuciła. */
    public static Optional<Object> invoke(Object target, String methodName) {
        try {
            return Optional.ofNullable(target.getClass().getMethod(methodName).invoke(target));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /** Bezargumentowa metoda zwracająca boolean (np. {@code isCreativeVendor}). */
    public static boolean invokeBoolean(Object target, String methodName) {
        return invoke(target, methodName).orElse(null) instanceof Boolean b && b;
    }

    /**
     * Wywołuje metodę o podanej nazwie i jednym parametrze, przekazując {@code null}.
     * Używane dla {@code TableClothBlockEntity#getStockLevelForTrade(ShoppingList)} —
     * typu parametru nie znamy w czasie kompilacji, więc szukamy po nazwie i arności.
     */
    public static Optional<Object> invokeWithNullArg(Object target, String methodName) {
        try {
            for (java.lang.reflect.Method m : target.getClass().getMethods()) {
                if (m.getName().equals(methodName) && m.getParameterCount() == 1
                        && !m.getParameterTypes()[0].isPrimitive()) {
                    return Optional.ofNullable(m.invoke(target, new Object[]{null}));
                }
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public static ItemStack getCoinItemByName(String name, int count) {
        try {
            // Próbujemy znaleźć przedmiot monety w rejestrze po ID: numismatics:<name>
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("numismatics", name);
            net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item, count);
            }
        } catch (Exception ignored) {}
        return ItemStack.EMPTY;
    }
}
