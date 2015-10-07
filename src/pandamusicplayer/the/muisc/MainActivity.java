package pandamusicplayer.the.muisc;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pandamusicplayer.the.domain.Music;
import pandamusicplayer.the.muisc.MusicService.CommandReceiver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	//显示组件
	private ImageButton imgBtn_Previous;        //上一个
	private ImageButton imgBtn_PlayOrPause; //开始或暂停
	private ImageButton imgBtn_Stop;             //停止
	private ImageButton imgBtn_Next;             //下一个
	private ListView list;
	private TextView text_Current;
	private TextView text_Duration; 
	private TextView song_Name;
	private Button upDetailBtn;
	//private SeekBar seekBar;
	private ProgressBar progressBar;
	//更新进度条的Handler
	//private Handler seekBarHandler;
	private Handler progressBarHandler;
	//当前歌曲的持续时间和当前位置，作用于进度条
	private int duration;
	private int time;
	//进度条控制常量
	private static final int PROGRESS_INCREASE = 0;
	private static final int PROGRESS_PAUSE = 1;
	private static final int PROGRESS_RESET = 2;
	//存放Music的线性表
	private List<Music> list_Music;
	//当前歌曲的序号，从1开始
	private int number;
	//player
	private MediaPlayer player;
	//播放状态
    private int status;
    //广播接收器
  	private StatusChangedReceiver receiver;
  	//标志位
  	boolean flag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//自定义标题栏显示，并显示UI
		//CustomTitleBar. setWelcomePage(this);
		CustomTitleBar. getTitleBar(this,"全部歌曲"); 
		setContentView(R.layout.main);
		//初始化播放的歌曲序号
		number = 0;
		//初始化当然音乐播放器的状态
		status = MusicService.STATUS_STOPPED;
		//初始化设置进度条的进度
		time = 0;
		duration = 0;
		//获取显示组件
		findViews();
		//为显示组件注册监听器
		registerListeners();
		 //开始Service
		startService(new Intent(this, MusicService.class));
		//绑定广播接收器
		bindStatusChangedReceiver();
		//检查播放器是否正在播放，如果正在播放，以上绑定的接收器会改变UI
		sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		//标志位
		flag = false;
		//初始化进度条
		//intitSeekBarHandler();
		intitProgressBarHandler();
	}
	
	//绑定广播接收器
	private void bindStatusChangedReceiver(){
		receiver = new StatusChangedReceiver();
		IntentFilter filter = new IntentFilter(
				MusicService.BROADCAST_MUSICSERVICE_UPDATE_STATUS);
		registerReceiver(receiver,filter);
	}
	
	//获取显示组件
	private void findViews(){
		imgBtn_Previous = (ImageButton)findViewById(R.id.btnPrevious_player);
		imgBtn_PlayOrPause = (ImageButton)findViewById(R.id.btnPlay_player);
		imgBtn_Next = (ImageButton)findViewById(R.id.btnNext_player);
		list = (ListView)findViewById(R.id.listView1);
		text_Current = (TextView)findViewById(R.id.list_song_current);
		text_Duration =  (TextView)findViewById(R.id.list_song_duration);
		//seekBar = (SeekBar)findViewById(R.id.progressBar1);	
		progressBar = (ProgressBar)findViewById(R.id.progressBar1);
		song_Name = (TextView)findViewById(R.id.list_song_name);
		upDetailBtn = (Button) findViewById(R.id.updetail);
	}

	//为显示组件注册监听器
	private void registerListeners(){
		imgBtn_Previous.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				//发送上一个的命令给Service
				sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
				if(flag == false) flag = true;
				//seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				progressBarHandler.sendEmptyMessage(PROGRESS_RESET);
			}
		});
		imgBtn_PlayOrPause.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				//发送播放或暂停的命令给Service
				if(isPlaying()){
					sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
					if(flag == false) flag = true;
				}else if(isPaused()){
					sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
					if(flag == false) flag = true;
				}else if(isStopped()){
					sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
					if(flag == false) flag = true;
					progressBarHandler.sendEmptyMessage(PROGRESS_INCREASE);
				}
			}
		});
		imgBtn_Next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				//发送下一个的命令给Service
				sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
				if(flag == false) flag = true;
				//seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				progressBarHandler.sendEmptyMessage(PROGRESS_RESET);
			}
		});
		list.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				if(flag == false) flag = true;
				number = arg2;
				sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
				//seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				progressBarHandler.sendEmptyMessage(PROGRESS_RESET);
			}
		});
		upDetailBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this,DetailActivity.class);
				startActivity(intent);
				CustomTitleBar.number = number;
			}	
		});
	}
	
	//是否正在播放音乐
	private boolean isPlaying(){
		return status == MusicService.STATUS_PLAYING;
	}
	
	//是否暂停播放音乐
	private boolean isPaused(){
		return status == MusicService.STATUS_PAUSED;
	}
	
	//是否停止状态
	private boolean isStopped(){
		return status == MusicService.STATUS_STOPPED;
	}
	
	//设置播放标志
	private void setViewVisibility(int number){
		for(int i=0;i<list.getCount();i++)
		{
			TextView music_line=(TextView)list.getChildAt(i).findViewById(R.id.divider_line);
			if(i != number){
				music_line.setVisibility(View.INVISIBLE);
			} else{	
				music_line.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		number = CustomTitleBar.number;
		//Toast.makeText(this,""+number, Toast.LENGTH_SHORT).show();
		sendBroadcastOnCommand(MusicService.COMMAND_TELL_ME_NUMBER);
		//初试话音乐列表
		initMusicList();
		//如果列表没有歌曲，则播放按钮不可用，并提醒用户
		if(list.getCount() == 0){
			imgBtn_Previous.setEnabled(false);
			imgBtn_PlayOrPause.setEnabled(false);
			//imgBtn_Stop.setEnabled(false);
			imgBtn_Next.setEnabled(false);
			Toast.makeText(this, this.getString(R.string.tip_no_music_file), Toast.LENGTH_SHORT).show();
		}else{
			imgBtn_Previous.setEnabled(true);
			imgBtn_PlayOrPause.setEnabled(true);
			//imgBtn_Stop.setEnabled(true);
			imgBtn_Next.setEnabled(true);
		}
	}
	
	//初始化音乐列表，包括获取音乐集合更新显示列表
	private void initMusicList(){
		GetMusicCursor();
		setListContent();
	}
	
	//更新列表内容
	private void setListContent(){
    	MusicItemAdapter adapter=new MusicItemAdapter();
    	list.setAdapter(adapter);
	}
	
	//获取系统扫描得到的音乐媒体集
	private void GetMusicCursor(){
		 list_Music = new ArrayList<Music>();
		//获取数据选择器
		ContentResolver resolver = getApplication().getContentResolver();
		//选择音乐媒体集
		Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
		if (cursor.moveToFirst()) {
			do {
				Music m = new Music();
				String title = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.TITLE));
				String singer = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ARTIST));
				if ("<unknown>".equals(singer)) {
					singer = "未知艺术家";
				}
				String album = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.ALBUM));
				long size = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media.SIZE));
				long time = cursor.getLong(cursor
						.getColumnIndex(MediaStore.Audio.Media.DURATION));
				String url = cursor.getString(cursor
						.getColumnIndex(MediaStore.Audio.Media.DATA));
				m.setTitle(title);
				m.setSinger(singer);
				m.setAlbum(album);
				m.setSize(size);
				m.setTime(time);
				m.setUrl(url);
				list_Music.add(m);
			} while (cursor.moveToNext());
		}
	}
	
	//移动到下一首
	private void moveNumberToNext(){
		//判断是否到达了列表低端
		if((number +1 ) >= list.getCount()){
			number = 0;
			Toast.makeText(MainActivity. this,
					MainActivity.this.getString(R.string.tip_reach_bottom) ,
					Toast.LENGTH_SHORT).show();
		} else {
			++number;
		}
	}
	
	//移动到上一首
	private void moveNumberToPrevious(){
		//判断是否到达了列表顶端
		if((number ==0) ){
			number = list.getCount()-1;
			Toast.makeText(MainActivity. this,
					MainActivity.this.getString(R.string.tip_reach_top) ,
					Toast.LENGTH_SHORT).show();
		} else {
			--number;
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	//读取音乐文件
	public void load(int number){
		//之前的资源不用了，释放掉
		if(player != null){
			player.release();
			player = null;
		}
		Music m = list_Music.get(number);
		String url = m.getUrl();
		Uri myUri = Uri.parse(url);

		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
		try {
			player.setDataSource(getApplicationContext(), myUri);
			player.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//播放音乐
	public void play(int number){
		//停止当前播放
		if(player.isPlaying()){
			player.stop();
		}
		load(number);
		player.start();
	}

	//暂停音乐
	public void pause(){
		if(player.isPlaying()){
			player.pause();
		}
	}
	
	//停止播放音乐
	public void stop(){
		if(player != null){
			player.stop();
		}
	}
	
	//恢复播放（暂停后）
	public void resume(){
		player.start();
	}
	
	//重新播放（播放完成后）
	public void replay(){
		player.start();
	}
	
	 private class MusicItemAdapter extends BaseAdapter{
			@Override
			public int getCount() {
				return list_Music.size();
			}
			@Override
			public Object getItem(int arg0) {
				return list_Music.get(arg0);
			}
			@Override
			public long getItemId(int position) {
				return position;
			}
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// TODO Auto-generated method stub
				if(convertView==null){
					convertView=LayoutInflater.from(getApplicationContext()).inflate(R.layout.musiclist, null);
				}
				Music m=list_Music.get(position);
				TextView textName=(TextView) convertView.findViewById(R.id.music_list_singer);
				textName.setText(m.getSinger());
				TextView music_singer=(TextView) convertView.findViewById(R.id.music_list_name);
				music_singer.setText(m.getTitle());
				TextView music_time=(TextView) convertView.findViewById(R.id.music_list_time);
				music_time.setText(getDateToString(m.getTime()));
				TextView music_line=(TextView)convertView.findViewById(R.id.divider_line);
				if(number == position && flag==true)
				{
					music_line.setVisibility(View.VISIBLE);
				}  else {
					music_line.setVisibility(View.INVISIBLE);
				}
				return convertView;
			}
	    }
       //时间戳转换成时间（分：秒）
	    public static String getDateToString(long time) {
	    	SimpleDateFormat sf;
	         Date d = new Date(time);
	         sf = new SimpleDateFormat("mm:ss");
	         return sf.format(d);
	     }
	    
	    //发送命令，控制音乐播放。参数定义在MusicService类中
	    private void sendBroadcastOnCommand(int command){
	    	Intent intent = new Intent(MusicService.BROADCAST_MUSICSERVICE_CONTROL);
	    	intent.putExtra("command", command);
	    	//根据不同命令，封装不同数据
	    	switch (command) {
	    	case MusicService.COMMAND_PLAY:
	    		intent.putExtra("number", number);
	    		break;
	    	case MusicService.COMMAND_PREVIOUS:
	    		moveNumberToPrevious();
	    		intent.putExtra("number", number);
	    		break;
	    	case MusicService.COMMAND_NEXT:
	    		moveNumberToNext();
	    		intent.putExtra("number", number);
	    		break;
	    	case MusicService.COMMAND_SEEK_TO:
	    		intent.putExtra("time", time);
	    		break;
	    	case MusicService.COMMAND_PAUSE:
	    	case MusicService.COMMAND_STOP:
	    	case MusicService.COMMAND_RESUME:
	    	default:
	    			break;
	    	}
	    	sendBroadcast(intent);
	    }
	    
	  //内部类，用于播放器状态更新的接收广播
	    class StatusChangedReceiver extends BroadcastReceiver{
			@Override
			public void onReceive(Context context, Intent intent) {
				//获取播放器状态
				status = intent.getIntExtra("status", -1);
				//如果播放结束了，播放下一曲
				switch (status){
				case MusicService.STATUS_PLAYING:
					time = intent.getIntExtra("time", 0);
					duration = intent.getIntExtra("duration", 0);
					//seekBarHandler.removeMessages(PROGRESS_INCREASE);
					progressBarHandler.removeMessages(PROGRESS_INCREASE);
					//seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
					progressBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
					//seekBar.setMax(duration);
					progressBar.setMax(duration);
					//seekBar.setProgress(time);
					progressBar.setMax(duration);
					Music m = list_Music.get(number);
					song_Name.setText(m.getTitle());
					text_Duration.setText(formatTime(duration));
					imgBtn_PlayOrPause.setImageDrawable(getResources().getDrawable(R.drawable.desktop_pausebt));
					break;
				case MusicService.STATUS_PAUSED:
					//seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
					progressBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
					imgBtn_PlayOrPause.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt));
					break;
				case MusicService.STATUS_COMPLETED:
					//seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
					progressBarHandler.sendEmptyMessage(PROGRESS_RESET);
					sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
					imgBtn_PlayOrPause.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt));
					break;
				case MusicService.RETURNBACK:
					number = intent.getIntExtra("number", 0);
					break;
				default:
					break;
				}
				updateUI(status);
				setViewVisibility(number);
			}
			//根据播放器的播放状态，更新UI
		    private void updateUI(int status){
		    	switch(status){
		    	case MusicService.STATUS_PLAYING:
		    		imgBtn_PlayOrPause.setImageDrawable(getResources().getDrawable(R.drawable.desktop_pausebt));
		    		break;
		    	case MusicService.STATUS_PAUSED:
		    	case MusicService.STATUS_STOPPED:
		    	case MusicService.STATUS_COMPLETED:
		    		imgBtn_PlayOrPause.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt));
		    		break;
		    	default:
		    		break;
		    	}
		    }
	    }
	    
	    @Override
	    protected void onDestroy(){
	    	if (isStopped()){
	    		stopService(new Intent(this, MusicService.class));
	    	}
	    	super.onDestroy();
	    }
	    
	    //格式化：毫秒 --> "mm:ss"
	    private String formatTime(int msec){
	    	int minute = (msec / 1000) / 60;
	    	int second = (msec / 1000) % 60;
	    	String minuteString;
	    	String secondString;
	    	if (minute < 10) {
	    		minuteString = "0" + minute;
	    	} else {
	    		minuteString = "" + minute;
	    	}
	    	if (second < 10) {
	    		secondString = "0" + second;
	    	} else {
	    		secondString = "" + second;
	    	}
	    	return minuteString + ":" + secondString;
	    }
	    
	    /*
	    //初始化进度条
	    private void intitSeekBarHandler(){
	    	seekBarHandler = new Handler(){
	    		public void handleMessage(Message msg){
	    			super.handleMessage(msg);
	    			switch (msg.what) {
	    			case PROGRESS_INCREASE:
	    				if (seekBar.getProgress() < duration) {
	    					//进度条前进1秒
	    					seekBar.incrementProgressBy(1000);
	    					seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
	    					//修改显示当前进度的文本
	    					text_Current.setText(formatTime(time));
	    					time += 1000;
	    				}
	    				break;
	    			case PROGRESS_PAUSE:
	    				seekBarHandler.removeMessages(PROGRESS_INCREASE);
	    				break;
	    			case PROGRESS_RESET:
	    				//重置进度条界面
	    				seekBarHandler.removeMessages(PROGRESS_INCREASE);
	    				seekBar.setProgress(0);
	    				text_Current.setText("00:00");
	    				break;
	    			}
	    		}
	    	};
	    }*/
	    
	    //初始化进度条
	    private void intitProgressBarHandler(){
	    	progressBarHandler = new Handler(){
	    		public void handleMessage(Message msg){
	    			super.handleMessage(msg);
	    			switch (msg.what) {
	    			case PROGRESS_INCREASE:
	    				if (progressBar.getProgress() < duration) {
	    					//进度条前进1秒
	    					progressBar.incrementProgressBy(1000);
	    					progressBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
	    					//修改显示当前进度的文本
	    					text_Current.setText(formatTime(time));
	    					time += 1000;
	    				}
	    				break;
	    			case PROGRESS_PAUSE:
	    				progressBarHandler.removeMessages(PROGRESS_INCREASE);
	    				break;
	    			case PROGRESS_RESET:
	    				//重置进度条界面
	    				progressBarHandler.removeMessages(PROGRESS_INCREASE);
	    				progressBar.setProgress(0);
	    				text_Current.setText("00:00");
	    				break;
	    			}
	    		}
	    	};
	    }
}





