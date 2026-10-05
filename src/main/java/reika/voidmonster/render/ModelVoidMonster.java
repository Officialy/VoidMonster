/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.voidmonster.render;

import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import org.joml.Quaternionf;

/** Literal 1.7.10 ModelEnderCrystal transforms, expressed with the native 26.3 model parts. */
public final class ModelVoidMonster extends EndCrystalModel {
    private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
    public ModelVoidMonster(ModelPart root) { super(root); }
    @Override public void setupAnim(EndCrystalRenderState state) {
        root().getAllParts().forEach(ModelPart::resetPose);
        base.visible = false;
        State crystal = (State)state;
        outerGlass.y = 16 * (0.8F + crystal.bob);
        // The old model applies 0.875 twice cumulatively; the native cube mesh
        // already has the squared scale, so restore the relative second scale.
        cube.xScale = cube.yScale = cube.zScale = 0.875F;
        float rotation = crystal.rotation;
        outerGlass.rotateBy(Axis.YP.rotationDegrees(rotation).rotateAxis((float)(Math.PI / 3), SIN_45, 0, SIN_45));
        innerGlass.rotateBy(new Quaternionf().setAngleAxis((float)(Math.PI / 3), SIN_45, 0, SIN_45)
                .rotateY((float)Math.toRadians(rotation)));
        cube.rotateBy(new Quaternionf().setAngleAxis((float)(Math.PI / 3), SIN_45, 0, SIN_45)
                .rotateY((float)Math.toRadians(rotation)));
    }
    public static final class State extends EndCrystalRenderState {
        public float rotation;
        public float bob;
    }
}
