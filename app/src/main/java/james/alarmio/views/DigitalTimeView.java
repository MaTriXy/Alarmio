package james.alarmio.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;

import com.afollestad.aesthetic.Aesthetic;

import java.lang.ref.WeakReference;
import java.util.Calendar;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import james.alarmio.utils.FormatUtils;

public class DigitalTimeView extends BaseSubscriptionView implements ViewTreeObserver.OnGlobalLayoutListener {

    private Paint paint;

    private Disposable textColorPrimarySubscription;

    public DigitalTimeView(Context context) {
        this(context, null, 0);
    }

    public DigitalTimeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DigitalTimeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.CENTER);

        subscribe();

        new UpdateThread(this).start();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    public void subscribe() {
        textColorPrimarySubscription = Aesthetic.get()
                .textColorPrimary()
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) throws Exception {
                        paint.setColor(integer);
                        invalidate();
                    }
                });
    }

    @Override
    public void unsubscribe() {
        textColorPrimarySubscription.dispose();
    }

    @Override
    public void onGlobalLayout() {
        paint.setTextSize(48f);
        Rect bounds = new Rect();
        paint.getTextBounds("00:00:00", 0, 8, bounds);
        paint.setTextSize((48f * getMeasuredWidth()) / (bounds.width() * 2));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawText(FormatUtils.format(getContext(), Calendar.getInstance().getTime()), canvas.getWidth() / 2, (canvas.getHeight() - paint.ascent()) / 2, paint);
    }

    private static class UpdateThread extends Thread {

        private WeakReference<DigitalTimeView> viewReference;

        private UpdateThread(DigitalTimeView view) {
            viewReference = new WeakReference<>(view);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        DigitalTimeView view = viewReference.get();
                        if (view != null)
                            view.invalidate();
                    }
                });
            }
        }
    }
}
