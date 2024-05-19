package bullfighter.dimensionalknives;

import org.jetbrains.annotations.Nullable;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.DimensionEffects.SkyType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import qouteall.imm_ptl.core.render.PortalEntityRenderer;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(DimensionalPortal.ENTITY_TYPE, PortalEntityRenderer::new);
		DimensionRenderingRegistry.registerDimensionEffects(new Identifier("dimensionalknives", "beyond_space"), new DimensionEffects(Float.NaN, false, SkyType.END, true, false) {
	
			public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
				return color.multiply(0.15000000596046448);
			}
	
			public boolean useThickFog(int camX, int camY) {
				return false;
			}
	
			@Nullable
			public float[] getFogColorOverride(float skyAngle, float tickDelta) {
				return null;
			}
		});
	}
}