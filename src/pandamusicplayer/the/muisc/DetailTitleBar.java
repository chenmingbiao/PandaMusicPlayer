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

public class DetailTitleBar {

	/**
	 * @see [自定义标题栏]
	 * @param activity
	 * @param title
	 */
	public static void getTitleBar(Activity activity,String title) {
		activity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		activity.setContentView(R.layout.detail_title);
		activity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.detail_title);
		TextView textView = (TextView) activity.findViewById(R.id.tv_songname);
		textView.setText(title);
	}
    public static void setWelcomePage(Activity activity){
    	activity.setContentView(R.layout.detail_title);
    	try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
