package com.example.textit;

import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * ItemTouchHelper class for whenever a RecyclerView item in the chat activity or friends activity
 * is swiped left on. Displays the background view from these items whenever a left swipe occurs.
 */
public class RecyclerItemTouchHelper extends ItemTouchHelper.SimpleCallback {
    // Swipe listener and ViewHolder
    private RecyclerItemTouchHelperListener mListener;
    private String mViewHolder;

    public RecyclerItemTouchHelper(int dragDirs, int swipeDirs, String mViewHolder,
                                   RecyclerItemTouchHelperListener mListener) {
        super(dragDirs, swipeDirs);

        this.mViewHolder = mViewHolder;
        this.mListener = mListener;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        if (viewHolder != null && mViewHolder != null) {
            if (mViewHolder.equals(ChatAdapter.ChatHolder.class.getName())) {
                final View foregroundView = ((ChatAdapter.ChatHolder) viewHolder).foregroundView;
                getDefaultUIUtil().onSelected(foregroundView);
            } else if (mViewHolder.equals(FriendsAdapter.FriendsHolder.class.getName())) {
                final View foregroundView = ((FriendsAdapter.FriendsHolder) viewHolder).foregroundView;
                getDefaultUIUtil().onSelected(foregroundView);
            }
        }
    }

    @Override
    public void onChildDrawOver(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                int actionState, boolean isCurrentlyActive) {
        if (mViewHolder != null) {
            if (mViewHolder.equals(ChatAdapter.ChatHolder.class.getName())) {
                final View foregroundView = ((ChatAdapter.ChatHolder) viewHolder).foregroundView;
                getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
                        actionState, isCurrentlyActive);
            } else if (mViewHolder.equals(FriendsAdapter.FriendsHolder.class.getName())) {
                final View foregroundView = ((FriendsAdapter.FriendsHolder) viewHolder).foregroundView;
                getDefaultUIUtil().onDrawOver(c, recyclerView, foregroundView, dX, dY,
                        actionState, isCurrentlyActive);
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder) {
        if (mViewHolder != null) {
            if (mViewHolder.equals(ChatAdapter.ChatHolder.class.getName())) {
                final View foregroundView = ((ChatAdapter.ChatHolder) viewHolder).foregroundView;

                getDefaultUIUtil().clearView(foregroundView);
            } else if (mViewHolder.equals(FriendsAdapter.FriendsHolder.class.getName())) {
                final View foregroundView = ((FriendsAdapter.FriendsHolder) viewHolder).foregroundView;

                getDefaultUIUtil().clearView(foregroundView);
            }
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {
        if (mViewHolder != null) {
            View foregroundView = null;
            float newDx = dX;

            if (mViewHolder.equals(ChatAdapter.ChatHolder.class.getName())) {
                foregroundView = ((ChatAdapter.ChatHolder) viewHolder).foregroundView;
            } else if (mViewHolder.equals(FriendsAdapter.FriendsHolder.class.getName())) {
                foregroundView = ((FriendsAdapter.FriendsHolder) viewHolder).foregroundView;
            }

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                // Swipe left with max direction
                if (newDx <= -200f) {
                    newDx = -200f;
                }
                // Swipe right with max direction
                else if (newDx >= 0) {
                    newDx = 0;
                }
            }

            getDefaultUIUtil().onDraw(c, recyclerView, foregroundView, newDx, dY,
                    actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        mListener.onSwiped(viewHolder, direction, viewHolder.getAdapterPosition());
    }

    /**
     * Interface to listen to swipes on the RecyclerView
     */
    public interface RecyclerItemTouchHelperListener {
        void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position);
    }
}
