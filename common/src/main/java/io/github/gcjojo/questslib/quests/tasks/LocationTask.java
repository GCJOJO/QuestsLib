package io.github.gcjojo.questslib.quests.tasks;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import io.github.gcjojo.questslib.quests.QuestTask;
import io.github.gcjojo.questslib.quests.enums.LocationTaskType;
import io.github.gcjojo.questslib.quests.enums.TaskType;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class LocationTask extends QuestTask {
    Either<ResourceLocation, LocationTagTarget> locationTarget;
    LocationTaskType locationTaskType;

    public LocationTask(JsonObject json) {
        super(json);

        locationTaskType = LocationTaskType.None;
        if (json.has("location_type"))
            locationTaskType = LocationTaskType.fromId(json.get("location_type").getAsString());

        if (json.has("location")) {
            String locationRaw = json.get("location").getAsString();
            if (locationRaw.startsWith("#")) {
                ResourceLocation locationId = ResourceLocation.tryParse(locationRaw.split("#")[1]);
                if (locationTaskType == LocationTaskType.Biome)
                    locationTarget = Either.right(new LocationTask.LocationTagTarget.BiomeTarget(TagKey.create(Registries.BIOME, locationId)));
                else if (locationTaskType == LocationTaskType.Structure)
                    locationTarget = Either.right(new LocationTask.LocationTagTarget.StructureTarget(TagKey.create(Registries.STRUCTURE, locationId)));
            } else
                locationTarget = Either.left(ResourceLocation.tryParse(json.get("location").getAsString()));
        }
    }

    public boolean locationMatch(ServerLevel level, ResourceLocation location) {
        AtomicBoolean matches = new AtomicBoolean(false);
        locationTarget.ifLeft(target -> {
            matches.set(location.equals(target));
        });

        locationTarget.ifRight(target -> {
            Optional<ResourceKey<? extends Registry<?>>> registryKeyOpt = Optional.empty();
            if (locationTaskType == LocationTaskType.Biome)
                registryKeyOpt = Optional.of(Registries.BIOME);
            else if (locationTaskType == LocationTaskType.Structure)
                registryKeyOpt = Optional.of(Registries.STRUCTURE);

            registryKeyOpt.ifPresent(registryKey -> {
                if (target instanceof LocationTagTarget.BiomeTarget biomeTarget) {
                    var currentKey = ResourceKey.create(Registries.BIOME, location);
                    var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
                    matches.set(registry.getHolder(currentKey)
                            .map(holder -> holder.is(biomeTarget.value()))
                            .orElse(false));
                } else if (target instanceof LocationTagTarget.StructureTarget structureTarget) {
                    var currentKey = ResourceKey.create(Registries.STRUCTURE, location);
                    var registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
                    matches.set(registry.getHolder(currentKey)
                            .map(holder -> holder.is(structureTarget.value()))
                            .orElse(false));
                }
            });

        });

        return matches.get();
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.Location;
    }

    public sealed interface LocationTagTarget {
        record BiomeTarget(TagKey<Biome> value) implements LocationTagTarget {
        }

        record StructureTarget(TagKey<Structure> value) implements LocationTagTarget {
        }
    }

    @Getter
    @Setter
    public static class LocationTaskData extends QuestTaskData<LocationTask> {
        boolean hasVisitedLocation = false;

        public LocationTaskData(@NotNull LocationTask parentTask) {
            super(parentTask);
        }

        @Override
        public float getProgression() {
            return hasVisitedLocation ? 1.0f : 0.0f;
        }

        @Override
        public CompoundTag serialize() {
            CompoundTag nbt = new CompoundTag();
            nbt.putBoolean("HasVisitedLocation", hasVisitedLocation);
            return nbt;
        }

        @Override
        public void deserialize(CompoundTag nbt) {
            if (nbt.contains("HasVisitedLocation"))
                hasVisitedLocation = nbt.getBoolean("HasVisitedLocation");
        }
    }
}
