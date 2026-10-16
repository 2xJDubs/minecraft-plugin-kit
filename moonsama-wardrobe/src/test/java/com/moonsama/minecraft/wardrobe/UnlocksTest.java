package com.moonsama.minecraft.wardrobe;

import com.moonsama.minecraft.api.AssetHolding;
import com.moonsama.minecraft.compositor.CompositorData.SlotValue;
import com.moonsama.minecraft.compositor.SkinCompositor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnlocksTest {
    private static SkinCompositor compositor;
    private static Unlocks unlocks;

    @BeforeAll
    static void load() {
        compositor = SkinCompositor.load(UnlocksTest.class.getClassLoader());
        unlocks = new Unlocks(compositor);
    }

    private static AssetHolding holding(String collection, long tokenId) {
        return new AssetHolding(collection, Long.toString(tokenId), "1");
    }

    private static boolean offers(List<SlotValue> options, String asset) {
        return options.stream().anyMatch(v -> v.asset().equals(asset));
    }

    @Test
    void traitsOfOtherOwnedTokensUnlockParts() {
        // Moonsama #2 wears the aviator hat; #1 has no hat.
        List<AssetHolding> onlyOne = List.of(holding("moonsama", 1));
        List<AssetHolding> both = List.of(holding("moonsama", 1), holding("moonsama", 2));

        assertFalse(offers(unlocks.available("moonsama", "hat", onlyOne), "moonsama:aviator"));
        assertTrue(offers(unlocks.available("moonsama", "hat", both), "moonsama:aviator"));
    }

    @Test
    void ownPartsAreAlwaysAvailable() {
        List<AssetHolding> onlyOne = List.of(holding("moonsama", 1));
        assertTrue(offers(unlocks.available("moonsama", "hair", onlyOne), "moonsama:blue_slick"));
        assertTrue(offers(unlocks.available("moonsama", "outfit", onlyOne), "moonsama:rainbow_shirt"));
    }

    @Test
    void multiverseItemsUnlockThroughMultiverseItemsHoldings() {
        List<AssetHolding> withoutItem = List.of(holding("moonsama", 1));
        List<AssetHolding> withItem = List.of(holding("moonsama", 1), holding("multiverseitems", 64));

        boolean anywhereWithout = compositor.customizableSlots("moonsama").stream()
                .anyMatch(slot -> offers(unlocks.available("moonsama", slot.referenceId(), withoutItem), "multiverse_items:amber_detectore"));
        boolean anywhereWith = compositor.customizableSlots("moonsama").stream()
                .anyMatch(slot -> offers(unlocks.available("moonsama", slot.referenceId(), withItem), "multiverse_items:amber_detectore"));

        assertFalse(anywhereWithout, "detectore must be locked without the Multiverse Items token");
        assertTrue(anywhereWith, "Multiverse Items #64 must unlock the amber detectore");
    }

    @Test
    void costumesUnlockThroughThePortalCostumeCollection() {
        // Multiverse Costumes (0xc630…) is the Portal collection `multivercostumes`, same token ids.
        // The party hat is costume #37; a generous wallet without it stays locked.
        SlotValue partyHat = new SlotValue("costume", "multiverse-costumes", "multiverse_costumes:2nd_anniversary_party_hat");
        List<AssetHolding> whale = List.of(holding("moonsama", 1), holding("moonsama", 2),
                holding("multiverseitems", 4), holding("multiverseitems", 37), holding("exosama", 1));
        assertFalse(unlocks.isUnlocked(partyHat, unlocks.ownedParts("moonsama", whale), whale));

        List<AssetHolding> withHat = List.of(holding("moonsama", 1), holding("multiversecostumes", 37));
        assertTrue(unlocks.isUnlocked(partyHat, unlocks.ownedParts("moonsama", withHat), withHat));

        // Kringle Kap is costume 19-20 (regular / soulbound twin).
        SlotValue kringle = new SlotValue("costume", "multiverse-costumes", "multiverse_costumes:kringle_kap");
        List<AssetHolding> soulbound = List.of(holding("moonsama", 1), holding("multiversecostumes", 20));
        assertTrue(unlocks.isUnlocked(kringle, unlocks.ownedParts("moonsama", soulbound), soulbound));
        List<AssetHolding> wrongCostume = List.of(holding("moonsama", 1), holding("multiversecostumes", 21));
        assertFalse(unlocks.isUnlocked(kringle, unlocks.ownedParts("moonsama", wrongCostume), wrongCostume));
    }

    @Test
    void freePolicyNeverBypassesPortalCollections() {
        // Every contract in the shipped data is a Portal collection now, so FREE has nothing to open.
        Unlocks free = new Unlocks(compositor, Unlocks.OutsidePortalPolicy.FREE);
        List<AssetHolding> holder = List.of(holding("moonsama", 1));
        Set<String> owned = free.ownedParts("moonsama", holder);
        assertFalse(free.isUnlocked(new SlotValue("costume", "multiverse-costumes", "multiverse_costumes:kringle_kap"), owned, holder));
        assertFalse(free.isUnlocked(new SlotValue("mainhand", "multiverse-items", "multiverse_items:amber_detectore"), owned, holder));
    }

    @Test
    void podsUnlockThroughThePortalPodsCollection() {
        List<AssetHolding> podHolder = List.of(holding("moonsama", 1), holding("pods", 12));
        Set<String> owned = unlocks.ownedParts("moonsama", podHolder);
        SlotValue pods = new SlotValue("costume", "multiverse-costumes", "multiverse_costumes:pods");
        assertTrue(unlocks.isUnlocked(pods, owned, podHolder));
        assertFalse(unlocks.isUnlocked(pods, owned, List.of(holding("moonsama", 1))));
    }

    @Test
    void holdingsWithZeroBalanceDoNotCount() {
        List<AssetHolding> sold = List.of(holding("moonsama", 1), new AssetHolding("moonsama", "2", "0"));
        assertFalse(offers(unlocks.available("moonsama", "hat", sold), "moonsama:aviator"));
    }
}
