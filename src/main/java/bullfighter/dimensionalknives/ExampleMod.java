package bullfighter.dimensionalknives;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Vanishable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.portal.Portal;

import java.util.Set;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("dimensionalknives");

	public static final RegistryKey<World> BeyondSpace = RegistryKey.of(RegistryKeys.WORLD, Identifier.of("dimensionalknives","beyond_space"));

	public final Set<RegistryKey<World>> DimensionalKniveWorlds = new HashSet<>();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		Registry.register(Registries.ENTITY_TYPE, new Identifier("dimensionalknives:dimensional_portal"), DimensionalPortal.ENTITY_TYPE);
		ServerLifecycleEvents.SERVER_STARTED.register(this::onServerStarted);
		
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(content -> {
			content.add(DimensionalKnife);
			content.add(AdvancedDimensionalKnife);
		});
	}

	private void onServerStarted(MinecraftServer server) {
		Registry<World> dimensionRegistry = server.getRegistryManager().get(RegistryKeys.WORLD);
		Set<RegistryKey<World>> dimensionKeys = dimensionRegistry.getKeys();
		for (RegistryKey<World> dimensionKey : dimensionKeys) {
			if (dimensionKey != BeyondSpace) {
				DimensionalKniveWorlds.add(dimensionKey);
			}
		}
	}

	public class DimensionalKniveItem extends Item implements Vanishable {
		private final boolean canSwapDimensions;

		private final Multimap<EntityAttribute, EntityAttributeModifier> attributeModifiers;

		public DimensionalKniveItem(Boolean canSwapDimensions, double attackDamage, double attackSpeed, Settings settings) {
			super(settings);
			this.canSwapDimensions = canSwapDimensions;
			ImmutableMultimap.Builder<EntityAttribute, EntityAttributeModifier> builder = ImmutableMultimap.builder();
      		builder.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Tool modifier", attackDamage, Operation.ADDITION));
      		builder.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Tool modifier", attackSpeed, Operation.ADDITION));
			this.attributeModifiers = builder.build();
		}

		@SuppressWarnings("unchecked")
		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
			ItemStack itemStack = user.getStackInHand(hand);
			if (user.isSneaking() && world.getRegistryKey() == BeyondSpace && canSwapDimensions) {
				if (itemStack.getOrCreateNbt().getInt("DimensionalKniveWorldIndex")+2 > DimensionalKniveWorlds.size()) {
					itemStack.getOrCreateNbt().putInt("DimensionalKniveWorldIndex", 0);
				} else {
					itemStack.getOrCreateNbt().putInt("DimensionalKniveWorldIndex", itemStack.getOrCreateNbt().getInt("DimensionalKniveWorldIndex")+1);
				}
				itemStack.getOrCreateNbt().putString("DimensionalKniveWorldNamespace", ((RegistryKey<World>)DimensionalKniveWorlds.toArray()[itemStack.getOrCreateNbt().getInt("DimensionalKniveWorldIndex")]).getValue().getNamespace());
				itemStack.getOrCreateNbt().putString("DimensionalKniveWorldPath", ((RegistryKey<World>)DimensionalKniveWorlds.toArray()[itemStack.getOrCreateNbt().getInt("DimensionalKniveWorldIndex")]).getValue().getPath());
				user.sendMessage(Text.of(itemStack.getOrCreateNbt().getInt("DimensionalKniveWorldIndex")+". "+ itemStack.getOrCreateNbt().getString("DimensionalKniveWorldNamespace") + ":" + itemStack.getOrCreateNbt().getString("DimensionalKniveWorldPath")), true);
			} else {
				Portal portal = DimensionalPortal.ENTITY_TYPE.create(world);

				Vec3d vec = user.getEyePos().add(user.getRotationVector().multiply(2));
				portal.setOriginPos(vec);
				portal.setDestination(vec);

				if (world.getRegistryKey() == BeyondSpace) {
					portal.setDestinationDimension(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(itemStack.getOrCreateNbt().getString("DimensionalKniveWorldNamespace"), itemStack.getOrCreateNbt().getString("DimensionalKniveWorldPath"))));
				} else {
					portal.setDestinationDimension(BeyondSpace);
				}

				// Calculate the forward vector from yaw and pitch
				Vec3d forward = user.getRotationVec(1.0F).normalize().multiply(-1);

				// Default up vector
				Vec3d up = new Vec3d(0, 1, 0);

				// Calculate the player's yaw in radians
				double yawRadians = Math.toRadians(user.getYaw());

				// Handle edge cases when the player is looking straight up or down
				if (Math.abs(forward.y) > 0.99) {
					// When looking straight up or down, use the player's yaw to determine axisW
					up = new Vec3d(-Math.sin(yawRadians), 0, Math.cos(yawRadians));
				}

				// Calculate the axis vectors
				Vec3d axisW = forward.crossProduct(up).normalize();
				Vec3d axisH = forward.crossProduct(axisW).normalize();

				// Set the portal orientation and size
				portal.setOrientationAndSize(
					axisW,  // axisW
					axisH,  // axisH
					0.01,   // width
					2       // height
				);

				Portal portalReverse = PortalAPI.createReversePortal(portal);
				
				portal.getWorld().spawnEntity(portal);
				portal.getDestinationWorld().spawnEntity(portalReverse);

				portal.setPortalSize(1, portal.getHeight(), portal.getThickness());
				portalReverse.setPortalSize(1, portal.getHeight(), portal.getThickness());

				/*
				new Object() {
					private int ticks = 0;
					public void startDelay(World world) {
					ServerTickEvents.END_SERVER_TICK.register((server) -> {
						this.ticks++;
						if (this.ticks == 200) {
							portal.setPortalSize(0.01, portal.getHeight(), portal.getThickness());
							portalReverse.setPortalSize(0.01, portal.getHeight(), portal.getThickness());
							new Object() {
								private int ticks = 0;
								public void startDelay(World world) {
								ServerTickEvents.END_SERVER_TICK.register((server) -> {
									this.ticks++;
									if (this.ticks == 60) {
										portal.kill();
										portalReverse.kill();
										return;
										}
									});
								}
							}.startDelay(world);
							return;
							}
						});
					}
				}.startDelay(world);
				*/

				if (user.getWorld().getRegistryKey() != BeyondSpace) {
					itemStack.getOrCreateNbt().putString("DimensionalKniveWorldNamespace", user.getWorld().getRegistryKey().getValue().getNamespace());
					itemStack.getOrCreateNbt().putString("DimensionalKniveWorldPath", user.getWorld().getRegistryKey().getValue().getPath());
				}

				itemStack.damage(1, user, (callback -> {}));
				
				world.playSound(vec.getX(), vec.getY(), vec.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1F, 1F, true);
				world.playSound(vec.getX(), vec.getY(), vec.getZ(), SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.PLAYERS, 0.75F, 1F, true);

				user.getItemCooldownManager().set(itemStack.getItem(), 4);
			}

			return TypedActionResult.success(itemStack, world.isClient());
		}

		public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(EquipmentSlot slot) {
     		return slot == EquipmentSlot.MAINHAND ? this.attributeModifiers : super.getAttributeModifiers(slot);
   		}

		public boolean canMine(BlockState state, World world, BlockPos pos, PlayerEntity miner) {
			return !miner.isCreative();
		}

		public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
			if (state.isOf(Blocks.COBWEB)) {
				return 15.0F;
			} else {
				return state.isIn(BlockTags.SWORD_EFFICIENT) ? 1.5F : 1.0F;
			}
		}

		public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			stack.damage(1, attacker, (e) -> {
				e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND);
			});
			return true;
		}

		public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
			if (state.getHardness(world, pos) != 0.0F) {
			   stack.damage(2, miner, (e) -> {
				  e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND);
			   });
			}
	  
			return true;
		 }
	  
		public boolean isSuitableFor(BlockState state) {
			return state.isOf(Blocks.COBWEB);
		}

	}

	public Item DimensionalKnife = Registry.register(Registries.ITEM, new Identifier("dimensionalknives","dimensional_knife"), new DimensionalKniveItem(false, 3, -1, new Item.Settings().maxDamage(250)));
	public Item AdvancedDimensionalKnife = Registry.register(Registries.ITEM, new Identifier("dimensionalknives","advanced_dimensional_knife"), new DimensionalKniveItem(true, 4, -0.8, new Item.Settings().maxDamage(750)));
}