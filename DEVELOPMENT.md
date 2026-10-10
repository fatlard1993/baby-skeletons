# Development

## Building

```
./gradlew build
```

or, from anywhere in the suite, `mc-build baby-skeletons`.

Compiles against Pandorical's working tree as a subproject, and against
block-tip's built jar as an optional compile-only dependency. If block-tip has
not been built, the build stops and says so rather than quietly dropping the
integration.

## Installing

Server-side. Drop the jar in the server's `mods/` alongside Pandorical. Players
need nothing: the babies are half-size on every client, because size is one of
the game's own attributes. Players with Pandorical (at least the version
`fabric.mod.json` asks for) see them drawn as babies.

## How a baby is drawn

The baby model is this mod's own content: `baby_skeleton.json` in
`src/main/resources/assets/baby-skeletons-justfatlard/pandorical/entity_models/`,
shipped to Pandorical clients by `registerModAssets` at init. It is Pandorical
entity-model part transforms, not new geometry, because the game has no baby
skeleton texture: it moves and resizes the grown skeleton's own parts,
so every kind keeps its own texture, coat, mushrooms and animation.

The numbers are 26.3's baby zombie (`BabyZombieModel.createBodyLayer`): a
6-pixel head on a 4-by-5 body, 5-pixel arms, 4-pixel legs, neck at 15, hips at
20, soles at 24. Against a grown humanoid that is the head at 3/4, body and arms
at 5/12 tall and 1/2 wide, legs at 1/3 tall and 1/2 wide:

| part | scale | offset |
|---|---|---|
| head | 0.75 | (0, 15, 0) |
| body, arms | (0.5, 5/12, 0.5) | (0, 15, 0) |
| legs | (0.5, 1/3, 0.5) | (0, 16, 0) |

One model serves every kind. They all hang the skeleton's limbs from the
model's root, and the wither skeleton is the skeleton with that root scaled 1.2
(`MeshTransformer.scaling(1.2F)` transforms the root, not the limbs), so the
same transforms make its baby a fifth larger, as the grown one is.

Wherever a baby's size is applied (`Babies.applySize`, which birth, a spawn
egg, `/summon`, a freeze into a stray and every load from the save all pass
through), it calls `PandoricalApi.entityModels().set(skeleton, Babies.MODEL,
null, 1 / Babies.SCALE)`. Pandorical keeps that only while the skeleton is loaded,
which is why it is said on every load. The model is a baby's size already, so it
is drawn at twice the half the scale attribute gives it, the size of a baby
zombie; the server keeps the attribute at half, because that is the hitbox and
what every other client draws.

## How speed follows the weapon

`Babies.applySpeed` gives a baby the +50% movement modifier only when it would
fight hand to hand, and takes it away otherwise. "Hand to hand" is the game's
own test: `AbstractSkeleton.reassessWeaponGoal` gives a skeleton the bow goal
if a bow is in the hand `ProjectileUtil.getWeaponHoldingHand` picks, and the
melee goal if not. That method runs whenever the skeleton's hands change
(`onEquipItem`), when it is read from a save, when it spawns and when it is
constructed, so `AbstractSkeletonMixin` says the speed again at its RETURN.
Nothing runs on any other tick. `applySize` says it too, which is what takes
the modifier off a baby archer saved by 1.0.0.

## How a baby rides

`Jockeys` is 26.3's chicken jockey (`Zombie.finalizeSpawn`) step for step. As
a baby is born (`AbstractSkeletonMixin`, and `Eggs` for an egg used on a grown
skeleton), one roll in twenty searches `inflate(5, 3, 5)` for an unridden
mount and climbs on the first; failing that roll, another one in twenty
brings a new mount, finished with `EntitySpawnReason.JOCKEY` and added to the
level before the baby is. `Jockeys.mountFor` picks the mount:

| baby | mount | when |
|---|---|---|
| skeleton | cave spider | sky light 0, in a dimension with sky light |
| stray | goat kid | `Biome.coldEnoughToSnow` |
| bogged | frog (2/3) or mooshroom calf (1/3) | frogs in the position's `NATURAL_MOB_SPAWNS` creatures |
| parched | armadillo | `!Biome.hasPrecipitation()`, in a dimension with sky light |
| wither skeleton | baby hoglin | `#minecraft:is_nether` |
| any | chicken | otherwise |

These are climates rather than the mounts' own spawn lists, because in 26.3
the two hardly overlap: strays spawn only in snowy plains and ice spikes,
goats only on peaks and slopes, parched only in desert, armadillos only in
savanna and badlands, hoglins only in crimson forest, and wither skeletons
only in fortresses. The frog is the one asked of the world, because a frog
and a bogged share both their swamps.

`Jockeys.saddle` marks a mount: the scoreboard tag `baby_skeleton_mount` on
every one, `setChickenJockey(true)` on a chicken (no eggs, ten experience, as
vanilla), and the golden dandelion's age lock on a young one
(`AgeableMobAccess`, an invoker), which also stops feeding from growing it.

Steering is vanilla: a `Mob` riding a `Mob` is its controlling passenger, its
`getNavigation()` and `getMoveControl()` return the vehicle's, and
`Mob.updateControlFlags` switches off the vehicle's MOVE, LOOK and JUMP goals.
That covers the goal-driven mounts (chicken, cave spider, mooshroom). The
brain-driven ones (goat, frog, armadillo, hoglin) have no such switch, so
`BrainMixin` skips `Brain.tick` while a baby is the controlling passenger
(`Jockeys.steered`, by the rider rather than the tag, so a `/summon`ed jockey
is steered too). Their body, navigation and move control keep ticking. It is
also why a ridden goat never rams (`RamTarget` hits the first living thing in
its box, its rider included) and a ridden armadillo neither panics nor rolls
up.

