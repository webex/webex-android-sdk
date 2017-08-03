package com.cisco.spark.android.spaceball;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.cisco.spark.android.client.VoidCallback;
import com.cisco.spark.android.util.Action;
import com.cisco.spark.android.util.ImageUtils;
import com.github.benoitdion.ln.Ln;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class SpaceballView extends FrameLayout {

    private List<SpaceballBaseItemView> itemViews;

    private static final int REFRESH_DELAY_BASE_TIME = 100;

    private GridLayout gridLayout;

    private PointF startPosition;

    protected AnimatorSet scaleSet;
    protected AnimatorSet refreshSet;

    protected boolean scaleAnimating;
    protected boolean refreshAnimating;
    protected int rowNum;
    protected int colNum;

    private Context context;

    public SpaceballView(Context context) {
        this(context, null);
    }

    public SpaceballView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SpaceballView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SpaceballView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        itemViews = new ArrayList<>();
        gridLayout = buildGridLayout(this.context);
        setClipChildren(false);
    }

    private GridLayout buildGridLayout(Context context) {
        GridLayout gridLayout = new GridLayout(context);
        gridLayout.setClipChildren(false);

        LayoutParams frameParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        frameParams.gravity = Gravity.CENTER;
        gridLayout.setLayoutParams(frameParams);
        addView(gridLayout);

        return gridLayout;
    }

    public void addChild(SpaceballBaseItemView view) {
        itemViews.add(view);
    }

    public List<SpaceballBaseItemView> getChild() {
        return itemViews;
    }

    public void setGridRowAndColumn(int rowNum, int colNum) {
        this.rowNum = rowNum;
        this.colNum = colNum;
        checkSetRowColumn();
    }

    public void notifyDataSource() {
        checkSetRowColumn();
        if (null != itemViews && itemViews.size() > 0) {
            redraw(true);
        }
    }

    private void checkSetRowColumn() {
        try {
            if (rowNum > 0) {
                gridLayout.setRowCount(rowNum);
            }
            if (colNum > 0) {
                gridLayout.setColumnCount(colNum);
            }
        } catch (IllegalArgumentException ex) {
            // Catch and rethrow exception with more debug data. The original issue is resolved
            // But if it still surfaces in the field we'd like more data.
            int childCount = gridLayout.getChildCount();
            throw new IllegalArgumentException(
                    "CheckSetRowColumn failed col = " + colNum +
                    " rows = " + rowNum +
                    " children = " + childCount +
                    " itemsViews = " + (itemViews == null ? null : itemViews.size()),
                    ex);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    protected synchronized void redraw(boolean refresh) {

        if (refreshAnimating || scaleAnimating)
            return;

        if (refreshSet != null && refreshSet.isRunning() && refresh) {
            refreshSet.cancel();
        }

        setVisibility(VISIBLE);

        if (refresh) {
            refreshAppPositions();

            Collection<Animator> anims = new ArrayList<>();
            for (int i = 0; i < itemViews.size(); i++) {
                SpaceballBaseItemView itemView = itemViews.get(i);
                itemView.setAlpha(0.0f);
                long delay = i * REFRESH_DELAY_BASE_TIME; // Milliseconds
                anims.addAll(itemView.getAnimators(delay, true));
            }

            refreshAnimating = true;
            refreshSet = new AnimatorSet();
            refreshSet.playTogether(anims);
            refreshSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    refreshAnimating = false;
                    for (SpaceballBaseItemView view : itemViews) {
                        view.setAlpha(1.0f);
                        view.setScaleX(1.0f);
                        view.setScaleY(1.0f);
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    refreshAnimating = false;
                    for (SpaceballBaseItemView view : itemViews) {
                        view.setAlpha(1.0f);
                        view.setScaleX(1.0f);
                        view.setScaleY(1.0f);
                    }
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });

            // The delay here is kind of arbitrary but because of activity animations, this one can
            // start too soon and we'll miss out on the beginning. This delay seems to smooth things out a bit
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshSet.start();
                }
            }, 2 * REFRESH_DELAY_BASE_TIME);
        } else {
            // TODO Need to kick the apps to refresh their state
        }

        gridLayout.requestLayout();
    }

    public synchronized void animateBalls(final View view, final Action<VoidCallback> onAnimationEndAction) {
        if (scaleAnimating) {
            return;
        }

        if (scaleSet != null && scaleSet.isRunning()) {
            scaleSet.cancel();
        }

        Collection<Animator> anims = new ArrayList<>();

        startPosition = new PointF(getX(), getY());

        final PointF vCentre = new PointF(gridLayout.getX() + view.getX() + view.getWidth() / 2,
                gridLayout.getY() + view.getY() + view.getHeight() / 2);

        PointF gCentre = new PointF(gridLayout.getX() + gridLayout.getWidth() / 2, gridLayout.getY() + gridLayout.getHeight() / 2);
        PointF offset = getOffset(vCentre, gCentre);

        if (view instanceof SpaceballBaseItemView) {
            SpaceballBaseItemView v = (SpaceballBaseItemView) view;
            v.getImageButton().setImageAlpha(0);
        }
                Animator scaleX = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, 20f);
        scaleX.setDuration(500);
        scaleX.setInterpolator(new AccelerateInterpolator(1.6f));
        this.setPivotX(vCentre.x - offset.x);
        anims.add(scaleX);

        Animator scaleY = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, 20f);
        scaleY.setDuration(500);
        scaleY.setInterpolator(new AccelerateInterpolator(1.6f));
        this.setPivotY(vCentre.y - offset.y);
        anims.add(scaleY);

        Animator alpha = ObjectAnimator.ofFloat(view, "alpha", 1.0f, 0.0f);
        alpha.setStartDelay(150);
        alpha.setDuration(300);
        alpha.setInterpolator(new AccelerateInterpolator());
        anims.add(alpha);

        scaleSet = new AnimatorSet();
        scaleSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                scaleAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (onAnimationEndAction != null) {
                    onAnimationEndAction.call(null);
                }
                // Reset the spaceball view
                SpaceballView.this.setScaleX(1.0f);
                SpaceballView.this.setScaleY(1.0f);
                SpaceballView.this.setX(startPosition.x);
                SpaceballView.this.setY(startPosition.y);
                setVisibility(GONE);
                scaleAnimating = false;
                if (view instanceof SpaceballBaseItemView) {
                    SpaceballBaseItemView v = (SpaceballBaseItemView) view;
                    v.getImageButton().setImageAlpha(255);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                SpaceballView.this.setScaleX(1.0f);
                SpaceballView.this.setScaleY(1.0f);
                SpaceballView.this.setX(startPosition.x);
                SpaceballView.this.setY(startPosition.y);
                setVisibility(GONE);
                scaleAnimating = false;
                if (view instanceof SpaceballBaseItemView) {
                    SpaceballBaseItemView v = (SpaceballBaseItemView) view;
                    v.getImageButton().setImageAlpha(255);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        scaleSet.playTogether(anims);
        scaleSet.start();
    }

    private PointF getOffset(PointF viewCentre, PointF gridCentre) {

        float offset = ImageUtils.dpToPx(getResources(), 13f);

        float x = 0;
        float y = 0;

        if (viewCentre.x < gridCentre.x) {
            x = offset;
        } else if (viewCentre.x > gridCentre.x) {
            x = -offset;
        }

        if (viewCentre.y < gridCentre.y) {
            y = offset;
        } else if (viewCentre.y > gridCentre.y) {
            y = -offset;
        }

        return new PointF(x, y);
    }

    public void cancelAnimations() {
        if (refreshSet != null) {
            refreshSet.cancel();
            refreshAnimating = false;
        }

        if (scaleSet != null) {
            scaleSet.cancel();
            scaleAnimating = false;
            Ln.i("Redraw Spaceball Because Animation is Cancelled");
            redraw(true);
        }
    }

    public void refreshAppPositions() {
        gridLayout.removeAllViews();

        int pos = 0;
        for (int i = 0; i < itemViews.size(); i++) {
            SpaceballBaseItemView baseItemView = itemViews.get(i);
            if (baseItemView.getVisibility() == VISIBLE) {
                gridLayout.addView(baseItemView, pos++);
            }
        }
    }

    public void removeAllBalls() {
        gridLayout.removeAllViews();
    }

}
