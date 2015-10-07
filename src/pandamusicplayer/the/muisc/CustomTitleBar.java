package pandamusicplayer.the.muisc;
import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;

public class CustomTitleBar {
	
	public static int number;

	/**
	 * @see [自定义标题栏]
	 * @param activity
	 * @param title
	 */
	public static void getTitleBar(Activity activity,String title) {
		activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		activity.setContentView(R.layout.custom_title);
		activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);
		TextView textView = (TextView) activity.findViewById(R.id.head_center_text);
		textView.setText(title);
	}
    public static void setWelcomePage(Activity activity){
    	activity.setContentView(R.layout.custom_title);
    	try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
