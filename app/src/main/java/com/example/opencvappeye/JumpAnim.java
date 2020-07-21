package com.example.opencvappeye;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;

public class JumpAnim {
    private TranslateAnimation upAnim;
    private TranslateAnimation downAnim;
    private boolean running = false;
    private MListener listener;

    private int step;
    private  int faileY;
    private View obj;

    JumpAnim(View _obj, int _step, int _duration, int _faileY){
        this.step = _step;
        this.faileY = _faileY;
        this.obj = _obj;

        upAnim = new TranslateAnimation(0, 0, 0, -step);
        upAnim.setDuration(_duration);
        upAnim.setInterpolator(new DecelerateInterpolator());
        downAnim = null;
        upAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                running = true;
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                obj.layout(obj.getLeft(),
                        obj.getTop()-step,
                        obj.getRight(),
                        obj.getBottom()-step);
                obj.clearAnimation();
                obj.startAnimation(downAnim);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });


        downAnim = new TranslateAnimation(0, 0, 0, step);
        downAnim.setDuration(_duration);
        downAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                running = false;
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                obj.layout(obj.getLeft(),
                        obj.getTop()+step,
                        obj.getRight(),
                        obj.getBottom()+step);
                obj.clearAnimation();

                if (viewHea()){
                    ListenerStart();
                    return;
                }
                obj.startAnimation(animation);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    void setListener(MListener listener) {
        this.listener = listener;
    }

    private void ListenerStart() {
        if (listener != null) {
            listener.onEnd();
        }
    }

    void startAnim(){
        if (!running)
            obj.startAnimation(upAnim);
    }

    public void stopAnim(){
        obj.clearAnimation();
    }

    private boolean viewHea(){
        int[] pos = new int[2];
        obj.getLocationOnScreen(pos);

        Log.i("info", "XXX : Y：" + pos[1] +  " X：" + pos[0] );

        return pos[1] <= 0 || pos[1] >= faileY;
    }

}
