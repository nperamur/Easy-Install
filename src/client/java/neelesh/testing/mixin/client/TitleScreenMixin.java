package neelesh.testing.mixin.client;


import neelesh.testing.ProjectBrowser;
import neelesh.testing.ProjectType;
import neelesh.testing.TestingClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TitleScreen.class)
//@Mixin(LivingEntity.class)
public class TitleScreenMixin extends Screen {
	//@Inject(at = @At("HEAD"), method = "takeKnockback")\
	/**
	 @Shadow
	 protected boolean jumping;

	 @Shadow
	 private int jumpingCooldown;

	 private int jumpCount = 0;


	 /**
	 @Inject(at = @At("HEAD"), method = "tickMovement()V")
	 public void init(CallbackInfo info) {
	 LivingEntity entity = (LivingEntity) (Object) this;
	 if (jumping && jumpingCooldown == 0 ) {
	 jump();

	 jumpCount++;
	 jumpingCooldown = 10;
	 }
	 if (entity.isOnGround()) {
	 jumpCount = 0;
	 }
	 }*/


	//@Inject (at = @At("RETURN"), method = "getJumpVelocity")
	//protected float init2(CallbackInfoReturnable<Float> info) {
	//return info.getReturnValue() * 2;
	//}
/**
 @Inject(at = @At("HEAD"), method = "fall(DZLnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;)V")
 public void init4(double heightDifference, boolean onGround, BlockState state, BlockPos landedPosition, CallbackInfo info) {
 Entity entity = (Entity) (Object) this;
 entity.slowMovement(state, Vec3d.unpackRgb(0));
 }


 //@Inject (at = @At("RETURN"), method = "getGravity")
 //protected double init3(CallbackInfoReturnable<Double> info) {
 //    return 0.01;
 //}


 @Inject (at = @At("RETURN"), method = "getScale", cancellable = true)
 protected void getScaleFactor(CallbackInfoReturnable<Float> info) {
 LivingEntity entity = (LivingEntity) (Object) this;
 if (entity.isPlayer()) {
 info.setReturnValue(info.getReturnValue()*0.3f);
 }
 else {
 info.setReturnValue(info.getReturnValue());
 }
 info.cancel();
 }




 @Shadow
 */
	/**
	 public void jump() {

	 }




	 @Shadow
	 private NativeImage image;
	 @Shadow
	 private NativeImageBackedTexture texture;
	 @Shadow
	 private MinecraftClient client;
	 @Shadow
	 private GameRenderer renderer;
	 @Shadow
	 private float flickerIntensity;
	 @Inject (at = @At("RETURN"), method = "<init>")

	 public void init(CallbackInfo info) {
	 for (int i = 0; i < 16; i++) {
	 for (int j = 0; j < 16; j++) {


	 this.image.setColor(j, i, 0xFF0000FF);
	 }
	 }
	 }
	 @Inject (at = @At("RETURN"), method = "update")
	 public void update(float delta, CallbackInfo info) {

	 this.client.getProfiler().push("lightTex");
	 ClientWorld clientWorld = this.client.world;
	 if (clientWorld != null) {
	 float f = clientWorld.getSkyBrightness(1.0F);
	 float g;
	 if (clientWorld.getLightningTicksLeft() > 0) {
	 g = 1.0F;
	 } else {
	 g = f * 0.95F + 0.05F;
	 }

	 float h = this.client.options.getDarknessEffectScale().getValue().floatValue();
	 float i = this.getDarknessFactor(delta) * h;
	 float j = this.getDarkness(this.client.player, i, delta) * h;
	 float k = this.client.player.getUnderwaterVisibility();
	 float l;
	 if (this.client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
	 l = GameRenderer.getNightVisionStrength(this.client.player, delta);
	 } else if (k > 0.0F && this.client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER)) {
	 l = k;
	 } else {
	 l = 0.0F;
	 }

	 Vector3f vector3f = new Vector3f(f, f, 1.0F).lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
	 float m = this.flickerIntensity + 1.5F;
	 Vector3f vector3f2 = new Vector3f();

	 for (int n = 0; n < 16; n++) {
	 for (int o = 0; o < 16; o++) {
	 float p = getBrightness(clientWorld.getDimension(), n) * g;
	 float q = getBrightness(clientWorld.getDimension(), o) * m;
	 float s = q * ((q * 0.6F + 0.4F) * 0.6F + 0.4F);
	 float t = q * (q * q * 0.6F + 0.4F);
	 vector3f2.set(q, s, t);
	 boolean bl = clientWorld.getDimensionEffects().shouldBrightenLighting();
	 if (bl) {
	 vector3f2.lerp(new Vector3f(0.99F, 1.12F, 1.0F), 0.25F);
	 clamp(vector3f2);
	 } else {
	 Vector3f vector3f3 = new Vector3f(vector3f).mul(p);
	 vector3f2.add(vector3f3);
	 vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
	 if (this.renderer.getSkyDarkness(delta) > 0.0F) {
	 float u = this.renderer.getSkyDarkness(delta);
	 Vector3f vector3f4 = new Vector3f(vector3f2).mul(0.7F, 0.6F, 0.6F);
	 vector3f2.lerp(vector3f4, u);
	 }
	 }

	 if (l > 0.0F) {
	 float v = Math.max(vector3f2.x(), Math.max(vector3f2.y(), vector3f2.z()));
	 if (v < 1.0F) {
	 float u = 1.0F / v;
	 Vector3f vector3f4 = new Vector3f(vector3f2).mul(u);
	 vector3f2.lerp(vector3f4, l);
	 }
	 }

	 if (!bl) {
	 if (j > 0.0F) {
	 vector3f2.add(-j, -j, -j);
	 }

	 clamp(vector3f2);
	 }

	 float v = this.client.options.getGamma().getValue().floatValue();
	 Vector3f vector3f5 = new Vector3f(this.easeOutQuart(vector3f2.x), this.easeOutQuart(vector3f2.y), this.easeOutQuart(vector3f2.z));
	 vector3f2.lerp(vector3f5, Math.max(0.0F, v - i));
	 vector3f2.lerp(new Vector3f(0.75F, 0.75F, 0.75F), 0.04F);
	 clamp(vector3f2);
	 vector3f2.mul(255.0F);
	 PlayerEntity playerEntity = clientWorld.getClosestPlayer(0, 0, 0, 1000000000 , false);
	 int x;
	 int y;
	 int z;
	 if (clientWorld.getLightLevel(LightType.BLOCK, playerEntity.getBlockPos()) > 1 || clientWorld.getDimension().equals(clientWorld.getRegistryManager().get(DimensionTypes.THE_NETHER.getRegistryRef()).get(DimensionTypes.THE_NETHER))) {
	 x = (int) (vector3f2.x() * 1F);
	 y = (int) (vector3f2.y() * 0.2F);
	 z = (int) (vector3f2.z() * 0.2F);
	 } else {
	 x = (int) (vector3f2.x() * 1F);
	 y = (int) (vector3f2.y() * 1F);
	 z = (int) (vector3f2.z() * 1F);
	 }

	 this.image.setColor(o, n, 0xFF000000 | z << 16 | y << 8 | x);
	 }
	 }
	 RenderSystem.setShaderColor(255, 10, 10, 255);

	 this.texture.upload();
	 this.client.getProfiler().pop();
	 }

	 }
	 @Shadow
	 private float getDarknessFactor(float delta) {
	 return 0;
	 }


	 @Shadow
	 private float getDarkness(LivingEntity entity, float factor, float delta) {
	 return 0;
	 }

	 @Shadow
	 private static void clamp(Vector3f vec) {

	 }

	 @Shadow
	 private float easeOutQuart(float x) {
	 return 0;
	 }

	 */

//
//	@Shadow
//	private ClientWorld world;
//
//	@Inject(method = "renderLayer", at = @At("INVOKE"))
//	private void beforeRenderLayer(RenderLayer layer, double x, double y, double z, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) throws IOException {
//
//		RenderSystem.setShaderColor(1.0f, 0.2f, 0.2f, 1.0f); // Full red for light sources
//
//
//	}
//
//
//	@Inject(method = "renderLayer", at = @At("TAIL"))
//	private void resetRenderLayerColor(RenderLayer layer, double x, double y, double z, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo info) {
//		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f); // Reset to white
//	}
	//@Shadow
	//protected <T extends Element & Selectable> T addSelectableChild(T child) {
		//return null; // This method will be implemented by the mixin framework
	//}

	private ProjectBrowser modBrowser = new ProjectBrowser(this, ProjectType.MOD);
	private ButtonWidget buttonWidget = new ButtonWidget.Builder(Text.of("Add Mods"), button -> {
		TestingClient.search("", ProjectType.MOD);
		MinecraftClient.getInstance().setScreen(modBrowser);
	}).build();

	protected TitleScreenMixin(Text title) {
		super(title);
	}


	@Inject(method = "render", at = @At("TAIL"))
	private void addCustomButton(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		buttonWidget.setHeight(15);
		buttonWidget.setWidth(80);
		buttonWidget.setPosition(175, height-15);
		buttonWidget.render(context, mouseX, mouseY, delta);
		this.addSelectableChild(buttonWidget);
	}






}






