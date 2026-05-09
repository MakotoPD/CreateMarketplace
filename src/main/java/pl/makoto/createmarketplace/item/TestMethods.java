package pl.makoto.createmarketplace.item;
import java.lang.reflect.Method;
public class TestMethods {
    public static void printMethods(Class<?> clazz) {
        System.out.println("Methods for " + clazz.getName() + ":");
        for (Method m : clazz.getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() != void.class) {
                System.out.println(m.getName() + " -> " + m.getReturnType().getSimpleName());
            }
        }
    }
}
