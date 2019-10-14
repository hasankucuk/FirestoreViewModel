package io.github.achmadhafid.sample_app

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.redmadrobot.acronymavatar.AvatarView
import io.github.achmadhafid.sample_app.entity.User
import io.github.achmadhafid.zpack.ktx.f
import io.github.achmadhafid.zpack.ktx.inflate

class UserListAdapter : ListAdapter<User, UserViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        UserViewHolder(parent.inflate(R.layout.recycler_view_user))

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bindTo(getItem(position))
    }
}

class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val ivUserAvatar: AvatarView = view.f(R.id.iv_user_avatar)
    private val tvUserName: TextView = view.f(R.id.tv_user_name)

    fun bindTo(user: User?) {
        ivUserAvatar.setText(user?.name ?: ("empty"))
        tvUserName.text = user?.name ?: "(empty)"
    }
}

private val diffCallback = object : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: User, newItem: User) =
        oldItem == newItem
}
