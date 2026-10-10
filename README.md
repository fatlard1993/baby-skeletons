# Baby Skeletons

Now and then a skeleton comes out half-size: frailer, with a weaker bow, still firing whatever its kind fires, and once in a while riding something.

## What This Mod Does

Zombies have had babies for as long as anyone can remember, and they are the most frightening thing a zombie does. Skeletons never got them. This gives every kind of skeleton a baby: a small thing that is harder to hit than it is to kill, and that now and then turns up on a chicken, a goat kid or a frog, the way a baby zombie turns up on a chicken.

Server-side; Pandorical carries the client's half, and draws each baby as a baby rather than a grown skeleton shrunk.

## Which Skeletons

Every hostile skeleton in the game has babies: the skeleton, the stray, the bogged, the parched and the wither skeleton, as of the version this mod targets (`gradle.properties`). It goes by what a mob *is* rather than a list, so a kind of skeleton added later has babies without anybody remembering to say so. The skeleton horse is not one of them: it is a horse.

A baby keeps everything its kind has, because it is that kind:

| Baby | Its arrows (or blade) |
|------|------|
| Skeleton | plain arrows |
| Stray | slowness |
| Bogged | poison |
| Parched | weakness |
| Wither Skeleton | a wooden sword, and the wither on every hit |

## How Big, How Tough

| | Grown | Baby |
|---|---|---|
| Size and hitbox | 1 | half |
| Health | 20 (16 for bogged and parched) | half: 10 (8) |
| Speed | 1 | half again as fast with a blade; the same with a bow |
| Bow | full draw, stops to shoot at fifteen blocks | three-quarter draw, comes in to ten |
| Wither skeleton's blade | stone sword | wooden sword |
| Experience | 5 and its gear | two and a half times that |

The speed and the experience are the baby zombie's own numbers, applied the same way.

**Only a baby that fights hand to hand is quick.** A baby zombie is frightening because it closes the distance before you can back away; an archer has no distance to close, because it stops and shoots. So the baby wither skeleton runs, and the archers walk at a grown skeleton's pace. It goes by what the baby is holding rather than by its kind, and changes the moment that does: a wither skeleton that picks up a bow slows to a walk, and a skeleton that picks up a sword starts to run. A baby saved before this was decided by weapon loses the speed it no longer has a use for the next time it loads.

**A weaker draw is a shorter reach.** The arrow leaves at three quarters of the speed, which hits about a quarter softer and falls short of anything a grown skeleton could reach. The baby still aims like a grown one, which assumes the full draw, so past about ten blocks its arrows land at your feet.

**There is no growing up.** A baby skeleton stays one.

## Spawning

**About one skeleton in twenty comes out a baby**, the same odds as a baby zombie. That goes for every way a skeleton is finished spawning: the night, spawners, trial spawners, structures, a spawn egg on the ground and `/summon`. That includes the skeleton the game sits on a spider jockey and the parched it sits on a husk's camel, which the game finishes as ordinary spawns, so now and then a spider carries a baby. A skeleton trap's horsemen and a skeleton freezing into a stray are never rolled; a frozen baby skeleton becomes a baby stray, and a grown one does not shrink.

**For a baby for certain:**

- **Use a spawn egg on a grown skeleton.** It hatches a baby of that kind, bow and all, the way an egg used on a zombie hatches a baby zombie.
- **`/summon skeleton ~ ~ ~ {Tags:["baby_skeleton"]}`**, with any kind of skeleton.

Which skeleton is a baby is written on it as a scoreboard tag, `baby_skeleton`. Tags are saved with the mob by the game itself and can be read or set with `/tag`. A skeleton that has the tag added by hand turns small the next time it loads.

## Jockeys

**A baby rides as often as a baby zombie does,** by the baby zombie's own two rolls. As it spawns, one baby in twenty looks for something to ride within five blocks that nobody is riding, and climbs on the first it finds; if there is nothing, that is the end of it. Failing that, another one in twenty brings its mount with it. On open ground that is a little under one baby in twenty, about one skeleton in four hundred, and more where there is something about to climb on. Every way a baby is born rolls it, a spawn egg used on a grown skeleton included.

Every kind rides a chicken, as a baby zombie does. Where a kind has an animal of its own country, it rides that instead:

| Baby | Rides | Where |
|------|-------|-------|
| Skeleton | a cave spider | wherever no daylight reaches: caves, mineshafts, dungeons |
| Stray | a goat kid | wherever it is cold enough to snow |
| Bogged | a frog, or a mooshroom calf | wherever frogs live: two frogs to every calf |
| Parched | an armadillo | dry country, where no rain falls |
| Wither Skeleton | a baby hoglin | the Nether |
| Any of them | a chicken | everywhere else |

A bogged's calf is red or brown, as often as each: the bogged wears one mushroom of each colour.

