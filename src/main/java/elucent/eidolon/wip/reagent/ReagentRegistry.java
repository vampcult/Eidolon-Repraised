package elucent.eidolon.wip.reagent;

import elucent.eidolon.Eidolon;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ReagentRegistry {
    static final Map<ResourceLocation, Reagent> reagents = new HashMap<>();

    public static Reagent register(Reagent r) {
        reagents.put(r.getRegistryName(), r);
        return r;
    }

    public static Collection<Reagent> getReagents() {
        return reagents.values();
    }

    @Nullable
    public static Reagent find(ResourceLocation location) {
        return reagents.getOrDefault(location, null);
    }

    public static final Reagent
            STEAM = register(new SteamReagent(new ResourceLocation(Eidolon.MODID, "steam")));
    public static Reagent ESPRIT = register(new EspritReagent(new ResourceLocation(Eidolon.MODID, "esprit")));
    public static Reagent CRIMSOL = register(new CrimsolReagent(new ResourceLocation(Eidolon.MODID, "crimsol")));
}