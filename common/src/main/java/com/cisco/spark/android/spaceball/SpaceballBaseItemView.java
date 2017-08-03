package com.cisco.spark.android.spaceball;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.cisco.spark.android.R;
import com.cisco.spark.android.util.AppScaleInterpolator;
import com.cisco.spark.android.util.ImageUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpaceballBaseItemView extends FrameLayout {

    protected ImageButton imageButton;

    private SpaceballView spaceballView;
    private @DrawableRes int iconResId;
    private @ColorRes int backgroundColorResId;
    private Context context;
    private int buttonId;

    public SpaceballBaseItemView(Context context, SpaceballView spaceballView, @DrawableRes int iconResId,
                                 @ColorRes int backgroundColorResId, @IdRes int viewId) {
        super(context);
        this.context = context;
        this.spaceballView = spaceballView;
        this.iconResId = iconResId;
        this.backgroundColorResId = backgroundColorResId;
        buttonId = viewId;
        init();
    }

    public SpaceballBaseItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpaceballBaseItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SpaceballBaseItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        if (imageButton == null)
            init();
        imageButton.setOnClickListener(l);
    }

    private void init() {
        imageButton = buildImageButton();
        setClipChildren(false);
        redraw();
    }

    private ImageButton buildImageButton() {
        ImageButton imageButton = new ImageButton(this.context);
        LayoutParams frameParams = new LayoutParams((int) getResources().getDimension(R.dimen.spaceball_button_item_width_height),
                (int) getResources().getDimension(R.dimen.spaceball_button_item_width_height));
        frameParams.gravity = Gravity.CENTER;
        imageButton.setLayoutParams(frameParams);
        imageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageButton.setId(buttonId);
        addView(imageButton);
        return imageButton;
    }

    public ImageButton getImageButton() {
        return imageButton;
    }

    protected List<ObjectAnimator> getAnimators(long delay, boolean entrance) {

        float start = entrance ? 0.0f : 1.0f;
        float end = entrance ? 1.0f : 0.0f;

        ObjectAnimator alpha = ObjectAnimator.ofFloat(this, "alpha", start, end);
        alpha.setStartDelay(delay);
        alpha.setDuration(250);

        AppScaleInterpolator.ControlPoint[] controlPoints = new AppScaleInterpolator.ControlPoint[] {
            new AppScaleInterpolator.ControlPoint(0, 0.8f),
            new AppScaleInterpolator.ControlPoint(0.5f, 1.1f),
            new AppScaleInterpolator.ControlPoint(0.7f, 0.95f),
            new AppScaleInterpolator.ControlPoint(1.0f, 1.0f)
        };

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageButton, "scaleX", start, end);
        scaleX.setStartDelay(delay);
        scaleX.setInterpolator(new AppScaleInterpolator(controlPoints));
        scaleX.setDuration(500);

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageButton, "scaleY", start, end);
        scaleY.setStartDelay(delay);
        scaleY.setInterpolator(new AppScaleInterpolator(controlPoints));
        scaleY.setDuration(500);

        if (entrance)
            return Arrays.asList(alpha, scaleX, scaleY);
        else
            return Collections.singletonList(alpha); // TODO This needs to reverse the scale animation too
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        imageButton.setEnabled(enabled);
    }

    private void redraw() {
        setAppVisibility();
        imageButton.setImageDrawable(getIcon());
        imageButton.setBackground(getSpaceballDrawable());
    }

    public void setAppVisibility() {
        setVisibility(VISIBLE);
    }

    private Drawable getIcon() {
        return ContextCompat.getDrawable(getContext(), iconResId);
    }

    private Drawable getSpaceballDrawable() {
        return ImageUtils.generateCircleButton(getContext(), getBaseColor());
    }

    @ColorRes
    private int getBaseColor() {
        if (this.backgroundColorResId > 0) {
            return this.backgroundColorResId;
        }
        return R.color.gray_dark_2;
    }
}
