class WebRtcVad {

    external fun nativeIsSpeech(frame: ShortArray): Boolean

    companion object {
        init {
            System.loadLibrary("vadlib")
        }
    }
}
