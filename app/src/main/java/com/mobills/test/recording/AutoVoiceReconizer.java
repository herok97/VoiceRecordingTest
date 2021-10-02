package com.mobills.test.recording;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Base64OutputStream;
import android.util.Log;
import android.util.Base64;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringEscapeUtils;

public class AutoVoiceReconizer {

	public static final int VOICE_READY = 1;
	public static final int VOICE_RECONIZING = 2;
	public static final int VOICE_RECONIZED = 3;
	public static final int VOICE_RECORDING_FINSHED = 4;
	public static final int VOICE_PLAYING = 5;
	
	RecordAudio recordTask;
	PlayAudio playTask;
	final int CUSTOM_FREQ_SOAP = 2;;

	File recordingFile;

	boolean isRecording = false;
	boolean isPlaying = false;

	int frequency = 11025;
	int outfrequency = frequency*CUSTOM_FREQ_SOAP;
	int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int bufferReadResult;
	
	private Handler handler;
	
	
	LinkedList<short[]> recData = new LinkedList<short[]>();
	
	int level; // 볼륨레벨
	private int startingIndex = -1; // 녹음 시작 인덱스
	private int endIndex = -1;
	private int cnt = 0;// 카운터
	private File path;
	private boolean voiceReconize = false;

	public AutoVoiceReconizer( Handler handler ){
		this.handler = handler;
		path = new File(
				Environment.getExternalStorageDirectory().getAbsolutePath()
						+ "/sdcard/meditest/");

		path.mkdirs();
		try {
			recordingFile = File.createTempFile("recording", ".pcm", path);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't create file on SD card", e);
		}
	}

	public void startLevelCheck(){
		voiceReconize = false;
		cnt = 0;
		startingIndex = -1;
		endIndex = -1;
		recData.clear();
		recordTask = new RecordAudio();
		recordTask.execute();
		isRecording = true;

	}

	public String getStringFile(File f) {
		InputStream inputStream = null;
		String encodedFile= "", lastVal;
		try {
			inputStream = new FileInputStream(f.getAbsolutePath());

			byte[] buffer = new byte[16000 * 45];//specify the size to allow
			int bytesRead;
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			Base64OutputStream output64 = null;
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
				output64 = new Base64OutputStream(output, Base64.DEFAULT);
			}

			while ((bytesRead = inputStream.read(buffer)) != -1) {
				output64.write(buffer, 0, bytesRead);
			}
			output64.close();
			encodedFile =  output.toString();
		}
		catch (FileNotFoundException e1 ) {
			e1.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		lastVal = encodedFile;
		return lastVal;
	}


