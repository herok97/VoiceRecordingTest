package com.mobills.test.recording;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class VoiceRecordingTestActivity extends Activity {
	private Button btnStart;
	private Button btnStop;
	private AutoVoiceReconizer autoVoiceRecorder;
	
	private TextView statusTextView;
	private TextView resultTextView;
	private String result;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        autoVoiceRecorder = new AutoVoiceReconizer( handler );
        statusTextView = (TextView)findViewById( R.id.text_view_status );
		resultTextView = (TextView)findViewById( R.id.tv_result );
        btnStart = (Button)findViewById( R.id.btn_start );
        btnStop = (Button)findViewById( R.id.btn_stop );

        statusTextView.setText("준비..");

		Thread threadRecog = new Thread(new Runnable() {
			public void run() {
				autoVoiceRecorder.startLevelCheck();
			}
		});
		threadRecog.start();
//		resultTextView.setText(result);
//		autoVoiceRecorder.startLevelCheck();

//        btnStart.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				autoVoiceRecorder.startLevelCheck();
//			}
//		});

//        btnStop.setOnClickListener( new OnClickListener(){
//        	@Override
//			public void onClick(View v) {
//
//			}
//        });

//		Thread threadRecog = new Thread(new Runnable() {
//			public void run() {
//				autoVoiceRecorder.startLevelCheck();
//				result = autoVoiceRecorder.stopLevelCheck();
//				Log.e("ee", result);
//				autoVoiceRecorder.startLevelCheck();
//			}
//		});
//		threadRecog.start();
//		resultTextView.setText(result);
    }


    
    Handler handler = new Handler(){
    	public void handleMessage(Message msg) {
			switch( msg.what ){
			case AutoVoiceReconizer.VOICE_READY:
				statusTextView.setText("준비...");
				break;
			case AutoVoiceReconizer.VOICE_RECONIZING:
//				statusTextView.setTextColor( Color.YELLOW );
				statusTextView.setText("목소리 인식중...");
				break;
			case AutoVoiceReconizer.VOICE_RECONIZED :
//				statusTextView.setTextColor( Color.GREEN );
				statusTextView.setText("목소리 감지... 녹음중...");
				break;
			case AutoVoiceReconizer.VOICE_RECORDING_FINSHED:
//				statusTextView.setTextColor( Color.YELLOW );
				statusTextView.setText("목소리 녹음 완료 재생 버튼을 누르세요...");
				break;
			case AutoVoiceReconizer.VOICE_PLAYING:
//				statusTextView.setTextColor( Color.WHITE );
				statusTextView.setText("플레이중...");
				break;
			}
    	}
    };
}