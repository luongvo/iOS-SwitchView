package vn.luongvo.widget.iosswitchview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

/**
 * @author luongvo
 */

public class SwitchView extends View {

    private final int DEFAULT_COLOR_ON = 0xff53d769;
    private final int DEFAULT_COLOR_OFF = 0xffe3e3e3;

    private int colorOn = DEFAULT_COLOR_ON;
    private int colorOff = DEFAULT_COLOR_OFF;

    private final Paint paint = new Paint();
    private final Path sPath = new Path();
    private final Path bPath = new Path();
    private final RectF bRectF = new RectF();
    private float sAnim, bAnim;
    private RadialGradient shadowGradient;
    private final AccelerateInterpolator aInterpolator = new AccelerateInterpolator(2);

    /**
     * state switch on
     */
    public static final int STATE_SWITCH_ON = 4;
    /**
     * state prepare to off
     */
    public static final int STATE_SWITCH_ON2 = 3;
    /**
     * state prepare to on
     */
    public static final int STATE_SWITCH_OFF2 = 2;
    /**
     * state switch off
     */
    public static final int STATE_SWITCH_OFF = 1;
    /**
     * current state
     */
    private int state = STATE_SWITCH_OFF;
    /**
     * last state
     */
    private int lastState = state;

    private boolean isChecked = false;

    private int mWidth, mHeight;
    private float sWidth, sHeight;
    private float sLeft, sTop, sRight, sBottom;
    private float sCenterX, sCenterY;
    private float sScale;

    private float bOffset;
    private float bRadius, bStrokeWidth;
    private float bWidth;
    private float bLeft, bTop, bRight, bBottom;
    private float bOnLeftX, bOn2LeftX, bOff2LeftX, bOffLeftX;

    private float shadowHeight;

    private OnCheckedChangeListener onCheckedChangeListener;

    public SwitchView(Context context) {
        this(context, null);
    }

