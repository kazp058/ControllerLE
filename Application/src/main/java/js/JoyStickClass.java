package js;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.widget.RelativeLayout;


import com.example.android.bluetoothlegatt.Joystick_def;

public class JoyStickClass extends Joystick_def {

    public JoyStickClass(Context applicationContext, RelativeLayout layout_joystick, int image_button) {

        mContext = applicationContext;

        stick = BitmapFactory.decodeResource(mContext.getResources(), image_button);

        stick_width = stick.getWidth();
        stick_height = stick.getHeight();

        draw = new DrawCanvas(mContext);
        paint = new Paint();
        mLayout = layout_joystick;
        params = mLayout.getLayoutParams();
    }
}
