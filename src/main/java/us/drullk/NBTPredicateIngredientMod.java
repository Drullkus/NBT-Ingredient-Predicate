package us.drullk;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.advancements.criterion.NBTPredicate;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod(NBTPredicateIngredientMod.MOD_ID)
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD, modid = NBTPredicateIngredientMod.MOD_ID)
public class NBTPredicateIngredientMod {
    final static String MOD_ID = "nbt_ingredient_predicate";
    private static final Logger LOGGER = LogManager.getLogger();

    public NBTPredicateIngredientMod() {
    }

    @SubscribeEvent
    public static void registerRecipeSerializers(final RegistryEvent.Register<IRecipeSerializer<?>> recipeEvent) {
        CraftingHelper.register(new ResourceLocation(NBTPredicateIngredientMod.MOD_ID, "nbt_includes"), NBTPredicateIngredient.Serializer.INSTANCE);

        LOGGER.info("Successfully added NBT Predicate Ingredient Deserializer");
    }

    public static final class NBTPredicateIngredient extends Ingredient {
        private final ItemStack stack;
        private final NBTPredicate predicate;

        public NBTPredicateIngredient(ItemStack stack) {
            super(Stream.of(new Ingredient.SingleItemList(stack)));

            this.stack = stack;

            CompoundNBT shareTag = stack.getShareTag();
            this.predicate = shareTag == null ? NBTPredicate.ANY : new NBTPredicate(shareTag);
        }

        @Override
        public boolean test(@Nullable ItemStack stack) {
            if (stack == null)
                return false;

            return this.stack.getItem() == stack.getItem() && this.stack.getDamageValue() == stack.getDamageValue() && this.predicate.matches(stack);
        }

        @Override
        public boolean isSimple() {
            return false;
        }

        @Override
        public IIngredientSerializer<? extends Ingredient> getSerializer() {
            return Serializer.INSTANCE;
        }

        @Override
        public JsonElement toJson() {
            JsonObject json = new JsonObject();

            json.addProperty("type", CraftingHelper.getID(NBTPredicateIngredient.Serializer.INSTANCE).toString());
            json.addProperty("item", this.stack.getItem().getRegistryName().toString());
            json.addProperty("count", this.stack.getCount());

            CompoundNBT shareTag = this.stack.getShareTag();
            if (shareTag != null) json.addProperty("nbt", shareTag.toString());

            return json;
        }

        public static final class Serializer implements IIngredientSerializer<NBTPredicateIngredient> {
            public static final NBTPredicateIngredient.Serializer INSTANCE = new NBTPredicateIngredient.Serializer();

            @Override
            public NBTPredicateIngredient parse(PacketBuffer buffer) {
                return new NBTPredicateIngredient(buffer.readItem());
            }

            @Override
            public NBTPredicateIngredient parse(JsonObject json) {
                ItemStack stack = CraftingHelper.getItemStack(json, true);

                return new NBTPredicateIngredient(stack);
            }

            @Override
            public void write(PacketBuffer buffer, NBTPredicateIngredient ingredient) {
                buffer.writeItem(ingredient.stack);
            }
        }
    }
}
