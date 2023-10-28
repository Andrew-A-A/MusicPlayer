package com.andrew.saba.musicplayer.adapter

import android.content.ContentUris
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.andrew.saba.musicplayer.R
import com.andrew.saba.musicplayer.model.AudioTrack
import com.squareup.picasso.Picasso


class RvAdapter(private val audioTracks:ArrayList<AudioTrack>):RecyclerView.Adapter<RvAdapter.MyViewHolder>() {
    private var onItemClickListener: OnItemClickListener? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        val tracksView = inflater.inflate(R.layout.music_item, parent, false)
        return MyViewHolder(tracksView)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        onItemClickListener = listener
    }

    override fun getItemCount(): Int {
        return audioTracks.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val trackItem = audioTracks[position]
        holder.trackName.text = trackItem.name
        holder.artistName.text = trackItem.artist
        holder.currentTrack=trackItem
        val albumArtUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            trackItem.image
        )
        Picasso.get()
            .load(albumArtUri)
            .error(R.drawable.default_track_ic)
            .placeholder(R.drawable.default_track_ic)
            .fit()
            .into(holder.trackImage)
    }

    fun updateData(filteredTracks: ArrayList<AudioTrack>) {
        audioTracks.clear() // Clear the existing data
        audioTracks.addAll(filteredTracks) // Add the new data
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var trackImage: ImageView = itemView.findViewById(R.id.track_img)
        var trackName: TextView = itemView.findViewById(R.id.track_name_text)
        var artistName: TextView = itemView.findViewById(R.id.artist_name_text)
       lateinit var currentTrack:AudioTrack
        init {
            itemView.setOnClickListener {
                onItemClickListener?.onItemClicked(currentTrack)
            }
        }

    }
    interface OnItemClickListener {
        fun onItemClicked(audioTrack: AudioTrack)
    }
}
