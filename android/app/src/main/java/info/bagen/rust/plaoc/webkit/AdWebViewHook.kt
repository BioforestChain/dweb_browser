package info.bagen.rust.plaoc.webkit

import android.view.ActionMode
import android.view.Menu
import android.view.MotionEvent

class AdWebViewHook {
    var onTouchEvent: ((event: MotionEvent?) -> Boolean)? = null
    var onCreateMenu: ((mode: ActionMode, menu: Menu) -> CustomMenu)? = null
}

interface CustomMenu {
    val title: String
    val subtitle: String
    val menus: Map</*name*/String, /*js-code*/String>
}
