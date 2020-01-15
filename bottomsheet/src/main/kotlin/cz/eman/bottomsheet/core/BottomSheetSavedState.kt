package cz.eman.bottomsheet.core

import android.os.Parcel
import android.os.Parcelable
import androidx.customview.view.AbsSavedState

/**
 * @author eMan s.r.o.
 * @since 1.0.0
 */
class SavedState : AbsSavedState {
    internal val state: BottomSheetState

    internal val peekHeightBig: Int
    internal val peekHeightSmall: Int
    internal val peekHeight: Int

    @JvmOverloads
    constructor(source: Parcel, loader: ClassLoader? = null) : super(source, loader) {

        state = BottomSheetState.valueOf(source.readString() ?: "")
        peekHeightBig = source.readInt()
        peekHeightSmall = source.readInt()
        peekHeight = source.readInt()
    }

    constructor(superState: Parcelable, state: BottomSheetState, peekBig: Int, peekSmall: Int, peek: Int) : super(superState) {
        this.state = state
        this.peekHeightBig = peekBig
        this.peekHeightSmall = peekSmall
        this.peekHeight = peek
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeString(state.name)
        out.writeInt(peekHeightBig)
        out.writeInt(peekHeightSmall)
        out.writeInt(peekHeight)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel): SavedState = SavedState(source)

            override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
        }
    }
}