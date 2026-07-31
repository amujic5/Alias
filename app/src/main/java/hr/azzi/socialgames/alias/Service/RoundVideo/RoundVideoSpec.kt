package hr.azzi.socialgames.alias.Service.RoundVideo

/**
 * Shape of the round video, in one place. Ported from the iOS `RoundVideoSpec` — the
 * encoder and the capture session both read these, and the camera is capped at the rate
 * the video is written at, because every frame above it would be composited and thrown
 * away.
 */
object RoundVideoSpec {

    /** The Figma canvas the overlay is laid out on. */
    const val CANVAS_WIDTH = 390f
    const val CANVAS_HEIGHT = 844f

    /**
     * 2× the design canvas, so the overlay layout maps 1:1 and both dimensions stay even
     * (H.264 requires that).
     */
    const val RENDER_WIDTH = 780
    const val RENDER_HEIGHT = 1688

    /**
     * What each camera is asked for. Concurrent capture only guarantees 720p per stream,
     * and asking for more is how a session that configures on one device fails on the
     * next. The back feed is aspect-filled into a portrait frame, so its 720 short side
     * is what survives the crop anyway.
     */
    const val CAPTURE_WIDTH = 1280
    const val CAPTURE_HEIGHT = 720

    /** Frames per second, for the video and for the capture that feeds it. */
    const val FRAME_RATE = 24
    const val FRAME_DURATION_NS = 1_000_000_000L / FRAME_RATE

    const val BIT_RATE = 6_000_000
    /** A keyframe a second — the clip is scrubbed in a player, not streamed. */
    const val I_FRAME_INTERVAL = 1

    const val MIME = "video/avc"

    /**
     * How long the intro card holds at full opacity, and how long it dissolves for.
     *
     * The hold is longer than the card needs to finish animating — its last row lands around
     * 0.7 s — because the card is also what the viewer reads: whose round this is and who is
     * explaining. Long enough to read twice.
     */
    const val INTRO_HOLD_S = 3.3
    const val INTRO_FADE_S = 0.6
    const val OUTRO_S = 2.0

    /** Where the round itself starts on the clip's timeline — both tracks are offset by it. */
    const val INTRO_HOLD_NS = (INTRO_HOLD_S * 1_000_000_000).toLong()
}
