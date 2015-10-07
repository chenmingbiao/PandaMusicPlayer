package pandamusicplayer.the.muisc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import pandamusicplayer.the.domain.Music;
import pandamusicplayer.the.muisc.MusicService.CommandReceiver;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class DetailActivity extends Activity {
	
	//显示组件
	private Button Btn_back;   
	private SeekBar seekBar;
	private TextView tv_Text_Current;
	private TextView tv_Text_Duration; 
	private TextView tv_Song_Name;
	private TextView tv_Singer;
	private ImageButton tv_Play;
	private ImageButton tv_Previous;
	private ImageButton tv_Next;
	
	//当前歌曲的序号，从1开始
	private int number;
	
	//播放状态
    private int status;
    
	//存放Music的线性表
	private List<Music> list_Music;
   
	//更新进度条的Handler
	private Handler seekBarHandler;
	//当前歌曲的持续时间和当前位置，作用于进度条
	private int duration;
	private int time;
	
	//存放音乐总数目
	private int sumMuisc;
	
	//广播接收器
  	private StatusChangedReceiver receiver;
	
	//进度条控制常量
	private static final int PROGRESS_INCREASE = 0;
	private static final int PROGRESS_PAUSE = 1;
	private static final int PROGRESS_RESET = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//自定义标题栏显示，并显示UI
		DetailTitleBar. getTitleBar(this,""); 
		setContentView(R.layout.playlayout);
		//获取显示组件
		findViews();
		//为显示组件注册监听器
		registerListeners();
		//检查播放器是否正在播放，如果正在播放，以上绑定的接收器会改变UI
		sendBroadcastOnCommand(MusicService.COMMAND_CHECK_IS_PLAYING);
		//绑定广播接收器
		bindStatusChangedReceiver();
		//初始化进度条
		intitSeekBarHandler();
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
			Btn_back = (Button)findViewById(R.id.btnback);
			tv_Text_Current = (TextView)findViewById(R.id.tv_Text_Current);
			tv_Text_Duration =  (TextView)findViewById(R.id.tv_Text_Duration);
			tv_Song_Name =  (TextView)findViewById(R.id.tv_songname);
			tv_Singer =  (TextView)findViewById(R.id.tv_singer);
			tv_Play = (ImageButton)findViewById(R.id.tv_play);
			tv_Previous = (ImageButton)findViewById(R.id.tv_previous);
			tv_Next = (ImageButton)findViewById(R.id.tv_next);
			seekBar = (SeekBar)findViewById(R.id.my_seekbar);
		}

		//为显示组件注册监听器
		private void registerListeners(){
			Btn_back.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent = new Intent();
					intent.setClass(DetailActivity.this,MainActivity.class);
					startActivity(intent);
				   CustomTitleBar.number  = number;
				}
			});
			tv_Previous.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					//发送上一个的命令给Service
					sendBroadcastOnCommand(MusicService.COMMAND_PREVIOUS);
					seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
				}
			});
			tv_Play.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					//发送播放或暂停的命令给Service
					if(isPlaying()){
						sendBroadcastOnCommand(MusicService.COMMAND_PAUSE);
					}else if(isPaused()){
						sendBroadcastOnCommand(MusicService.COMMAND_RESUME);
					}else if(isStopped()){
						sendBroadcastOnCommand(MusicService.COMMAND_PLAY);
						seekBarHandler.sendEmptyMessage(PROGRESS_INCREASE);
					}
				}
			});
			tv_Next.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					//发送下一个的命令给Service
					sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
					seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
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
	    
	    @Override
		protected void onResume(){
			super.onResume();	 
			number = CustomTitleBar.number ;
			//Toast.makeText(this, ""+number, Toast.LENGTH_SHORT).show();
			//初始化总数为0
			sumMuisc = 0;
			//取得音乐列表
			GetMusicCursor();
			sendBroadcastOnCommand(MusicService.COMMAND_TELL_ME_NUMBER);
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
					sumMuisc++;
				} while (cursor.moveToNext());
			}
		}
		
		//移动到下一首
		private void moveNumberToNext(){
			//判断是否到达了列表低端
			if((number +1 ) >= sumMuisc){
				number = 0;
			} else {
				++number;
			}
		}
		
		//移动到上一首
		private void moveNumberToPrevious(){
			//判断是否到达了列表顶端
			if((number ==0) ){
				number = sumMuisc-1;
			} else {
				--number;
			}
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
	    					tv_Text_Current.setText(formatTime(time));
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
	    				tv_Text_Current.setText("00:00");
	    				break;
	    			}
	    		}
	    	};
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
					seekBarHandler.removeMessages(PROGRESS_INCREASE);
					seekBarHandler.sendEmptyMessageDelayed(PROGRESS_INCREASE, 1000);
					seekBar.setMax(duration);
					seekBar.setProgress(time);
					Music m = list_Music.get(number);
					tv_Song_Name.setText(m.getTitle());
					tv_Text_Duration.setText(formatTime(duration));
					tv_Play.setImageDrawable(getResources().getDrawable(R.drawable.desktop_pausebt_b));
					break;
				case MusicService.STATUS_PAUSED:
					seekBarHandler.sendEmptyMessage(PROGRESS_PAUSE);
					tv_Play.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt_b));
					break;
				case MusicService.STATUS_COMPLETED:
					seekBarHandler.sendEmptyMessage(PROGRESS_RESET);
					sendBroadcastOnCommand(MusicService.COMMAND_NEXT);
					tv_Play.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt_b));
					break;
				case MusicService.RETURNBACK:
					number = intent.getIntExtra("number", 0);
					Music m1 = list_Music.get(number);
				    tv_Song_Name.setText(m1.getTitle());
				    tv_Singer.setText(m1.getSinger());
				    break;
				default:
					break;
				}
				updateUI(status);
			}
			//根据播放器的播放状态，更新UI
		    private void updateUI(int status){
		    	switch(status){
		    	case MusicService.STATUS_PLAYING:
		    		tv_Play.setImageDrawable(getResources().getDrawable(R.drawable.desktop_pausebt_b));
		    		break;
		    	case MusicService.STATUS_PAUSED:
		    	case MusicService.STATUS_STOPPED:
		    	case MusicService.STATUS_COMPLETED:
		    		tv_Play.setImageDrawable(getResources().getDrawable(R.drawable.desktop_playbt_b));
		    		break;
		    	default:
		    		break;
		    	}
		    }
	    }
	
}
