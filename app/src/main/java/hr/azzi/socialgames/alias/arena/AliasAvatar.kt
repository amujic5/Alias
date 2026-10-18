package hr.azzi.socialgames.alias.arena

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import hr.azzi.socialgames.alias.R

/**
 * A face a player can wear on the leaderboard.
 *
 * Android port of iOS `AliasAvatar.swift`. Data rather than a plain enum for one reason,
 * and it is not a defensive nicety: the catalogue grows in the app, but ids live in the
 * database. The day three new avatars ship, every player still on the old build starts
 * meeting profiles and bots wearing ids it has never heard of. [named] fails soft and
 * shows them Robby; failing hard would show a hole in the leaderboard.
 */
data class AliasAvatar(
    val id: String,
    @DrawableRes val drawable: Int,
    @StringRes val nameRes: Int,
) {
    /** The level this avatar is handed over at. Zero for the eight that start unlocked. */
    val unlockLevel: Int get() = LevelRewards.levelForAvatar(id)

    /** The Black Friday bundle counts as having climbed the whole ladder. */
    val isUnlockedByLevel: Boolean
        get() = hr.azzi.socialgames.alias.Service.BlackFriday.isOwned ||
            PlayerLevel.current >= unlockLevel

    companion object {
        val ROBOT_BLUE = AliasAvatar("robot_blue", R.drawable.robot_blue, R.string.arena_avatar_robot_blue)
        val MONSTER_LIME = AliasAvatar("monster_lime", R.drawable.monster_lime, R.string.arena_avatar_monster_lime)
        val OWL_GLASSES = AliasAvatar("owl_glasses", R.drawable.owl_glasses, R.string.arena_avatar_owl_glasses)
        val CAT_PROFESSOR = AliasAvatar("cat_professor", R.drawable.cat_professor, R.string.arena_avatar_cat_professor)
        val HUMAN_MIA = AliasAvatar("human_mia", R.drawable.human_mia, R.string.arena_avatar_human_mia)
        val HUMAN_MAX = AliasAvatar("human_max", R.drawable.human_max, R.string.arena_avatar_human_max)
        val EXPLORER_LUNA = AliasAvatar("explorer_luna", R.drawable.explorer_luna, R.string.arena_avatar_explorer_luna)
        val ALIEN_ZIPPY = AliasAvatar("alien_zippy", R.drawable.alien_zippy, R.string.arena_avatar_alien_zippy)

        // The second row of the library. Same art batch as the first — cut out — so the
        // picker reads as one set rather than two.
        val HUMAN_TESSA = AliasAvatar("human_tessa", R.drawable.human_tessa, R.string.arena_avatar_human_tessa)
        val HUMAN_FINN = AliasAvatar("human_finn", R.drawable.human_finn, R.string.arena_avatar_human_finn)
        val RED_PANDA_KIKO = AliasAvatar("red_panda_kiko", R.drawable.red_panda_kiko, R.string.arena_avatar_red_panda_kiko)
        val PANDA_NORI = AliasAvatar("panda_nori", R.drawable.panda_nori, R.string.arena_avatar_panda_nori)
        val DRAGON_EMBER = AliasAvatar("dragon_ember", R.drawable.dragon_ember, R.string.arena_avatar_dragon_ember)
        val AXOLOTL_BUBBLES = AliasAvatar("axolotl_bubbles", R.drawable.axolotl_bubbles, R.string.arena_avatar_axolotl_bubbles)
        val CORGI_BOLT = AliasAvatar("corgi_bolt", R.drawable.corgi_bolt, R.string.arena_avatar_corgi_bolt)
        val TRICERATOPS_PEBBLE = AliasAvatar("triceratops_pebble", R.drawable.triceratops_pebble, R.string.arena_avatar_triceratops_pebble)

        // The third row, and a different art batch: drawn on their own coloured ground
        // rather than cut out.
        val ASTRONAUT_NOVA = AliasAvatar("astronaut_nova", R.drawable.astronaut_nova, R.string.arena_avatar_astronaut_nova)
        val WIZARD_HEX = AliasAvatar("wizard_hex", R.drawable.wizard_hex, R.string.arena_avatar_wizard_hex)
        val PARROT_RIO = AliasAvatar("parrot_rio", R.drawable.parrot_rio, R.string.arena_avatar_parrot_rio)
        val NINJA_SHADE = AliasAvatar("ninja_shade", R.drawable.ninja_shade, R.string.arena_avatar_ninja_shade)
        val GAMER_PIXEL = AliasAvatar("gamer_pixel", R.drawable.gamer_pixel, R.string.arena_avatar_gamer_pixel)
        val SKATER_DASH = AliasAvatar("skater_dash", R.drawable.skater_dash, R.string.arena_avatar_skater_dash)
        val GHOST_BOO = AliasAvatar("ghost_boo", R.drawable.ghost_boo, R.string.arena_avatar_ghost_boo)
        val MUSHROOM_SPROUT = AliasAvatar("mushroom_sprout", R.drawable.mushroom_sprout, R.string.arena_avatar_mushroom_sprout)

        // The last three, which exist because the reward ladder ran past the art.
        val HUMAN_JAX = AliasAvatar("human_jax", R.drawable.human_jax, R.string.arena_avatar_human_jax)
        val HUMAN_ZOE = AliasAvatar("human_zoe", R.drawable.human_zoe, R.string.arena_avatar_human_zoe)
        val HUMAN_REX = AliasAvatar("human_rex", R.drawable.human_rex, R.string.arena_avatar_human_rex)

        /** Everything this build can draw, in picker order. */
        val ALL: List<AliasAvatar> = listOf(
            ROBOT_BLUE, MONSTER_LIME, OWL_GLASSES, CAT_PROFESSOR,
            HUMAN_MIA, HUMAN_MAX, EXPLORER_LUNA, ALIEN_ZIPPY,
            HUMAN_TESSA, HUMAN_FINN, RED_PANDA_KIKO, PANDA_NORI,
            DRAGON_EMBER, AXOLOTL_BUBBLES, CORGI_BOLT, TRICERATOPS_PEBBLE,
            ASTRONAUT_NOVA, WIZARD_HEX, PARROT_RIO, NINJA_SHADE,
            GAMER_PIXEL, SKATER_DASH, GHOST_BOO, MUSHROOM_SPROUT,
            HUMAN_JAX, HUMAN_ZOE, HUMAN_REX,
        )

        /** Worn by anyone who has never chosen, and by anyone whose choice this build cannot draw. */
        val DEFAULT = ROBOT_BLUE

        /** The avatar for an id, falling back to the default rather than to nothing. */
        fun named(id: String?): AliasAvatar =
            ALL.firstOrNull { it.id == id } ?: DEFAULT

        /**
         * A catalogue id chosen at random — for seeding and placeholder profiles.
         *
         * Draws only from what this build actually ships, never from a wider planned
         * range, for the same reason [named] exists.
         */
        fun randomId(): String = ALL.random().id
    }
}
