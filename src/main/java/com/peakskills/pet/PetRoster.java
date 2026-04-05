package com.peakskills.pet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A player's full pet collection — 1 active + up to 20 passive slots.
 */
public class PetRoster {

    public static final int MAX_SLOTS = 20;

    private final List<PetInstance> pets = new ArrayList<>();

    // --- Roster management ---

    public boolean addPet(PetInstance pet) {
        if (pets.size() >= MAX_SLOTS) return false;
        pets.add(pet);
        return true;
    }

    public boolean removePet(UUID petId) {
        return pets.removeIf(p -> p.getId().equals(petId));
    }

    public List<PetInstance> getPets() {
        return Collections.unmodifiableList(pets);
    }

    public int size() { return pets.size(); }
    public boolean isFull() { return pets.size() >= MAX_SLOTS; }

    // --- Active pet ---

    public Optional<PetInstance> getActivePet() {
        return pets.stream().filter(PetInstance::isActive).findFirst();
    }

    /**
     * Sets a pet as active. Deactivates any previously active pet.
     * Pass null to deactivate without setting a new one.
     */
    public void setActivePet(UUID petId) {
        pets.forEach(p -> p.setActive(false));
        if (petId != null) {
            pets.stream()
                .filter(p -> p.getId().equals(petId))
                .findFirst()
                .ifPresent(p -> p.setActive(true));
        }
    }

    public void deactivate() {
        pets.forEach(p -> p.setActive(false));
    }

    // --- XP feeding ---

    /**
     * Awards XP to the active pet when the player gains skill XP.
     * Full XP if the skill matches the pet's affinity, 10% otherwise.
     * Returns true if the pet leveled up.
     */
    public boolean feedXp(com.peakskills.skill.Skill skill, long skillXp) {
        return feedXp(skill, skillXp, 1.0);
    }

    /**
     * Awards XP to the active pet, scaled by a taming multiplier.
     * Beast Bond (Taming lv50) passes 2.0, Pet Whisperer (lv99) passes 3.0.
     */
    public boolean feedXp(com.peakskills.skill.Skill skill, long skillXp, double tamingMult) {
        Optional<PetInstance> active = getActivePet();
        if (active.isEmpty()) return false;

        PetInstance pet = active.get();
        long petXp = (long)((pet.getType().affinity == skill
            ? skillXp          // full XP for affinity skill
            : skillXp / 10)    // 10% for non-affinity
            * tamingMult);

        if (petXp <= 0) return false;
        return pet.addXp(petXp);
    }

    // --- Lookup ---

    public Optional<PetInstance> findById(UUID id) {
        return pets.stream().filter(p -> p.getId().equals(id)).findFirst();
    }
}
