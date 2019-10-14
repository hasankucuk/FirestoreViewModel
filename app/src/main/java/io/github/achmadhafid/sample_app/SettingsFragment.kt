package io.github.achmadhafid.sample_app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.florent37.viewanimator.ViewAnimator
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.ramotion.fluidslider.FluidSlider
import io.github.achmadhafid.simplepref.SimplePref
import io.github.achmadhafid.simplepref.livedata.simplePrefLiveData
import io.github.achmadhafid.simplepref.simplePref
import io.github.achmadhafid.zpack.delegate.fragmentView
import io.github.achmadhafid.zpack.ktx.resolveColor
import kotlin.math.floor

class SettingsFragment : BottomSheetDialogFragment(), SimplePref {

    //region Preference

    private var requireAuth by simplePref("require_auth") { true }
    private var requireOnline by simplePref("require_online") { true }
    private var syncWait by simplePref("sync_wait") { 0L }

    //endregion
    //region View

    private val swRequireAuth by fragmentView<SwitchMaterial>(R.id.sw_require_auth)
    private val swRequireOnline by fragmentView<SwitchMaterial>(R.id.sw_require_online)
    private val tvSyncWait by fragmentView<TextView>(R.id.tv_sync_wait)
    private val sbSyncWait by fragmentView<FluidSlider>(R.id.sb_sync_wait)

    //endregion
    //region Lifecycle Callback

    override fun getTheme() = R.style.LottieDialogTheme_BottomSheet_DayNight

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    @Suppress("MagicNumber")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //region setup switch `require auth`

        swRequireAuth?.setOnCheckedChangeListener { _, isChecked ->
            requireAuth = isChecked
        }
        simplePrefLiveData(requireAuth, ::requireAuth) {
            swRequireAuth?.isChecked = it
        }

        //endregion
        //region setup switch `require online`

        swRequireOnline?.setOnCheckedChangeListener { _, isChecked ->
            requireOnline = isChecked
        }
        simplePrefLiveData(requireOnline, ::requireOnline) {
            swRequireOnline?.isChecked = it
        }

        //endregion
        //region setup slider `sync wait`

        sbSyncWait?.run {
            colorBubble     = context.resolveColor(R.attr.colorPrimary)
            colorBubbleText = context.resolveColor(R.attr.colorOnPrimary)
            colorBar        = colorBubble
            positionListener = {
                bubbleText = "${(floor(5000 * position / 100) * 100).toLong()}"
            }
            val height by lazy { tvSyncWait!!.height.toFloat() }
            val animation by lazy { ViewAnimator.animate(tvSyncWait) }
            beginTrackingListener = {
                animation.translationY(0F, -height)
                    .alpha(1F, 0F)
                    .duration(200L)
                    .start()
            }
            endTrackingListener = {
                animation.translationY(-height, 0F)
                    .alpha(0F, 1F)
                    .duration(400L)
                    .start()
                syncWait = (floor(5000 * position / 100) * 100).toLong()
            }
        }
        simplePrefLiveData(syncWait, ::syncWait) {
            sbSyncWait?.position   = syncWait / 5000F
            sbSyncWait?.bubbleText = "$syncWait"
        }

        //endregion
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(true)
        }

    //endregion
}

//region Extension Helper

fun BaseActivity.showSettings() {
    SettingsFragment().show(
        supportFragmentManager.beginTransaction()
            .addToBackStack("settings"), null
    )
}

//endregion
