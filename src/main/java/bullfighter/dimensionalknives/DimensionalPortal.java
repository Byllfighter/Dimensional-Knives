package bullfighter.dimensionalknives;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.portal.Portal;

public class DimensionalPortal extends Portal {
    public static final EntityType<DimensionalPortal> ENTITY_TYPE = Portal.createPortalEntityType(DimensionalPortal::new);
    public DimensionalPortal(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    private int age; // Field to store the age of the entity

    @Override
    public void tick() {
        super.tick();
        age++;
        if (age > 200) { // Despawn after 200 ticks (10 seconds)
            this.setPortalSize(0.001, this.getHeight(), this.getThickness());
            if (age > 250) { // Despawn after 200 ticks (10 seconds)
                this.discard();
            }
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Age")) {
            this.age = nbt.getInt("Age");
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Age", this.age);
    }
}