    public SwitchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.app);
        try {
            colorOn = a.getColor(R.styleable.app_color_on, DEFAULT_COLOR_ON);
            colorOff = a.getColor(R.styleable.app_color_off, DEFAULT_COLOR_OFF);
            isChecked = a.getBoolean(R.styleable.app_checked, false);

            state = isChecked ? STATE_SWITCH_ON : STATE_SWITCH_OFF;
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = (int) (widthSize * 0.65f);
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;

        sLeft = sTop = 0;
        sRight = mWidth;
        sBottom = mHeight * 0.91f;
        sWidth = sRight - sLeft;
        sHeight = sBottom - sTop;
        sCenterX = (sRight + sLeft) / 2;
        sCenterY = (sBottom + sTop) / 2;

        shadowHeight = mHeight - sBottom;

        bLeft = bTop = 0;
        bRight = bBottom = sBottom;
        bWidth = bRight - bLeft;
        final float halfHeightOfS = (sBottom - sTop) / 2;
        bRadius = halfHeightOfS * 0.95f;
        bOffset = bRadius * 0.2f;
        bStrokeWidth = (halfHeightOfS - bRadius) * 2;

        bOnLeftX = sWidth - bWidth;
        bOn2LeftX = bOnLeftX - bOffset;
        bOffLeftX = 0;
        bOff2LeftX = 0;

        sScale = 1 - bStrokeWidth / sHeight;

        RectF sRectF = new RectF(sLeft, sTop, sBottom, sBottom);
        sPath.arcTo(sRectF, 90, 180);
        sRectF.left = sRight - sBottom;
        sRectF.right = sRight;
        sPath.arcTo(sRectF, 270, 180);
        sPath.close();

        bRectF.left = bLeft;
        bRectF.right = bRight;
        bRectF.top = bTop + bStrokeWidth / 2;
        bRectF.bottom = bBottom - bStrokeWidth / 2;

        shadowGradient = new RadialGradient(bWidth / 2, bWidth / 2, bWidth / 2, 0xff000000, 0x00000000, Shader.TileMode.CLAMP);
    }

    private void calcBPath(float percent) {
        bPath.reset();
        bRectF.left = bLeft + bStrokeWidth / 2;
        bRectF.right = bRight - bStrokeWidth / 2;
        bPath.arcTo(bRectF, 90, 180);
        bRectF.left = bLeft + percent * bOffset + bStrokeWidth / 2;
        bRectF.right = bRight + percent * bOffset - bStrokeWidth / 2;
        bPath.arcTo(bRectF, 270, 180);
        bPath.close();
    }

    private float calcBTranslate(float percent) {
        float result = 0;
        int switchState = state - lastState;
        switch (switchState) {
            case 1:
                // off -> off2
                if (state == STATE_SWITCH_OFF2) {
                    result = bOff2LeftX - (bOff2LeftX - bOffLeftX) * percent;
                }
                // on2 -> on
                else if (state == STATE_SWITCH_ON) {
                    result = bOnLeftX - (bOnLeftX - bOn2LeftX) * percent;
                }
                break;
            case 2:
                // off2 -> on
                if (state == STATE_SWITCH_ON) {
                    result = bOnLeftX - (bOnLeftX - bOff2LeftX) * percent;
                }
                // off -> on2
                else if (state == STATE_SWITCH_ON) {
                    result = bOn2LeftX - (bOn2LeftX - bOffLeftX) * percent;
                }
                break;
            case 3: // off -> on
                result = bOnLeftX - (bOnLeftX - bOffLeftX) * percent;
                break;
            case -1:
                // on -> on2
                if (state == STATE_SWITCH_ON2) {
                    result = bOn2LeftX + (bOnLeftX - bOn2LeftX) * percent;
                }
                // off2 -> off
                else if (state == STATE_SWITCH_OFF) {
                    result = bOffLeftX + (bOff2LeftX - bOffLeftX) * percent;
                }
                break;
            case -2:
                // on2 -> off
                if (state == STATE_SWITCH_OFF) {
                    result = bOffLeftX + (bOn2LeftX - bOffLeftX) * percent;
                }
                // on -> off2
                else if (state == STATE_SWITCH_OFF2) {
                    result = bOff2LeftX + (bOnLeftX - bOff2LeftX) * percent;
                }
                break;
            case -3: // on -> off
                result = bOffLeftX + (bOnLeftX - bOffLeftX) * percent;
                break;
        }

        return result - bOffLeftX;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setAntiAlias(true);
        final boolean isChecked = (state == STATE_SWITCH_ON || state == STATE_SWITCH_ON2);
        // draw background
        paint.setStyle(Style.FILL);
        paint.setColor(isChecked ? colorOn : colorOff);
        canvas.drawPath(sPath, paint);

        sAnim = sAnim - 0.1f > 0 ? sAnim - 0.1f : 0;
        bAnim = bAnim - 0.1f > 0 ? bAnim - 0.1f : 0;

        final float dsAnim = aInterpolator.getInterpolation(sAnim);
        final float dbAnim = aInterpolator.getInterpolation(bAnim);
        // draw background animation
        final float scale = sScale * (isChecked ? dsAnim : 1 - dsAnim);
        final float scaleOffset = (bOnLeftX + bRadius - sCenterX) * (isChecked ? 1 - dsAnim : dsAnim);
        canvas.save();
        canvas.scale(scale, scale, sCenterX + scaleOffset, sCenterY);
        paint.setColor(0xffffffff);
        canvas.drawPath(sPath, paint);
        canvas.restore();
        // draw center bar
        canvas.save();
        canvas.translate(calcBTranslate(dbAnim), shadowHeight);
        final boolean isState2 = (state == STATE_SWITCH_ON2 || state == STATE_SWITCH_OFF2);
        calcBPath(isState2 ? 1 - dbAnim : dbAnim);
        // draw shadow
        paint.setStyle(Style.FILL);
        paint.setColor(0xff333333);
        paint.setShader(shadowGradient);
        canvas.drawPath(bPath, paint);
        paint.setShader(null);
        canvas.translate(0, -shadowHeight);

        canvas.scale(0.98f, 0.98f, bWidth / 2, bWidth / 2);
        paint.setStyle(Style.FILL);
        paint.setColor(0xffffffff);
        canvas.drawPath(bPath, paint);

        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(bStrokeWidth * 0.5f);

        paint.setColor(isChecked ? colorOn : colorOff);
        canvas.drawPath(bPath, paint);

        canvas.restore();

        paint.reset();
        if (sAnim > 0 || bAnim > 0) invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ((state == STATE_SWITCH_ON || state == STATE_SWITCH_OFF) && (sAnim * bAnim == 0) && isEnabled()) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    return true;
                case MotionEvent.ACTION_UP:
                    lastState = state;
                    if (state == STATE_SWITCH_OFF) {
                        refreshState(STATE_SWITCH_OFF2);
                    } else if (state == STATE_SWITCH_ON) {
                        refreshState(STATE_SWITCH_ON2);
                    }
                    bAnim = 1;
                    invalidate();

                    if (state == STATE_SWITCH_OFF2) {
                        toggle(STATE_SWITCH_ON);
                    } else if (state == STATE_SWITCH_ON2) {
                        toggle(STATE_SWITCH_OFF);
                    }

                    if (onCheckedChangeListener != null) {
                        onCheckedChangeListener.onCheckedChanged(this, isChecked);
                    }
                    break;
            }
        }
        return super.onTouchEvent(event);
    }

    private void refreshState(int newState) {
        if (!isChecked && newState == STATE_SWITCH_ON) {
            isChecked = true;
        } else if (isChecked && newState == STATE_SWITCH_OFF) {
            isChecked = false;
        }
        lastState = state;
        state = newState;
        postInvalidate();
    }

    /**
     * @return the state of switch view
     */
    public boolean isChecked() {
        return isChecked;
    }

    /**
     * if set true , the state change to on;
     * if set false, the state change to off
     *
     * @param isChecked
     */
    public void setChecked(boolean isChecked) {
        refreshState(isChecked ? STATE_SWITCH_ON : STATE_SWITCH_OFF);
    }

    /**
     * if set true , the state change to on;
     * if set false, the state change to off
     * <br><b>change state with animation</b>
     *
     * @param isChecked
     */
    public void toggle(final boolean isChecked) {
        this.isChecked = isChecked;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                toggle(isChecked ? STATE_SWITCH_ON : STATE_SWITCH_OFF);
            }
        }, 300);
    }

    private synchronized void toggle(int switchState) {
        if (switchState == STATE_SWITCH_ON || switchState == STATE_SWITCH_OFF) {
            if ((switchState == STATE_SWITCH_ON && (lastState == STATE_SWITCH_OFF || lastState == STATE_SWITCH_OFF2))
                    || (switchState == STATE_SWITCH_OFF && (lastState == STATE_SWITCH_ON || lastState == STATE_SWITCH_ON2))) {
                sAnim = 1;
            }
            bAnim = 1;
            refreshState(switchState);
        }
    }

    public interface OnCheckedChangeListener {
        void onCheckedChanged(SwitchView switchView, boolean isChecked);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.isChecked = isChecked;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.isChecked = ss.isChecked;
        this.state = this.isChecked ? STATE_SWITCH_ON : STATE_SWITCH_OFF;
    }

    static final class SavedState extends BaseSavedState {
        private boolean isChecked;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            isChecked = 1 == in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isChecked ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