**The baby fights and the mount carries it.** A mob riding a mob steers it: the mount stops wandering, grazing and panicking, and goes where the baby's fight takes it, toward you for the wither skeleton's sword and in to ten blocks for a bow. That is the game's own rule for a mounted mob. A goat, a frog, an armadillo and a hoglin think differently from a chicken and would otherwise go on about their own business underneath, so they are given the chicken's treatment too: while a baby rides one, it does what its rider wants and nothing else.

- **A goat kid does not ram while it carries a rider.** Its rider does the fighting, so it never throws or hurts the baby on its back. Once the baby is gone it rams as any goat does.
- **An armadillo does not roll up while it carries a rider,** which is the game's own rule, and does not panic either. Struck, it keeps walking.
- **A frog is slow.** On land a frog walks, and gets about by leaping, which its rider does not ask of it.
- **A calf, a kid and a hoglin piglet stay young,** held by the same age lock a golden dandelion sets, so feeding one does not grow it either. A dandelion lifts it, as on any locked baby. A calf can be neither sheared nor milked, being a calf, and lightning turns its colour, as any mooshroom's, with its rider still on.
- **No mount turns on its rider,** and none drops it, except where the game drops anyone: a chicken in deep water, and a spear's knockdown.

**A jockey goes when nobody is near,** as a vanilla one does, but never from under its rider. The baby despawns as any skeleton does, and its mount, once it is alone, follows. A mount whose rider was killed stays a jockey's, as vanilla's chicken does: a chicken lays no eggs and is worth ten experience, and any of them despawns rather than staying for good, as an animal would.

A jockey put together by hand works the same, with the baby steering and its mount's own will put away: `/summon frog ~ ~ ~ {Passengers:[{id:"bogged",Tags:["baby_skeleton"]}]}`. Add the tag `baby_skeleton_mount` to the mount for it to despawn as a jockey does. Nothing climbs on anything by itself after it has spawned; a baby zombie does not either.

## Drops

Vanilla's, unchanged: a baby drops what a grown one of its kind drops. It is worth two and a half times the experience.

## Looking At One

With [block-tip](https://github.com/fatlard1993/block-tip) installed, the card names it properly: **Baby Skeleton**, **Baby Stray**, **Baby Wither Skeleton**, and so on, rather than the plain name the game gives it.

The card is the only place the word appears, deliberately. A custom name would fix the card too, but it hangs the word in the air over the mob, and a wild skeleton with its name floating over it looks like somebody's pet. Elsewhere, a death message says you were shot by a Stray, because you were.

## What One Looks Like

**With Pandorical**, a baby skeleton is shaped like a baby zombie: a big head on a small body, with short legs, standing as tall as a baby zombie beside it. Every kind keeps its own look, painted with its own texture: the stray in its coat, the bogged with its mushrooms (gone once it is sheared), the parched, and the wither skeleton a fifth bigger, as a grown one is. It walks, aims and swings as a skeleton does. Armour is a grown skeleton's, fitted to the baby's shape, and a bow or sword is held at its usual size, as a baby zombie holds one.

**Without it**, on a vanilla client or one with a Pandorical too old to know babies, a baby is a grown skeleton at half size: the same height, with a smaller head and longer legs. Nothing else differs. The hitbox, the speed, the bow and the health are the server's, and the same for every client.

## None Of Them Is A New Mob

A baby is the game's own skeleton, at half the size. A server-side mod cannot hand a vanilla client a new creature to draw, and it does not need to: size travels as one of the game's own attributes, which every client already knows how to draw. The bow shrinks with the hand holding it, and the hitbox with the body. This mod ships a baby skeleton model, the skeleton's own parts moved and resized to a baby zombie's proportions, and Pandorical draws babies with it, at a baby zombie's size rather than half of one.

So a baby is everything its kind is. A command that finds strays finds baby strays, and anything that changes what a stray does changes what a baby stray does.

**Removing this mod leaves its babies small.** The size, health and speed are saved on the mob, so they stay, but the weaker bow goes, and so does the extra experience. Jockeys stay mounted, and their mounts go back to thinking for themselves; a young mount stays young until a golden dandelion lifts its lock.

## Pandorical

Pandorical is required on the server; the mod will not load without it. It introduces the mod to players who join, and tells returning players what changed.

**With Pandorical client-side**, babies are drawn with the baby model (see [What One Looks Like](#what-one-looks-like)), from the Pandorical this mod declares as its minimum onwards.

**Without Pandorical client-side**, nothing about the babies is lost but their proportions. They are half-size on every client, vanilla included, because size is part of the game's own protocol.

## Development

Building, the icon and the tests are in [DEVELOPMENT.md](DEVELOPMENT.md).

## License

MIT, see [LICENSE](LICENSE).
