package com.github.kr328.clash.adapter

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.google.android.material.card.MaterialCardView

class ProxyChipAdapter(
    private val context: Context,
    val onClick: (String) -> Unit
) :
    RecyclerView.Adapter<ProxyChipAdapter.Holder>() {
    var chips = listOf<String>()
    var selected: String = ""
        set(value) {
            val lastIndex = chips.indexOf(field)
            val newIndex = chips.indexOf(value)

            field = value

            if (lastIndex >= 0)
                notifyItemChanged(lastIndex)
            if (newIndex >= 0)
                notifyItemChanged(newIndex)
        }

    @ColorInt
    private val colorOnSurface: Int

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
        colorOnSurface = typedValue.data
    }

    class Holder(root: View) : RecyclerView.ViewHolder(root) {
        val card: MaterialCardView = root.findViewById(R.id.root)
        val title: TextView = root.findViewById(android.R.id.title)

        private var cardAnimator: ValueAnimator? = null
        private var cardColor: Int = card.cardBackgroundColor.defaultColor
        private var titleAnimator: ValueAnimator? = null
        private var titleColor: Int = title.textColors.defaultColor

        fun setCardColorAnimation(color: Int) {
            if (cardColor == color)
                return

            cardAnimator?.cancel()

            cardAnimator = ValueAnimator.ofArgb(cardColor, color).apply {
                addUpdateListener {
                    val v = animatedValue as Int

                    card.setCardBackgroundColor(v)

                    cardColor = v
                }

                duration = 200
                start()
            }
        }

        fun setTitleColorAnimation(color: Int) {
            if (color == titleColor)
                return

            titleAnimator?.cancel()

            titleAnimator = ValueAnimator.ofArgb(titleColor, color).apply {
                addUpdateListener {
                    val v = animatedValue as Int

                    title.setTextColor(v)

                    titleColor = v
                }

                duration = 200
                start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layoutInflater = LayoutInflater.from(context)

        return Holder(layoutInflater.inflate(R.layout.adapter_proxies_chip, parent, false))
    }

    override fun getItemCount(): Int {
        return chips.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = chips[position]

        holder.title.text = current
        holder.card.setOnClickListener {
            onClick(current)
        }

        if (selected == current) {
            holder.setTitleColorAnimation(Color.WHITE)
            holder.setCardColorAnimation(context.getColor(R.color.primaryCardColorStarted))
        } else {
            holder.setTitleColorAnimation(colorOnSurface)
            holder.setCardColorAnimation(context.getColor(R.color.chipBackgroundColor))
        }
    }
}