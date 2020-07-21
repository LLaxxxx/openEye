package com.example.opencvappeye;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

class MoveLeftAnim {

    private View back;
    private View fore;
    private int duration = 0;
    private TranslateAnimation anim;

    private MListener listener;
    private boolean running = false;

    MoveLeftAnim(View _back, View _fore, int _step, int _duration){
        back = _back;
        fore = _fore;
        duration = _duration;

        anim = new TranslateAnimation(0, -_step, 0, 0);
        anim.setDuration(_duration);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                running = false;
                ListenerStart();
                //结束
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
        if (!running){
            back.startAnimation(anim);
            fore.startAnimation(anim);
            running = true;
        }
    }

    void randPos(int startX,int width){
        fore.layout(startX,
                fore.getTop(),
                startX + width,
                fore.getBottom());
    }

    void clearAnim(){
        if (running){
            back.clearAnimation();
            fore.clearAnimation();
            running = false;
        }
    }
}
