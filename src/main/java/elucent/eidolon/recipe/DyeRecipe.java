package elucent.eidolon.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import elucent.eidolon.api.IDyeable;
import elucent.eidolon.registries.EidolonRecipes;
import elucent.eidolon.util.RecipeUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;

import static elucent.eidolon.util.RegistryUtil.getRegistryName;

public class DyeRecipe extends ShapelessRecipe {

    public DyeRecipe(ResourceLocation idIn, String groupIn, ItemStack recipeOutputIn, NonNullList<Ingredient> recipeItemsIn) {
        super(idIn, groupIn, CraftingBookCategory.MISC, recipeOutputIn, recipeItemsIn);
    }

    @Override
    public @NotNull ItemStack assemble(final @NotNull CraftingContainer inv, @NotNull RegistryAccess registry) {
        final ItemStack output = super.assemble(inv, registry); // Get the default output

        if (!output.isEmpty()) {
            for (int i = 0; i < inv.getContainerSize(); i++) { // For each slot in the crafting inventory,
                final ItemStack ingredient = inv.getItem(i); // Get the ingredient in the slot
                if (!ingredient.isEmpty() && ingredient.getItem() instanceof IDyeable) {
                    output.setTag(ingredient.getOrCreateTag().copy());
                }
            }
            for (int i = 0; i < inv.getContainerSize(); i++) { // For each slot in the crafting inventory,
                final ItemStack ingredient = inv.getItem(i); // Get the ingredient in the slot
                DyeColor color = DyeColor.getColor(ingredient);
                if (!ingredient.isEmpty() && color != null) {
                    if (output.getItem() instanceof IDyeable dyeable) {
                        dyeable.onDye(output, color);
                    }
                }
            }
        }

        return output; // Return the modified output
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return EidolonRecipes.DYE_RECIPE.get();
    }

    /*

    @Override
    public @NotNull RecipeType<?> getType() {
        return EidolonRecipes.DYE_TYPE.get();
    }
    */

    public static JsonElement asRecipe(Item item) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("type", "eidolon:dye");
        JsonArray ingredients = new JsonArray();
        JsonObject dyeObject = new JsonObject();
        dyeObject.addProperty("tag", Tags.Items.DYES.location().toString());
        ingredients.add(dyeObject);

        JsonObject input = new JsonObject();
        input.addProperty("item", getRegistryName(item).toString());
        ingredients.add(input);

        jsonobject.add("ingredients", ingredients);
        JsonObject itemObject = new JsonObject();
        itemObject.addProperty("item", getRegistryName(item).toString());
        jsonobject.add("result", itemObject);
        return jsonobject;
    }

    public static class Serializer implements RecipeSerializer<DyeRecipe> {
        @Override
        public @NotNull DyeRecipe fromJson(final @NotNull ResourceLocation recipeID, final @NotNull JsonObject json) {
            final String group = GsonHelper.getAsString(json, "group", "");
            final NonNullList<Ingredient> ingredients = RecipeUtil.parseShapeless(json);
            final ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);

            return new DyeRecipe(recipeID, group, result, ingredients);
        }

        @Override
        public DyeRecipe fromNetwork(final @NotNull ResourceLocation recipeID, final FriendlyByteBuf buffer) {
            final String group = buffer.readUtf(Short.MAX_VALUE);
            final int numIngredients = buffer.readVarInt();
            final NonNullList<Ingredient> ingredients = NonNullList.withSize(numIngredients, Ingredient.EMPTY);

            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));

            final ItemStack result = buffer.readItem();

            return new DyeRecipe(recipeID, group, result, ingredients);
        }

        @Override
        public void toNetwork(final FriendlyByteBuf buffer, final DyeRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeVarInt(recipe.getIngredients().size());

            for (final Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItem(recipe.getResultItem(RegistryAccess.EMPTY));
        }
    }
}
