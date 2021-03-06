package com.farcr.swampexpansion.common.entity.goals;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.farcr.swampexpansion.common.entity.SlabfishEntity;
import com.farcr.swampexpansion.common.entity.SlabfishType;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.stats.Stats;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class SlabbyBreedGoal extends Goal {
   private static final EntityPredicate field_220689_d = (new EntityPredicate()).setDistance(8.0D).allowInvulnerable().allowFriendlyFire().setLineOfSiteRequired();
   protected final SlabfishEntity animal;
   private final Class<? extends SlabfishEntity> mateClass;
   protected final World world;
   protected SlabfishEntity targetMate;
   private int spawnBabyDelay;
   private final double moveSpeed;

   public SlabbyBreedGoal(SlabfishEntity animal, double speedIn) {
      this(animal, speedIn, animal.getClass());
   }

   public SlabbyBreedGoal(SlabfishEntity p_i47306_1_, double p_i47306_2_, Class<? extends SlabfishEntity> p_i47306_4_) {
      this.animal = p_i47306_1_;
      this.world = p_i47306_1_.world;
      this.mateClass = p_i47306_4_;
      this.moveSpeed = p_i47306_2_;
      this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
   }

   public boolean shouldExecute() {
      if (!this.animal.isInLove()) {
         return false;
      } else {
         this.targetMate = this.getNearbyMate();
         return this.targetMate != null;
      }
   }

   public boolean shouldContinueExecuting() {
      return this.targetMate.isAlive() && this.targetMate.isInLove() && this.spawnBabyDelay < 60;
   }

   public void resetTask() {
      this.targetMate = null;
      this.spawnBabyDelay = 0;
   }

   public void tick() {
      this.animal.getLookController().setLookPositionWithEntity(this.targetMate, 10.0F, (float)this.animal.getVerticalFaceSpeed());
      this.animal.getNavigator().tryMoveToEntityLiving(this.targetMate, this.moveSpeed);
      ++this.spawnBabyDelay;
      if (this.spawnBabyDelay >= 60 && this.animal.getDistanceSq(this.targetMate) < 9.0D) {
         this.spawnBaby();
      }

   }

   @Nullable
   private SlabfishEntity getNearbyMate() {
      List<SlabfishEntity> list = this.world.getTargettableEntitiesWithinAABB(this.mateClass, field_220689_d, this.animal, this.animal.getBoundingBox().grow(8.0D));
      double d0 = Double.MAX_VALUE;
      SlabfishEntity SlabfishEntity = null;

      for(SlabfishEntity SlabfishEntity1 : list) {
         if (this.animal.canMateWith(SlabfishEntity1) && this.animal.getDistanceSq(SlabfishEntity1) < d0) {
            SlabfishEntity = SlabfishEntity1;
            d0 = this.animal.getDistanceSq(SlabfishEntity1);
         }
      }

      return SlabfishEntity;
   }
   
   protected void spawnBaby() {
	   Random rand = new Random();
	   SlabfishEntity slabby = this.animal.createChild(this.targetMate);
      slabby.setSlabfishType(animal.getTypeForBreeding(animal.world, this.animal, this.targetMate));
      if (this.animal.getLoveCause().getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) >= 72000 && animal.world.isNightTime()) {
    	  if (rand.nextInt(4) == 0) slabby.setSlabfishType(SlabfishType.NIGHTMARE);
      }
      slabby.setPreNameType(slabby.getSlabfishType());
      final net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(animal, targetMate, slabby);
      final boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
      slabby = (SlabfishEntity) event.getChild();
      if (cancelled) {
         //Reset the "inLove" state for the animals
         this.animal.setGrowingAge(6000);
         this.targetMate.setGrowingAge(6000);
         this.animal.resetInLove();
         this.targetMate.resetInLove();
         return;
      }
      if (slabby != null) {
         ServerPlayerEntity serverplayerentity = this.animal.getLoveCause();
         if (serverplayerentity == null && this.targetMate.getLoveCause() != null) {
            serverplayerentity = this.targetMate.getLoveCause();
         }

         if (serverplayerentity != null) {
            serverplayerentity.addStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity, this.animal, this.targetMate, slabby);
         }

         this.animal.setGrowingAge(6000);
         this.targetMate.setGrowingAge(6000);
         this.animal.resetInLove();
         this.targetMate.resetInLove();
         slabby.setGrowingAge(-24000);
         slabby.setLocationAndAngles(this.animal.getPosX(), this.animal.getPosY(), this.animal.getPosZ(), 0.0F, 0.0F);
         this.world.addEntity(slabby);
         this.world.setEntityState(this.animal, (byte)18);
         if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT)) {
            this.world.addEntity(new ExperienceOrbEntity(this.world, this.animal.getPosX(), this.animal.getPosY(), this.animal.getPosZ(), this.animal.getRNG().nextInt(7) + 1));
         }

      }
   }
}
