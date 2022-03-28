package io.github.moehreag.axolotlclient.modules.sky;

import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This implementation of custom skies is based on the FabricSkyBoxes mod by AMereBagatelle
 * https://github.com/AMereBagatelle/FabricSkyBoxes
 **/

public class SkyboxManager {

    public static final double MINIMUM_ALPHA = 0.01;

    private final ArrayList<SkyboxInstance> skyboxes = new ArrayList<>();
    private final ArrayList<SkyboxInstance> active_skies = new ArrayList<>();
    private final Predicate<? super SkyboxInstance> renderPredicate = (skybox) -> !this.active_skies.contains(skybox) && skybox.getAlpha() >= MINIMUM_ALPHA;

    public void addSkybox(SkyboxInstance skybox){skyboxes.add(Objects.requireNonNull(skybox));}

    private static final SkyboxManager INSTANCE = new SkyboxManager();

    public void renderSkyboxes(){
        this.skyboxes.stream().filter(this.renderPredicate).forEach(this.active_skies::add);
        this.active_skies.forEach(SkyboxInstance::renderSkybox);
        this.active_skies.removeIf((skybox) -> skybox.getAlpha() <= MINIMUM_ALPHA);
    }

    public void clearSkyboxes() {
        skyboxes.clear();
        active_skies.clear();
    }

    public static SkyboxManager getInstance(){
        return INSTANCE;
    }


}
