package me.mss1r.siegeworks.config;

//? if <1.21 {
/*import net.minecraftforge.common.ForgeConfigSpec;
*///?} else {
import net.neoforged.neoforge.common.ModConfigSpec;
//?}

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SiegeworksConfigSpec {
    public static final int FORMAT_VERSION = 1;

    //? if <1.21 {
    /*private final ForgeConfigSpec delegate;

    private SiegeworksConfigSpec(ForgeConfigSpec delegate) {
        this.delegate = delegate;
    }

    public ForgeConfigSpec unwrap() {
        return delegate;
    }
    *///?} else {
    private final ModConfigSpec delegate;

    private SiegeworksConfigSpec(ModConfigSpec delegate) {
        this.delegate = delegate;
    }

    public ModConfigSpec unwrap() {
        return delegate;
    }
    //?}

    public boolean wraps(Object spec) {
        return delegate == spec;
    }

    public static final class Builder {
        //? if <1.21 {
        /*private final ForgeConfigSpec.Builder delegate = new ForgeConfigSpec.Builder();
        *///?} else {
        private final ModConfigSpec.Builder delegate = new ModConfigSpec.Builder();
        //?}

        public Builder() {
            delegate.comment("Config layout version, not the mod version. Leave this unchanged.",
                            "Adding settings does not change the layout version.")
                    .defineInRange("configVersion", FORMAT_VERSION, 1, Integer.MAX_VALUE);
        }

        public Builder comment(String... comments) {
            delegate.comment(comments);
            return this;
        }

        public Builder push(String path) {
            delegate.push(path);
            return this;
        }

        public Builder pop() {
            delegate.pop();
            return this;
        }

        public Builder pop(int count) {
            delegate.pop(count);
            return this;
        }

        public BooleanValue define(String path, boolean defaultValue) {
            var value = delegate.define(path, defaultValue);
            return new BooleanValue(value::get, value::set);
        }

        public IntValue defineInRange(String path, int defaultValue, int minimum, int maximum) {
            var value = delegate.defineInRange(path, defaultValue, minimum, maximum);
            return new IntValue(value::get, value::set);
        }

        public DoubleValue defineInRange(String path, double defaultValue,
                                         double minimum, double maximum) {
            var value = delegate.defineInRange(path, defaultValue, minimum, maximum);
            return new DoubleValue(value::get, value::set);
        }

        public <E extends Enum<E>> EnumValue<E> defineEnum(String path, E defaultValue) {
            var value = delegate.defineEnum(path, defaultValue);
            return new EnumValue<>(value::get, value::set);
        }

        public SiegeworksConfigSpec build() {
            return new SiegeworksConfigSpec(delegate.build());
        }
    }

    public static final class BooleanValue extends Value<Boolean> {
        private BooleanValue(Supplier<Boolean> getter, Consumer<Boolean> setter) {
            super(getter, setter);
        }
    }

    public static final class IntValue extends Value<Integer> {
        private IntValue(Supplier<Integer> getter, Consumer<Integer> setter) {
            super(getter, setter);
        }
    }

    public static final class DoubleValue extends Value<Double> {
        private DoubleValue(Supplier<Double> getter, Consumer<Double> setter) {
            super(getter, setter);
        }
    }

    public static final class EnumValue<E extends Enum<E>> extends Value<E> {
        private EnumValue(Supplier<E> getter, Consumer<E> setter) {
            super(getter, setter);
        }
    }

    public abstract static class Value<T> {
        private final Supplier<T> getter;
        private final Consumer<T> setter;

        private Value(Supplier<T> getter, Consumer<T> setter) {
            this.getter = getter;
            this.setter = setter;
        }

        public T get() {
            return getter.get();
        }

        public void set(T value) {
            setter.accept(value);
        }
    }
}