Despawning is in `MobMixin`, at the two calls in `Mob.checkDespawn`:
`requiresCustomPersistence()` is true for a tagged mount carrying a baby and
false for a baby riding a tagged mount (vanilla has it the other way about:
the rider is kept, and the chicken despawns from under it), and
`removeWhenFarAway()` is true for any tagged mount, which is what
`Chicken.removeWhenFarAway` does for a jockey's chicken, extended to animals
whose own answer is false. Read at the call rather than overridden, because
`Animal` overrides `removeWhenFarAway` without calling up.

Vanilla finishes the skeleton it puts on a spider jockey, and the parched it
puts on a husk's camel, with the spider's or husk's own spawn reason, so those
are rolled like any other spawn, and a baby among them gets the jockey roll
too. On the one time in four hundred that it brings a mount, the spider or
camel then takes it, and the mount it brought is left standing as a
riderless jockey's mount, despawning as one. A baby husk does exactly the same
to its chicken in vanilla.

## The icon

```
python3 generate_icon.py
```

Cuts the skeleton's face out of the vanilla texture in the Loom cache and lays a
grown one and a baby, exactly half its size, on a night sky. Idempotent and safe
to re-run; it does not need the game running, only a build having happened once
so the Minecraft jar is cached.

## Tests

```
xvfb-run -a -s "-screen 0 2048x1152x24" ./gradlew runClientGameTest
```

Client gametests, so they open a window; run them under xvfb. `./gradlew build`
compiles them but does not run them. Each one prints
`[baby-skeletons test] PASSED <name>` when it gets to the end, so a run that
passed can be told from one that tested nothing.

- **SmokeTest** - the mod loads, its mixins bind, and a world comes up.
- **BabiesAreBorn** - the skeleton family in the game's `#skeletons` tag is the
  one the tests cover; about one natural spawn in twenty is a baby across all
  five kinds (it prints the count); a conversion never makes one; a spawn egg
  used on a grown skeleton hatches a baby with a bow; `/summon` with the tag
  makes a correctly sized one. Both are given their baby model.
- **BabiesAreSmall** - for every kind: half scale, half the grown one's health
  and at full health, half again as fast with a sword and a grown one's pace
  with a bow, a hitbox half as wide and tall, its
  kind's weapon (a wooden sword for the wither skeleton), more experience,
  given its baby model for Pandorical (and a grown one not); re-applying the
  size does not stack it; and a baby skeleton frozen in powder snow comes out a
  baby stray, with its baby model. The speed follows a weapon swap both ways,
  never reaches a grown skeleton, and a baby read from a 1.0.0 save (by
  `/summon` with the old modifier in its attributes) loses it if it is an
  archer and keeps it if it is a wither skeleton.
- **BabiesRideJockeys** - jockeys among 20,000 natural spawns against what
  vanilla's rolls expect (it prints both); a baby climbs on the right mount
  beside it and not a grown goat, and marks it; each kind's mount in its
  climate, painted with `/fillbiome` and a sealed stone room, the bogged's
  frog-to-calf split, and a chicken out of it; every pairing given an iron
  golem eighteen blocks off, still mounted after eight seconds and carried at
  least two blocks closer (it prints each); a ridden goat kid does not ram,
  against a riderless one that does; a struck armadillo with a rider does not
  roll up; a calf survives shears, a bucket, a bowl, wheat and a lightning
  strike as a calf with its rider on; a far-off jockey despawns rider first,
  then mount, and an ordinary goat does not; and a jockey made by `/summon`
  with passengers is steered.
- **JockeysPictured** - every kind on a chicken at the back and each kind's own
  mount in front, photographed as `baby-jockeys`.
- **BabiesLookLikeBabies** - every baby has its model on the server, including a
  bogged made by `/summon` with the tag (given it only by its load). On the
  test's own Pandorical client, each kind's model, posed as the game poses it,
  is measured against the game's own baby zombie model beside it: the same
  drawn scale, the same height (the wither skeleton 1.2 times), the same head;
  and a grown skeleton at half scale, what a vanilla client draws, is shown to
  measure differently. It takes two pictures, `baby-skeleton-model` (a baby
  skeleton, stray and wither skeleton beside a baby zombie) and
  `baby-skeleton-armor` (a baby skeleton and a baby zombie in iron), into the
  run's `screenshots/`, or with `-Ppictures` into this repo's too.

## What to check by hand

What the tests cannot see is how it looks in motion:

- **The arrows fall short past about ten blocks.** That is what the weaker draw
  is for, and it is a matter of physics rather than a number any test reads.
- **How a baby looks on a client.** The tests check that it is drawn as a baby
  and at what scale; whether the result looks right is a matter of looking at
  one, beside a baby zombie, on a client with Pandorical and on one without.
- **How a jockey fights.** The tests show each mount carries its rider toward a
  target; whether it looks right in a real fight, on real ground, is a matter
  of watching one. A frog jockey is slow on land, and a cave spider climbs.
- **The block-tip card.** block-tip is not in the test runtime, so its namer is
  compiled against but not exercised.
