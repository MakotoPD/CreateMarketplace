package pl.makoto.createmarketplace.util;

import net.minecraft.world.item.ItemStack;
import java.util.Optional;

public class ShopScanner {
    
    public static ItemStack findItemStackRecursive(Object source, int depth) {
        if (source == null || depth < 0)
            return ItemStack.EMPTY;
        Class<?> type = source.getClass();
        while (type != null && type != Object.class) {
            for (java.lang.reflect.Field field : type.getDeclaredFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()))
                    continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    if (value instanceof ItemStack stack && !stack.isEmpty()) {
                        return stack.copy();
                    }
                    if (value != null && depth > 0 && !value.getClass().isPrimitive() && !(value instanceof String)
                            && !(value instanceof Number)) {
                        ItemStack found = findItemStackRecursive(value, depth - 1);
                        if (!found.isEmpty())
                            return found;
                    }
                } catch (Exception ignored) {}
            }
            type = type.getSuperclass();
        }
        return ItemStack.EMPTY;
    }

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