	public static String readStream(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);
		for (String line = r.readLine(); line != null; line =r.readLine()){
			sb.append(line);
		}
		in.close();
		return sb.toString();
	}

	public String sendDataAndGetResult (String audioByte) {
		String openApiURL = "http://aiopen.etri.re.kr:8000/WiseASR/Recognition";
		String accessKey = "154f164e-34ca-4b52-88b3-8108edd22849";
		Gson gson = new Gson();

		Map<String, Object> request = new HashMap<>();
		Map<String, String> argument = new HashMap<>();


		argument.put("language_code", "korean");
		argument.put("audio", audioByte);

		request.put("access_key", accessKey);
		request.put("argument", argument);

		URL url;
		Integer responseCode;
		String responBody;
		try {
			url = new URL(openApiURL);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.write(gson.toJson(request).getBytes("UTF-8"));
			wr.flush();
			wr.close();

			String jsonStr = gson.toJson(request);
			System.out.println(jsonStr);

			responseCode = con.getResponseCode();
			if ( responseCode == 200 ) {
				InputStream is = new BufferedInputStream(con.getInputStream());
				responBody = readStream(is);
				System.out.println(StringEscapeUtils.unescapeJava(responBody));
				return responBody;
			}
			else
				return "ERROR: " + Integer.toString(responseCode);
		} catch (Throwable t) {
			return "ERROR: " + t.toString();
		}
	}

	public String stopLevelCheck(){
		short[] buffer = null;

		isRecording = false;

		try {
			DataOutputStream dos = new DataOutputStream(
					new BufferedOutputStream(new FileOutputStream(
							recordingFile)));

			Log.i("test", "startingIndex = " + startingIndex + " endIndex = " + endIndex );
			for( int i = startingIndex ; i < endIndex ; i++ ){
				buffer = recData.get( i );
				for( int j = 0 ; j < bufferReadResult ; j++ ){
					dos.writeShort( buffer[ j ] );
				}
			}
			dos.close();
//			String audioByte = getStringFile(recordingFile);

			int size = (int) recordingFile.length();
			byte[] bytes = new byte[size];
			try {
				BufferedInputStream buf = new BufferedInputStream(new FileInputStream(recordingFile));
				buf.read(bytes, 0, bytes.length);
				buf.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			byte[] encodedBytes = new byte[0];
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
				encodedBytes = Base64.encode(bytes, 0);
			}




			String encodedString = new String(encodedBytes);
			return sendDataAndGetResult(encodedString);

		} catch (FileNotFoundException e) {
			Log.e("e","fffffffffffffffffffffffffffffff");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e("e","IOExceptionddddddddddddddddddddddddddddd");
			e.printStackTrace();
		}


		Message msg = handler.obtainMessage( VOICE_PLAYING );
		handler.sendMessage( msg );

		playTask = new PlayAudio();
		playTask.execute();

		///
		return "false";
	}

	public void playVoice(){

	}

	private class PlayAudio extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;

			int bufferSize = AudioTrack.getMinBufferSize((int)(outfrequency * 1.5),
					channelConfiguration, audioEncoding);
			short[] audiodata = new short[bufferSize / 4];

			/*
			int bufferSize = AudioTrack.getMinBufferSize((int)(outfrequency),
					channelConfiguration, audioEncoding);
			short[] audiodata = new short[bufferSize / 4];
			*/

			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								recordingFile)));

				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, (int) (outfrequency * 1.5),
						channelConfiguration, audioEncoding, bufferSize,
						AudioTrack.MODE_STREAM);
				///////////////////// 약간 목소리가 변형되어 나옴.. * 1.5 를 빼면 원본 목소리가 나옴 /////
				/*
				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, (int) (outfrequency * 1.5),
						channelConfiguration, audioEncoding, bufferSize,
						AudioTrack.MODE_STREAM);
						*/

				audioTrack.play();

				while (isPlaying && dis.available() > 0) {
					int i = 0;
					while (dis.available() > 0 && i < audiodata.length) {
						audiodata[i] = dis.readShort();
						i++;
					}
					audioTrack.write(audiodata, 0, audiodata.length);
				}

				dis.close();

			} catch (Throwable t) {
				Log.e("AudioTrack", "Playback Failed");
			}

			Message msg = handler.obtainMessage( VOICE_READY );
			handler.sendMessage( msg );

			return null;
		}
	}

	private class RecordAudio extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {

			Message msg = null;
			try {

				msg = handler.obtainMessage( VOICE_RECONIZING );
				handler.sendMessage( msg );

				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								recordingFile)));
				int bufferSize = AudioRecord.getMinBufferSize(outfrequency,
						channelConfiguration, audioEncoding);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, outfrequency,
						channelConfiguration, audioEncoding, bufferSize);
				short[] buffer = null;
				audioRecord.startRecording();
				int total = 0;
				buffer = new short[bufferSize];
				while (isRecording) {
					buffer = new short[bufferSize];
					bufferReadResult = audioRecord.read(buffer, 0,
							bufferSize);
					total = 0;
					for (int i = 0; i < bufferReadResult; i++) {
						total += Math.abs(buffer[i]);
					}
					recData.add( buffer );
					level = (int) ( total / bufferReadResult );

					// level 은 볼륨..
					// level 값이 2000이 넘은 경우 목소리를 체크를 시작
					// 2000이 넘는 상태에서 cnt 를 증가시켜 10회 이상 지속되면 목소리가 나는 것으로 간주함
					// voiceReconize 가 활성화 되면 시작 포인트
					if( voiceReconize == false ){
						if( level > 1000 ){
							if( cnt == 0 )
								startingIndex = recData.size();
							cnt++;
						}

						if( cnt > 5 ){
							cnt = 0;
							voiceReconize = true;
							// level 값이 처음으로 1000 값을 넘은시점으로부터 15 만큼 이전부터 플레이 시점 설정
							// 시작하는 목소리가 끊겨 들리지 않게 하기 위하여 -15
							startingIndex -= 15;
							if( startingIndex < 0 )
								startingIndex = 0;

							msg = handler.obtainMessage( VOICE_RECONIZED );
							handler.sendMessage( msg );
						}
					}

					if( voiceReconize == true ){
						// 목소리가 끝나고 500이하로 떨어진 상태가 40이상 지속된 경우
						// 더이상 말하지 않는것으로 간주.. 레벨 체킹 끝냄
						if( level < 500 ){
							cnt++;
						}
						// 도중에 다시 소리가 커지는 경우 잠시 쉬었다가 계속 말하는 경우이므로 cnt 값은 0
						if( level > 1000 ){
							cnt = 0;
						}
						// endIndex 를 저장하고 레벨체킹을 끝냄
						if( cnt > 10 ){
							endIndex = recData.size();
							isRecording = false;
							
							msg = handler.obtainMessage( VOICE_RECORDING_FINSHED );
							handler.sendMessage( msg );
							stopLevelCheck();
							startLevelCheck();
						}
					}
				}
				audioRecord.stop();
				dos.close();
			} catch (Exception e) {
				Log.e("AudioRecord", "Recording Failed");
				Log.e("AudioRecord", e.toString() );
			}

			return null;
		}

		protected void onPostExecute(Void result) {
		}
	}

}
